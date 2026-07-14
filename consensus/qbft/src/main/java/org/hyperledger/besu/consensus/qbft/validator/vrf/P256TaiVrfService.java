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
package org.hyperledger.besu.consensus.qbft.validator.vrf;

import org.hyperledger.besu.datatypes.Address;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import org.apache.tuweni.bytes.Bytes;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.math.ec.ECPoint;

// import org.hyperledger.besu.datatypes.Hash;

/**
 * RFC 9381 ECVRF-P256-SHA256-TAI service.
 *
 * <p>This initial version performs curve and key conversion only. Proof generation and proof
 * verification will be added in the next stages.
 */
public final class P256TaiVrfService implements VrfService {

  private static final String CURVE_NAME = "secp256r1";
  private static final int SUITE_IDENTIFIER = 0x01;
  private static final int ENCODE_TO_CURVE_FRONT_SEPARATOR = 0x01;
  private static final int ENCODE_TO_CURVE_BACK_SEPARATOR = 0x00;
  private static final int COMPRESSED_EVEN_Y_PREFIX = 0x02;
  private static final int MAX_COUNTER_VALUE = 255;
  private static final int SHA256_OUTPUT_SIZE = 32;

  private final Address localValidator;
  private final X9ECParameters curveParameters;
  private final BigInteger privateScalar;
  private final ECPoint publicPoint;

  private static final int CHALLENGE_LENGTH = 16;
  private static final int SCALAR_LENGTH = 32;
  private static final int COMPRESSED_POINT_LENGTH = 33;
  private static final int PROOF_LENGTH =
      COMPRESSED_POINT_LENGTH + CHALLENGE_LENGTH + SCALAR_LENGTH;

  private static final int CHALLENGE_FRONT_SEPARATOR = 0x02;
  private static final int CHALLENGE_BACK_SEPARATOR = 0x00;

  private static final int PROOF_TO_HASH_FRONT_SEPARATOR = 0x03;
  private static final int PROOF_TO_HASH_BACK_SEPARATOR = 0x00;

  public P256TaiVrfService(final Address localValidator, final KeyPair keyPair) {
    if (localValidator == null) {
      throw new IllegalArgumentException("Local validator cannot be null");
    }

    if (keyPair == null) {
      throw new IllegalArgumentException("VRF key pair cannot be null");
    }

    if (!(keyPair.getPrivate() instanceof ECPrivateKey ecPrivateKey)) {
      throw new IllegalArgumentException("VRF private key must be an EC private key");
    }

    if (!(keyPair.getPublic() instanceof ECPublicKey ecPublicKey)) {
      throw new IllegalArgumentException("VRF public key must be an EC public key");
    }

    final X9ECParameters parameters = ECNamedCurveTable.getByName(CURVE_NAME);

    if (parameters == null) {
      throw new IllegalStateException("Unable to load P-256 curve parameters");
    }

    this.localValidator = localValidator;
    this.curveParameters = parameters;
    this.privateScalar = ecPrivateKey.getS();

    this.publicPoint =
        parameters
            .getCurve()
            .createPoint(ecPublicKey.getW().getAffineX(), ecPublicKey.getW().getAffineY())
            .normalize();

    validateKeyPair();
  }

  P256TaiVrfService(final Address localValidator, final BigInteger privateScalar) {
    if (localValidator == null) {
      throw new IllegalArgumentException("Local validator cannot be null");
    }

    if (privateScalar == null) {
      throw new IllegalArgumentException("VRF private scalar cannot be null");
    }

    final X9ECParameters parameters = ECNamedCurveTable.getByName(CURVE_NAME);

    if (parameters == null) {
      throw new IllegalStateException("Unable to load P-256 curve parameters");
    }

    this.localValidator = localValidator;
    this.curveParameters = parameters;
    this.privateScalar = privateScalar;
    this.publicPoint = parameters.getG().multiply(privateScalar).normalize();

    validateKeyPair();
  }

  /**
   * Returns the local validator's compressed P-256 public key.
   *
   * <p>The result is 33 bytes: one compression prefix byte followed by the 32-byte x-coordinate.
   *
   * @return compressed public key
   */
  public Bytes getCompressedPublicKey() {
    return Bytes.wrap(publicPoint.getEncoded(true));
  }

