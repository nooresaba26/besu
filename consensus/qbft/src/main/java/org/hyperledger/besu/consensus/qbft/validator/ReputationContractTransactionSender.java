package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;

public class ReputationContractTransactionSender {

  private static final Logger LOG =
      LoggerFactory.getLogger(ReputationContractTransactionSender.class);

  private static final String RECORD_FINALIZED_BLOCK = "recordFinalizedBlock";

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

    LOG.info(
        "Prepared recordFinalizedBlock transaction for contract {} at block {} with payload size {} bytes",
        contractAddress,
        blockNumber,
        payload.size());
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