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

import static org.assertj.core.api.Assertions.assertThat;

import org.hyperledger.besu.datatypes.Address;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

class P256TaiVrfServiceTest {

  private static final Address TEST_VALIDATOR =
      Address.fromHexString("0x0000000000000000000000000000000000000001");

  @Test
  void shouldMatchRfc9381Example10() {
    final BigInteger privateScalar =
        new BigInteger("c9afa9d845ba75166b5c215767b1d693" + "4e50c3db36e89b127b8a622b120f6721", 16);

    final Bytes input = Bytes.wrap("sample".getBytes(StandardCharsets.US_ASCII));

    final Bytes expectedPublicKey =
        Bytes.fromHexString(
            "0x0360fed4ba255a9d31c961eb74c6356d68" + "c049b8923b61fa6ce669622e60f29fb6");

    final Bytes expectedProof =
        Bytes.fromHexString(
            "0x035b5c726e8c0e2c488a107c600578ee75"
                + "cb702343c153cb1eb8dec77f4b5071b4a"
                + "53f0a46f018bc2c56e58d383f2305e097"
                + "5972c26feea0eb122fe7893c15af376b33"
                + "edf7de17c6ea056d4d82de6bc02f");

    final Bytes expectedOutput =
        Bytes.fromHexString(
            "0xa3ad7b0ef73d8fc6655053ea22f9bede" + "8c743f08bbed3d38821f0e16474b505e");

    final P256TaiVrfService service = new P256TaiVrfService(TEST_VALIDATOR, privateScalar);

    final VrfProof generated = service.generate(input);

    assertThat(service.getCompressedPublicKey()).isEqualTo(expectedPublicKey);

    assertThat(generated.proof()).isEqualTo(expectedProof);

    assertThat(generated.output()).isEqualTo(expectedOutput);

    assertThat(service.verify(input, generated, expectedPublicKey)).isTrue();
  }

  @Test
  void shouldRejectModifiedProof() {
    final BigInteger privateScalar =
        new BigInteger("c9afa9d845ba75166b5c215767b1d693" + "4e50c3db36e89b127b8a622b120f6721", 16);

    final Bytes input = Bytes.wrap("sample".getBytes(StandardCharsets.US_ASCII));

    final P256TaiVrfService service = new P256TaiVrfService(TEST_VALIDATOR, privateScalar);

    final VrfProof generated = service.generate(input);
    final byte[] modifiedProofBytes = generated.proof().toArrayUnsafe().clone();

    modifiedProofBytes[modifiedProofBytes.length - 1] ^= 0x01;

    final VrfProof modified =
        new VrfProof(generated.validator(), generated.output(), Bytes.wrap(modifiedProofBytes));

    assertThat(service.verify(input, modified, service.getCompressedPublicKey())).isFalse();
  }
}
