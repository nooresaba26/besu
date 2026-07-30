/*
 * Copyright contributors to Besu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.consensus.qbft.core.payload;

import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.qbft.core.messagedata.QbftV1;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.util.Objects;
import java.util.StringJoiner;

import org.apache.tuweni.bytes.Bytes;

/** Signed VRF announcement for a validator-selection block height. */
public class VrfAnnouncementPayload extends QbftPayload {

  private static final int TYPE = QbftV1.VRF_ANNOUNCEMENT;

  private final ConsensusRoundIdentifier roundIdentifier;
  private final Bytes publicKey;
  private final Bytes output;
  private final Bytes proof;

  /**
   * Creates a VRF announcement payload.
   *
   * @param roundIdentifier target block height and selection round
   * @param publicKey compressed P-256 VRF public key
   * @param output RFC 9381 VRF output
   * @param proof RFC 9381 VRF proof
   */
  public VrfAnnouncementPayload(
      final ConsensusRoundIdentifier roundIdentifier,
      final Bytes publicKey,
      final Bytes output,
      final Bytes proof) {

    if (roundIdentifier == null) {
      throw new IllegalArgumentException("Round identifier cannot be null");
    }

    if (publicKey == null || output == null || proof == null) {
      throw new IllegalArgumentException("VRF payload values cannot be null");
    }

    this.roundIdentifier = roundIdentifier;
    this.publicKey = publicKey;
    this.output = output;
    this.proof = proof;
  }

  /**
   * Decodes a VRF announcement payload.
   *
   * @param rlpInput encoded payload
   * @return decoded payload
   */
  public static VrfAnnouncementPayload readFrom(final RLPInput rlpInput) {
    rlpInput.enterList();

    final ConsensusRoundIdentifier roundIdentifier = readConsensusRound(rlpInput);
    final Bytes publicKey = rlpInput.readBytes();
    final Bytes output = rlpInput.readBytes();
    final Bytes proof = rlpInput.readBytes();

    rlpInput.leaveList();

    return new VrfAnnouncementPayload(roundIdentifier, publicKey, output, proof);
  }

  @Override
  public void writeTo(final RLPOutput rlpOutput) {
    rlpOutput.startList();

    writeConsensusRound(rlpOutput);
    rlpOutput.writeBytes(publicKey);
    rlpOutput.writeBytes(output);
    rlpOutput.writeBytes(proof);

    rlpOutput.endList();
  }

  @Override
  public int getMessageType() {
    return TYPE;
  }

  @Override
  public ConsensusRoundIdentifier getRoundIdentifier() {
    return roundIdentifier;
  }

  public Bytes getPublicKey() {
    return publicKey;
  }

  public Bytes getOutput() {
    return output;
  }

  public Bytes getProof() {
    return proof;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }

    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    final VrfAnnouncementPayload that = (VrfAnnouncementPayload) other;

    return Objects.equals(roundIdentifier, that.roundIdentifier)
        && Objects.equals(publicKey, that.publicKey)
        && Objects.equals(output, that.output)
        && Objects.equals(proof, that.proof);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roundIdentifier, publicKey, output, proof);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", VrfAnnouncementPayload.class.getSimpleName() + "[", "]")
        .add("roundIdentifier=" + roundIdentifier)
        .add("publicKey=" + publicKey)
        .add("output=" + output)
        .add("proofSize=" + proof.size())
        .toString();
  }
}
