package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.BlockHeader;

public class StaticValidatorMetricsProvider implements ValidatorMetricsProvider {

  @Override
  public double uptime(final Address validator, final BlockHeader parentHeader) {
    return 1.0;
  }

  @Override
  public double successRate(final Address validator, final BlockHeader parentHeader) {
    return 1.0;
  }

  @Override
  public double failureRate(final Address validator, final BlockHeader parentHeader) {
    return 0.0;
  }
}