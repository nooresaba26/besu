/*
 * Copyright ConsenSys AG.
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

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.mainnet.TransactionValidationParams;
import org.hyperledger.besu.ethereum.transaction.CallParameter;
import org.hyperledger.besu.ethereum.transaction.ImmutableCallParameter;
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator;
import org.hyperledger.besu.ethereum.transaction.TransactionSimulatorResult;
import org.hyperledger.besu.evm.tracing.OperationTracer;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.tuweni.bytes.Bytes;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

/** The Validator contract controller. */
public class ValidatorContractController {
  public static final String GET_VALIDATORS = "getActiveValidators";
  public static final String GET_VALIDATOR_STATS = "getValidatorStats";
  public static final String CONTRACT_ERROR_MSG = "Failed validator smart contract call";

  private final TransactionSimulator transactionSimulator;
  private final Function getValidatorsFunction;

  public ValidatorContractController(final TransactionSimulator transactionSimulator) {
    this.transactionSimulator = transactionSimulator;

    try {
      this.getValidatorsFunction =
          new Function(
              GET_VALIDATORS,
              List.of(),
              List.of(new TypeReference<DynamicArray<org.web3j.abi.datatypes.Address>>() {}));
    } catch (final Exception e) {
      throw new RuntimeException("Error creating smart contract function", e);
    }
  }

  public Collection<Address> getValidators(final long blockNumber, final Address contractAddress) {
    return callFunction(blockNumber, getValidatorsFunction, contractAddress)
        .map(this::parseGetValidatorsResult)
        .orElseThrow(() -> new IllegalStateException(CONTRACT_ERROR_MSG));
  }

  public ValidatorStats getValidatorStats(
      final long blockNumber, final Address contractAddress, final Address validatorAddress) {
    final Function function =
        new Function(
            GET_VALIDATOR_STATS,
            List.of(new org.web3j.abi.datatypes.Address(validatorAddress.toHexString())),
            List.of(
                new TypeReference<Uint256>() {},
                new TypeReference<Uint256>() {},
                new TypeReference<Uint256>() {},
                new TypeReference<Uint256>() {},
                new TypeReference<Uint256>() {},
                new TypeReference<Uint256>() {},
                new TypeReference<Uint256>() {},
                new TypeReference<Uint256>() {},
                new TypeReference<Bool>() {}));

    return callFunction(blockNumber, function, contractAddress)
        .map(result -> parseGetValidatorStatsResult(result, function))
        .orElseThrow(() -> new IllegalStateException(CONTRACT_ERROR_MSG));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Collection<Address> parseGetValidatorsResult(final TransactionSimulatorResult result) {
    final List<Type> resultDecoding = decodeResult(result, getValidatorsFunction);
    final List<org.web3j.abi.datatypes.Address> addresses =
        (List<org.web3j.abi.datatypes.Address>) resultDecoding.get(0).getValue();
    return addresses.stream()
        .map(a -> Address.fromHexString(a.getValue()))
        .collect(Collectors.toList());
  }

  @SuppressWarnings("rawtypes")
  private ValidatorStats parseGetValidatorStatsResult(
      final TransactionSimulatorResult result, final Function function) {
    final List<Type> decoded = decodeResult(result, function);

    return new ValidatorStats(
        ((Uint256) decoded.get(0)).getValue(),
        ((Uint256) decoded.get(1)).getValue(),
        ((Uint256) decoded.get(2)).getValue(),
        ((Uint256) decoded.get(3)).getValue(),
        ((Uint256) decoded.get(4)).getValue(),
        ((Uint256) decoded.get(5)).getValue(),
        ((Uint256) decoded.get(6)).getValue(),
        ((Uint256) decoded.get(7)).getValue(),
        ((Bool) decoded.get(8)).getValue());
  }

  private Optional<TransactionSimulatorResult> callFunction(
      final long blockNumber, final Function function, final Address contractAddress) {
    final Bytes payload = Bytes.fromHexString(FunctionEncoder.encode(function));
    final CallParameter callParams =
        ImmutableCallParameter.builder().to(contractAddress).input(payload).build();
    final TransactionValidationParams transactionValidationParams =
        TransactionValidationParams.transactionSimulatorAllowExceedingBalance();
    return transactionSimulator.process(
        callParams, transactionValidationParams, OperationTracer.NO_TRACING, blockNumber);
  }

  @SuppressWarnings("rawtypes")
  private List<Type> decodeResult(final TransactionSimulatorResult result, final Function function) {
    if (result.isSuccessful()) {
      final List<Type> decodedList =
          FunctionReturnDecoder.decode(
              result.result().getOutput().toHexString(), function.getOutputParameters());

      if (decodedList.isEmpty()) {
        throw new IllegalStateException(
            "Unexpected empty result from validator smart contract call");
      }

      return decodedList;
    } else {
      throw new IllegalStateException(
          "Failed validator smart contract call: " + result.getValidationResult());
    }
  }

  public record ValidatorStats(
      BigInteger observedBlocks,
      BigInteger onlineBlocks,
      BigInteger participatedRounds,
      BigInteger successfulVotes,
      BigInteger unsuccessfulVotes,
      BigInteger selectedRounds,
      BigInteger consecutiveParticipation,
      BigInteger lastParticipatedBlock,
      boolean active) {}
}