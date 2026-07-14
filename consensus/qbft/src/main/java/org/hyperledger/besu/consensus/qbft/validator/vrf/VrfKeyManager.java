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

import org.hyperledger.besu.crypto.SecureRandomProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/** Loads or creates the persistent P-256 key pair used by the validator-selection VRF. */
public final class VrfKeyManager {

  private static final String CURVE_NAME = "secp256r1";
  private static final String KEY_ALGORITHM = "EC";

  private static final String PRIVATE_KEY_FILENAME = "vrf-private-key.pk8";
  private static final String PUBLIC_KEY_FILENAME = "vrf-public-key.x509";

  private VrfKeyManager() {}

  public static KeyPair loadOrCreate(final Path dataDirectory) {
    if (dataDirectory == null) {
      throw new IllegalArgumentException("Data directory cannot be null");
    }

    final Path privateKeyPath = dataDirectory.resolve(PRIVATE_KEY_FILENAME);
    final Path publicKeyPath = dataDirectory.resolve(PUBLIC_KEY_FILENAME);

    final boolean privateKeyExists = Files.exists(privateKeyPath);
    final boolean publicKeyExists = Files.exists(publicKeyPath);

    if (privateKeyExists != publicKeyExists) {
      throw new IllegalStateException(
          "VRF key pair is incomplete. Both VRF key files must exist or neither must exist.");
    }

    try {
      if (privateKeyExists) {
        return load(privateKeyPath, publicKeyPath);
      }

      return generateAndStore(privateKeyPath, publicKeyPath);
    } catch (final IOException | GeneralSecurityException e) {
      throw new IllegalStateException("Unable to load or create VRF key pair", e);
    }
  }

  private static KeyPair load(final Path privateKeyPath, final Path publicKeyPath)
      throws IOException, GeneralSecurityException {

    final byte[] privateKeyBytes = Files.readAllBytes(privateKeyPath);
    final byte[] publicKeyBytes = Files.readAllBytes(publicKeyPath);

    final KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);

    return new KeyPair(
        keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes)),
        keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes)));
  }

  private static KeyPair generateAndStore(final Path privateKeyPath, final Path publicKeyPath)
      throws IOException, GeneralSecurityException {

    final Path dataDirectory = privateKeyPath.getParent();

    if (dataDirectory != null) {
      Files.createDirectories(dataDirectory);
    }

    final KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
    generator.initialize(
        new ECGenParameterSpec(CURVE_NAME), SecureRandomProvider.createSecureRandom());

    final KeyPair keyPair = generator.generateKeyPair();

    Files.write(privateKeyPath, keyPair.getPrivate().getEncoded());
    Files.write(publicKeyPath, keyPair.getPublic().getEncoded());

    return keyPair;
  }
}
