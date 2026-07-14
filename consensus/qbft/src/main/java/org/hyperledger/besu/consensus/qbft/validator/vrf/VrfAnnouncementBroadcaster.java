/*
 * Copyright contributors to Besu.
 *
 * Licensed under the Apache License, Version 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.consensus.qbft.validator.vrf;

import org.apache.tuweni.bytes.Bytes;

/** Broadcasts a validator's VRF announcement to the QBFT network. */
@FunctionalInterface
public interface VrfAnnouncementBroadcaster {

  VrfAnnouncementBroadcaster NO_OP =
      (blockHeight, publicKey, output, proof) -> {};

  void broadcast(
      long blockHeight,
      Bytes publicKey,
      Bytes output,
      Bytes proof);
}