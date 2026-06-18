package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.BlockHeader;

public interface ValidatorMetricsProvider {

  double uptime(Address validator, BlockHeader parentHeader);

  double successRate(Address validator, BlockHeader parentHeader);

  double failureRate(Address validator, BlockHeader parentHeader);
}