  @Override
  public VrfProof generate(final Bytes input) {
    if (input == null) {
      throw new IllegalArgumentException("VRF input cannot be null");
    }

    // RFC 9381, ECVRF proving:
    // H = encode_to_curve(PK, alpha)
    final ECPoint hPoint = encodeToCurve(input, getCompressedPublicKey());

    // Gamma = x * H
    final ECPoint gamma = hPoint.multiply(privateScalar).normalize();

    // k = deterministic RFC 6979 nonce
    final BigInteger nonce = generateNonce(hPoint);

    // k * B and k * H
    final ECPoint nonceBasePoint = curveParameters.getG().multiply(nonce).normalize();

    final ECPoint nonceHashPoint = hPoint.multiply(nonce).normalize();

    // c = challenge(Y, H, Gamma, kB, kH)
    final BigInteger challenge =
        generateChallenge(publicPoint, hPoint, gamma, nonceBasePoint, nonceHashPoint);

    // s = (k + c*x) mod q
    final BigInteger response =
        nonce.add(challenge.multiply(privateScalar)).mod(curveParameters.getN());

    final Bytes proof = encodeProof(gamma, challenge, response);

    final Bytes output = proofToHash(gamma);

    return new VrfProof(localValidator, output, proof);
  }

  @Override
  public boolean verify(final Bytes input, final VrfProof proof, final Bytes publicKey) {
    if (input == null || proof == null || publicKey == null) {
      return false;
    }

    try {
      final ECPoint verifierPublicPoint = decodePublicKey(publicKey);
      final DecodedProof decodedProof = decodeProof(proof.proof());

      final ECPoint gamma = decodedProof.gamma();
      final BigInteger challenge = decodedProof.challenge();
      final BigInteger response = decodedProof.response();

      /*
       * RFC 9381 verification:
       *
       * U = s*B - c*Y
       * V = s*H - c*Gamma
       */
      final ECPoint hPoint = encodeToCurve(input, publicKey);

      final ECPoint uPoint =
          curveParameters
              .getG()
              .multiply(response)
              .subtract(verifierPublicPoint.multiply(challenge))
              .normalize();

      final ECPoint vPoint =
          hPoint.multiply(response).subtract(gamma.multiply(challenge)).normalize();

      if (uPoint.isInfinity() || vPoint.isInfinity() || !uPoint.isValid() || !vPoint.isValid()) {
        return false;
      }

      final BigInteger expectedChallenge =
          generateChallenge(verifierPublicPoint, hPoint, gamma, uPoint, vPoint);

      if (!expectedChallenge.equals(challenge)) {
        return false;
      }

      final Bytes expectedOutput = proofToHash(gamma);

      return expectedOutput.equals(proof.output());
    } catch (final IllegalArgumentException | IllegalStateException e) {
      return false;
    }
  }

  private ECPoint decodePublicKey(final Bytes publicKey) {
    if (publicKey.size() != COMPRESSED_POINT_LENGTH) {
      throw new IllegalArgumentException("Compressed P-256 public key must be 33 bytes");
    }

    final ECPoint point;

    try {
      point = curveParameters.getCurve().decodePoint(publicKey.toArrayUnsafe()).normalize();
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid compressed P-256 public key", e);
    }

    if (point.isInfinity() || !point.isValid()) {
      throw new IllegalArgumentException("Invalid P-256 public-key point");
    }

    /*
     * Validate subgroup membership.
     * For a valid point Y, q*Y must be the point at infinity.
     */
    if (!point.multiply(curveParameters.getN()).isInfinity()) {
      throw new IllegalArgumentException("P-256 public key is not in the required subgroup");
    }

    return point;
  }

