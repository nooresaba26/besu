package org.hyperledger.besu.consensus.qbft.validator;

public class ReputationSelectionConfig {

  private final int targetCommitteeSize;
  private final int minimumCommitteeSize;
  private final int ticketScalingFactor;
  private final double alpha;
  private final double beta;
  private final double gamma;
  private final double lambda;
  private final double delta;

  public ReputationSelectionConfig() {
    this.targetCommitteeSize = 10;
    this.minimumCommitteeSize = 4;
    this.ticketScalingFactor = 100;
    this.alpha = 0.3;
    this.beta = 0.3;
    this.gamma = 0.4;
    this.lambda = 0.8;
    this.delta = 0.7;
  }

  public int getTargetCommitteeSize() {
    return targetCommitteeSize;
  }

  public int getMinimumCommitteeSize() {
    return minimumCommitteeSize;
  }

  public int getTicketScalingFactor() {
    return ticketScalingFactor;
  }

  public double getAlpha() {
    return alpha;
  }

  public double getBeta() {
    return beta;
  }

  public double getGamma() {
    return gamma;
  }

  public double getLambda() {
    return lambda;
  }

  public double getDelta() {
    return delta;
  }
}
