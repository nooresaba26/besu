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
package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.crypto.KeyPair;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.api.query.BlockchainQueries;
import org.hyperledger.besu.ethereum.core.Transaction;
import org.hyperledger.besu.ethereum.eth.transactions.TransactionPool;
import org.hyperledger.besu.ethereum.mainnet.ValidationResult;
import org.hyperledger.besu.ethereum.transaction.TransactionInvalidReason;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalLong;

import org.apache.tuweni.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;

public class ReputationContractTransactionSender {

  private static final Logger LOG =
      LoggerFactory.getLogger(ReputationContractTransactionSender.class);

  private static final String RECORD_FINALIZED_BLOCK = "recordFinalizedBlock";

  private static final long GAS_LIMIT = 3_000_000L;
  private static final Wei GAS_PRICE = Wei.of(1_000_000_000L);

  private final TransactionPool transactionPool;
  private final BlockchainQueries blockchainQueries;
  private final KeyPair localKeyPair;
  private final Address localAddress;

  public ReputationContractTransactionSender(
      final TransactionPool transactionPool,
      final BlockchainQueries blockchainQueries,
      final KeyPair localKeyPair,
      final Address localAddress) {
    this.transactionPool = transactionPool;
    this.blockchainQueries = blockchainQueries;
    this.localKeyPair = localKeyPair;
    this.localAddress = localAddress;
  }

  public Bytes buildRecordFinalizedBlockPayload(
      final long blockNumber,
      final Collection<Address> onlineValidators,
      final Collection<Address> selectedValidators,
      final Collection<Address> successfulVoters,
      final Collection<Address> unsuccessfulVoters) {

    final Function function =
        new Function(
            RECORD_FINALIZED_BLOCK,
            List.of(
                new Uint256(BigInteger.valueOf(blockNumber)),
                toAddressArray(onlineValidators),
                toAddressArray(selectedValidators),
                toAddressArray(successfulVoters),
                toAddressArray(unsuccessfulVoters)),
            List.of());

    return Bytes.fromHexString(FunctionEncoder.encode(function));
  }

  public void prepareRecordFinalizedBlockTransaction(
      final Address contractAddress,
      final long blockNumber,
      final Collection<Address> onlineValidators,
      final Collection<Address> selectedValidators,
      final Collection<Address> successfulVoters,
      final Collection<Address> unsuccessfulVoters) {

    final Bytes payload =
        buildRecordFinalizedBlockPayload(
            blockNumber,
            onlineValidators,
            selectedValidators,
            successfulVoters,
            unsuccessfulVoters);

    final long nonce = getNextNonce();

    final Transaction transaction =
        Transaction.builder()
            .nonce(nonce)
            .gasPrice(GAS_PRICE)
            .gasLimit(GAS_LIMIT)
            .to(contractAddress)
            .value(Wei.ZERO)
            .payload(payload)
            .guessType()
            .signAndBuild(localKeyPair);

    final ValidationResult<TransactionInvalidReason> result =
        transactionPool.addTransactionViaApi(transaction);

    if (result.isValid()) {
      LOG.info(
          "Submitted recordFinalizedBlock transaction {} for block {} from {} to contract {}",
          transaction.getHash(),
          blockNumber,
          localAddress,
          contractAddress);
    } else {
      LOG.warn(
          "Failed to submit recordFinalizedBlock transaction for block {} from {}: {}",
          blockNumber,
          localAddress,
          result.getInvalidReason());
    }
  }

  private long getNextNonce() {
    final OptionalLong pendingNonce = transactionPool.getNextNonceForSender(localAddress);

    if (pendingNonce.isPresent()) {
      return pendingNonce.getAsLong();
    }

    return blockchainQueries.getTransactionCount(localAddress);
  }

  private DynamicArray<org.web3j.abi.datatypes.Address> toAddressArray(
      final Collection<Address> addresses) {
    final List<org.web3j.abi.datatypes.Address> web3jAddresses = new ArrayList<>();

    for (final Address address : addresses) {
      web3jAddresses.add(new org.web3j.abi.datatypes.Address(address.toHexString()));
    }

    return new DynamicArray<>(org.web3j.abi.datatypes.Address.class, web3jAddresses);
  }
}