  /**
   * Implements RFC 9381 ECVRF_encode_to_curve_try_and_increment for ECVRF-P256-SHA256-TAI.
   *
   * @param input public VRF input for the validator-selection round
   * @return a valid non-identity P-256 curve point
   */
  private ECPoint encodeToCurve(final Bytes input, final Bytes publicKeySalt) {
    if (input == null) {
      throw new IllegalArgumentException("VRF input cannot be null");
    }

    if (publicKeySalt == null || publicKeySalt.size() != COMPRESSED_POINT_LENGTH) {
      throw new IllegalArgumentException(
          "VRF public-key salt must be a compressed 33-byte P-256 point");
    }

    for (int counter = 0; counter <= MAX_COUNTER_VALUE; counter++) {
      final Bytes hashInput =
          Bytes.concatenate(
              Bytes.of(SUITE_IDENTIFIER),
              Bytes.of(ENCODE_TO_CURVE_FRONT_SEPARATOR),
              publicKeySalt,
              input,
              Bytes.of(counter),
              Bytes.of(ENCODE_TO_CURVE_BACK_SEPARATOR));

      final Bytes hashValue = sha256(hashInput);

      final Bytes compressedPointCandidate =
          Bytes.concatenate(Bytes.of(COMPRESSED_EVEN_Y_PREFIX), hashValue);

      try {
        final ECPoint point =
            curveParameters
                .getCurve()
                .decodePoint(compressedPointCandidate.toArrayUnsafe())
                .normalize();

        if (!point.isInfinity() && point.isValid()) {
          return point;
        }
      } catch (final IllegalArgumentException ignored) {
        // Try the next counter value.
      }
    }

    throw new IllegalStateException(
        "Unable to encode VRF seed to a P-256 point after 256 attempts");
  }

  private Bytes sha256(final Bytes input) {
    final SHA256Digest digest = new SHA256Digest();
    final byte[] inputBytes = input.toArrayUnsafe();
    final byte[] output = new byte[SHA256_OUTPUT_SIZE];

    digest.update(inputBytes, 0, inputBytes.length);
    digest.doFinal(output, 0);

    return Bytes.wrap(output);
  }

  private BigInteger generateNonce(final ECPoint hPoint) {

    final HMacDSAKCalculator calculator = new HMacDSAKCalculator(new SHA256Digest());

    calculator.init(
        curveParameters.getN(),
        privateScalar,
        sha256(Bytes.wrap(hPoint.getEncoded(true))).toArrayUnsafe());

    return calculator.nextK();
  }

  private BigInteger generateChallenge(
      final ECPoint publicKeyPoint,
      final ECPoint hPoint,
      final ECPoint gamma,
      final ECPoint nonceBasePoint,
      final ECPoint nonceHashPoint) {

    final Bytes challengeInput =
        Bytes.concatenate(
            Bytes.of(SUITE_IDENTIFIER),
            Bytes.of(CHALLENGE_FRONT_SEPARATOR),
            encodePoint(publicKeyPoint),
            encodePoint(hPoint),
            encodePoint(gamma),
            encodePoint(nonceBasePoint),
            encodePoint(nonceHashPoint),
            Bytes.of(CHALLENGE_BACK_SEPARATOR));

    final Bytes challengeHash = sha256(challengeInput);

    /*
     * RFC 9381 uses the first cLen bytes of the hash.
     * For P-256 TAI, cLen = 16 bytes.
     */
    final Bytes truncatedChallenge = challengeHash.slice(0, CHALLENGE_LENGTH);

    return new BigInteger(1, truncatedChallenge.toArrayUnsafe());
  }

  private void validateKeyPair() {
    final BigInteger groupOrder = curveParameters.getN();

    if (privateScalar.signum() <= 0 || privateScalar.compareTo(groupOrder) >= 0) {
      throw new IllegalArgumentException("VRF private scalar is outside the P-256 group order");
    }

    if (publicPoint.isInfinity() || !publicPoint.isValid()) {
      throw new IllegalArgumentException("VRF public key is not a valid P-256 point");
    }

    final ECPoint expectedPublicPoint = curveParameters.getG().multiply(privateScalar).normalize();

    if (!expectedPublicPoint.equals(publicPoint)) {
      throw new IllegalArgumentException(
          "VRF private key and public key do not form a valid key pair");
    }
  }

  public Address getLocalValidator() {
    return localValidator;
  }

  private Bytes encodeProof(
      final ECPoint gamma, final BigInteger challenge, final BigInteger response) {

    final Bytes gammaBytes = encodePoint(gamma);
    final Bytes challengeBytes = unsignedFixedLength(challenge, CHALLENGE_LENGTH);
    final Bytes responseBytes = unsignedFixedLength(response, SCALAR_LENGTH);

    final Bytes proof = Bytes.concatenate(gammaBytes, challengeBytes, responseBytes);

    if (proof.size() != PROOF_LENGTH) {
      throw new IllegalStateException("Unexpected VRF proof length: " + proof.size());
    }

    return proof;
  }

