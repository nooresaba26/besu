/*
 * Copyright contributors to Besu.
 *
 * Licensed under the Apache License, Version 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.consensus.qbft.validator.vrf;

import org.hyperledger.besu.consensus.qbft.core.messagewrappers.VrfAnnouncementMessage;
import org.hyperledger.besu.consensus.qbft.core.types.VrfAnnouncementHandler;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.tuweni.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Verifies received VRF announcements before storing them. */
public class VerifiedVrfAnnouncementHandler implements VrfAnnouncementHandler {

  private static final Logger LOG =
      LoggerFactory.getLogger(VerifiedVrfAnnouncementHandler.class);

  private final Blockchain blockchain;
  private final P256TaiVrfService vrfVerifier;
  private final VrfAnnouncementStore announcementStore;

  private final ConcurrentMap<Address, Bytes> registeredPublicKeys =
      new ConcurrentHashMap<>();

  public VerifiedVrfAnnouncementHandler(
      final Blockchain blockchain,
      final P256TaiVrfService vrfVerifier,
      final VrfAnnouncementStore announcementStore) {

    if (blockchain == null) {
      throw new IllegalArgumentException("Blockchain cannot be null");
    }

    if (vrfVerifier == null) {
      throw new IllegalArgumentException("VRF verifier cannot be null");
    }

    if (announcementStore == null) {
      throw new IllegalArgumentException("VRF announcement store cannot be null");
    }

    this.blockchain = blockchain;
    this.vrfVerifier = vrfVerifier;
    this.announcementStore = announcementStore;
  }

  @Override
  public void handle(final VrfAnnouncementMessage message) {
    if (message == null) {
      return;
    }

    final Address validator = message.getAuthor();
    final long blockHeight =
        message.getRoundIdentifier().getSequenceNumber();

    if (blockHeight <= 0) {
      LOG.warn(
          "Rejected VRF announcement with invalid block height: validator={} block={}",
          validator,
          blockHeight);
      return;
    }

    final long parentBlockNumber = blockHeight - 1;

    final BlockHeader parentHeader =
        blockchain
            .getBlockHeader(parentBlockNumber)
            .orElse(null);

    if (parentHeader == null) {
      LOG.warn(
          "Rejected VRF announcement because parent block {} was unavailable: validator={}",
          parentBlockNumber,
          validator);
      return;
    }

    final Bytes publicKey = message.getPublicKey();
    final Bytes output = message.getOutput();
    final Bytes proofBytes = message.getProof();

    if (!bindPublicKey(validator, publicKey)) {
      LOG.warn(
          "Rejected VRF announcement because validator {} attempted to change its VRF public key",
          validator);
      return;
    }

    final Bytes vrfInput =
        VrfSeedGenerator.createSeed(parentHeader).getBytes();

    final VrfProof proof =
        new VrfProof(
            validator,
            output,
            proofBytes);

    if (!vrfVerifier.verify(vrfInput, proof, publicKey)) {
      LOG.warn(
          "Rejected invalid VRF proof: block={} validator={}",
          blockHeight,
          validator);
      return;
    }

    final VrfAnnouncement existing =
        announcementStore.getAnnouncement(
            blockHeight,
            validator);

    if (existing != null) {
      if (!existing.output().equals(output)
          || !existing.proof().equals(proofBytes)
          || !existing.publicKey().equals(publicKey)) {

        LOG.warn(
            "Rejected conflicting VRF announcement: block={} validator={}",
            blockHeight,
            validator);
      }

      return;
    }

    final VrfAnnouncement announcement =
        new VrfAnnouncement(
            blockHeight,
            validator,
            publicKey,
            output,
            proofBytes);

    announcementStore.put(announcement);

    LOG.info(
        "Stored verified remote VRF announcement: block={} validator={} output={}",
        blockHeight,
        validator,
        output);
  }

  private boolean bindPublicKey(
      final Address validator,
      final Bytes publicKey) {

    if (publicKey == null) {
      return false;
    }

    final Bytes existing =
        registeredPublicKeys.putIfAbsent(
            validator,
            publicKey);

    return existing == null || existing.equals(publicKey);
  }
}