  private Bytes proofToHash(final ECPoint gamma) {
    /*
     * P-256 has cofactor 1, so:
     *
     *     cofactor * Gamma = Gamma
     */
    return sha256(
        Bytes.concatenate(
            Bytes.of(SUITE_IDENTIFIER),
            Bytes.of(PROOF_TO_HASH_FRONT_SEPARATOR),
            encodePoint(gamma),
            Bytes.of(PROOF_TO_HASH_BACK_SEPARATOR)));
  }

  private Bytes encodePoint(final ECPoint point) {
    if (point == null || point.isInfinity() || !point.isValid()) {
      throw new IllegalArgumentException("Cannot encode an invalid P-256 point");
    }

    final Bytes encoded = Bytes.wrap(point.normalize().getEncoded(true));

    if (encoded.size() != COMPRESSED_POINT_LENGTH) {
      throw new IllegalStateException(
          "Unexpected compressed P-256 point length: " + encoded.size());
    }

    return encoded;
  }

  private Bytes unsignedFixedLength(final BigInteger value, final int length) {

    if (value == null) {
      throw new IllegalArgumentException("Integer value cannot be null");
    }

    if (value.signum() < 0) {
      throw new IllegalArgumentException("Integer value cannot be negative");
    }

    final byte[] raw = value.toByteArray();
    final byte[] result = new byte[length];

    /*
     * BigInteger.toByteArray() may contain a leading zero sign byte.
     */
    final int sourceOffset = raw.length > 1 && raw[0] == 0 ? 1 : 0;

    final int sourceLength = raw.length - sourceOffset;

    if (sourceLength > length) {
      throw new IllegalArgumentException("Integer does not fit in " + length + " bytes");
    }

    System.arraycopy(raw, sourceOffset, result, length - sourceLength, sourceLength);

    return Bytes.wrap(result);
  }

  private record DecodedProof(ECPoint gamma, BigInteger challenge, BigInteger response) {}

  private DecodedProof decodeProof(final Bytes proofBytes) {
    if (proofBytes == null || proofBytes.size() != PROOF_LENGTH) {
      throw new IllegalArgumentException("VRF proof must be exactly " + PROOF_LENGTH + " bytes");
    }

    final Bytes gammaBytes = proofBytes.slice(0, COMPRESSED_POINT_LENGTH);

    final Bytes challengeBytes = proofBytes.slice(COMPRESSED_POINT_LENGTH, CHALLENGE_LENGTH);

    final Bytes responseBytes =
        proofBytes.slice(COMPRESSED_POINT_LENGTH + CHALLENGE_LENGTH, SCALAR_LENGTH);

    final ECPoint gamma;

    try {
      gamma = curveParameters.getCurve().decodePoint(gammaBytes.toArrayUnsafe()).normalize();
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException("VRF proof contains an invalid Gamma point", e);
    }

    if (gamma.isInfinity() || !gamma.isValid()) {
      throw new IllegalArgumentException("VRF proof Gamma is not a valid P-256 point");
    }

    if (!gamma.multiply(curveParameters.getN()).isInfinity()) {
      throw new IllegalArgumentException("VRF proof Gamma is not in the required subgroup");
    }

    final BigInteger challenge = new BigInteger(1, challengeBytes.toArrayUnsafe());

    final BigInteger response = new BigInteger(1, responseBytes.toArrayUnsafe());

    if (response.signum() < 0 || response.compareTo(curveParameters.getN()) >= 0) {
      throw new IllegalArgumentException("VRF proof response is outside the P-256 scalar range");
    }

    return new DecodedProof(gamma, challenge, response);
  }

  public void runSelfTest(final Bytes input) {
    final VrfProof generatedProof = generate(input);

    final boolean valid = verify(input, generatedProof, getCompressedPublicKey());

    if (!valid) {
      throw new IllegalStateException("Generated VRF proof failed local verification");
    }

    final byte[] corruptedBytes = generatedProof.proof().toArrayUnsafe().clone();

    corruptedBytes[corruptedBytes.length - 1] ^= 0x01;

    final VrfProof corruptedProof =
        new VrfProof(
            generatedProof.validator(), generatedProof.output(), Bytes.wrap(corruptedBytes));

    if (verify(input, corruptedProof, getCompressedPublicKey())) {
      throw new IllegalStateException("Corrupted VRF proof was incorrectly accepted");
    }
  }
}
