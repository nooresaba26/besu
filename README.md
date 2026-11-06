# Reputation-Based Validator Selection in Hyperledger Besu

A research implementation of reputation-based validator committee selection integrated into Hyperledger Besu’s QBFT consensus framework.

**Researcher:** Kazi Noor e Saba  
**Implementation branch:** `backup-before-adaptive-model`  
**Status:** Experimental research implementation.

## Research overview

This project investigates how validator performance and participation history can inform committee selection in permissioned blockchains.

I developed a mathematical model for reputation-based validator selection and implemented its components within Hyperledger Besu. The implementation extends the existing QBFT infrastructure with reputation scoring, historical time decay, participation balancing, ticket-based committee selection, and contract-backed validator statistics.

This repository contains the modified blockchain client. It should be distinguished from the complete research study and its experimental artifacts.

## My contribution

My contribution focuses on developing the selection model and implementing its integration into Besu, including:

- Reputation scoring based on uptime, success rate, and failure rate.
- Historical score aggregation with time decay.
- Participation balancing using consecutive participation counts.
- Reputation-weighted ticket allocation and probabilistic committee selection.
- Committee-size calculation based on an estimated adversarial ratio.
- Integration of custom validator and proposer selection into QBFT.
- Contract-backed metrics and reputation-update transaction handling.

The underlying Ethereum client and QBFT implementation originate from the upstream Besu project.

## Selection workflow

1. Retrieve candidate validators and their performance statistics from the reputation contract, with fallback behavior when contract data is unavailable.
2. Estimate network conditions and calculate eligibility thresholds.
3. Calculate reputation scores using historical metrics, time decay, and participation balancing.
4. Convert eligible validators’ scores into ticket counts.
5. Calculate a target committee size and per-ticket selection probability.
6. Draw the committee using deterministic hash-derived ticket values, supplementing undersized draws through a deterministic fallback.
7. Select a proposer using block height and retry-round number.
8. Track participation and submit reputation-update transactions following block addition.

## Code guide

| Component | Location |
|---|---|
| Reputation scoring, committee selection, contract integration, and supporting classes | [Validator package](consensus/qbft/src/main/java/org/hyperledger/besu/consensus/qbft/validator/) |
| P-256 VRF generation, verification, key management, and announcement handling | [VRF package](consensus/qbft/src/main/java/org/hyperledger/besu/consensus/qbft/validator/vrf/) |
| Integration with Besu’s QBFT controller | [QbftBesuControllerBuilder.java](app/src/main/java/org/hyperledger/besu/controller/QbftBesuControllerBuilder.java) |
| Proposer rotation | [ReputationProposerSelector.java](consensus/common/src/main/java/org/hyperledger/besu/consensus/common/bft/blockcreation/ReputationProposerSelector.java) |
| Eight-node deployment configuration | [Docker Compose configuration](docker/reputation-network-8/docker-compose.yml) |
| VRF test cases | [P256TaiVrfServiceTest.java](consensus/qbft/src/test/java/org/hyperledger/besu/consensus/qbft/validator/vrf/P256TaiVrfServiceTest.java) |

For a first look at the implementation, start with `ReputationScoreCalculator.java`, `WeightedValidatorSelector.java`, and `ReputationValidatorProvider.java` in the validator package.

## Implementation scope

This branch includes additional experimental components whose integration should be distinguished from the active selection path:

- P-256 VRF proofs are generated and verified, but the current ticket draw uses hash-derived values rather than VRF outputs.
- Fuzzy weights are calculated and stored, while the active time-decayed selection score uses fixed configured weights.
- Adaptive eligibility filtering is activated conditionally using a behavioral risk estimate.
- The committee-size calculation uses a probabilistic model; it does not independently establish a formal security guarantee for the complete protocol.

## Deployment and evaluation

The eight-node Docker Compose configuration references the custom image `custom-besu-reputation:1.0` and passes a reputation-contract address through environment configuration.

Running the complete experiment requires the corresponding custom client build, deployed reputation contract, validator configuration, and experiment setup. The included Compose file alone is not a complete reproduction guide.

Research-specific benchmarking scripts and result datasets are not included in this branch. Upstream Besu benchmarking utilities should not be confused with this project’s experimental evaluation.

## Upstream attribution

This project extends Besu rather than implementing an Ethereum client from scratch. The upstream license and attribution are retained; see [LICENSE](LICENSE).

The original Besu documentation follows below.



# Besu Ethereum Client
 [![CircleCI](https://circleci.com/gh/besu-eth/besu/tree/main.svg?style=svg)](https://circleci.com/gh/besu-eth/besu/tree/main)
 [![CodeQL](https://github.com/besu-eth/besu/actions/workflows/codeql.yml/badge.svg)](https://github.com/besu-eth/besu/actions/workflows/codeql.yml)
 [![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/3174/badge)](https://bestpractices.coreinfrastructure.org/projects/3174)
 [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/besu-eth/besu/blob/main/LICENSE)
 [![Discord](https://img.shields.io/discord/905194001349627914?logo=Hyperledger&style=plastic)](https://discord.com/invite/hyperledger)
 [![Twitter Follow](https://img.shields.io/twitter/follow/HyperledgerBesu)](https://twitter.com/HyperledgerBesu)

[Download](https://github.com/besu-eth/besu/releases)

Besu is an Apache 2.0 licensed, MainNet compatible, Ethereum client written in Java.

## Useful Links

* [Besu User Documentation]
* [Besu Issues]
* [Besu Wiki](https://lf-hyperledger.atlassian.net/wiki/spaces/BESU/)
* [How to Contribute to Besu](https://lf-hyperledger.atlassian.net/wiki/spaces/BESU/pages/22156850/How+to+Contribute)
* [Besu Roadmap & Planning](https://lf-hyperledger.atlassian.net/wiki/spaces/BESU/pages/22154278/Besu+Roadmap+Planning)


## Issues 

Besu issues are tracked [in the github issues tab][Besu Issues].
See our [guidelines](https://lf-hyperledger.atlassian.net/wiki/spaces/BESU/pages/22154243/Issues) for more details on searching and creating issues.

If you have any questions, queries or comments, [Besu channel on Discord] is the place to find us.


## Besu Users

To install the Besu binary, follow [these instructions](https://besu.hyperledger.org/public-networks/get-started/install/binary-distribution).    

## Besu Developers

* [Contributing Guidelines]
* [Coding Conventions](https://lf-hyperledger.atlassian.net/wiki/spaces/BESU/pages/22154259/Coding+Conventions)
* [Command Line Interface (CLI) Style Guide](https://lf-hyperledger.atlassian.net/wiki/spaces/BESU/pages/22154260/Besu+CLI+Style+Guide)
* [Besu User Documentation] for running and using Besu


### Development

Instructions for how to get started with developing on the Besu codebase. Please also read the
[wiki](https://lf-hyperledger.atlassian.net/wiki/spaces/BESU/pages/22154251/Pull+Requests) for more details on how to submit a pull request (PR).  

* [Checking Out and Building](https://lf-hyperledger.atlassian.net/wiki/spaces/BESU/pages/22154264/Building+from+source)
* [Code Coverage](https://lf-hyperledger.atlassian.net/wiki/spaces/BESU/pages/22154288/Code+coverage)
* [Logging](https://lf-hyperledger.atlassian.net/wiki/spaces/BESU/pages/22154291/Logging) or the [Documentation's Logging section](https://besu.hyperledger.org/public-networks/how-to/monitor/logging)

#### Dependency Verification

This project uses [Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html). When adding or updating dependencies, regenerate `gradle/verification-metadata.xml` with:

```shell
./gradlew --write-verification-metadata sha256 resolveSourceArtifacts
```

The `resolveSourceArtifacts` task ensures source JARs are included in the metadata, which is required for IDE sync (e.g. IntelliJ automatically downloads sources).

### Profiling Besu

Besu supports performance profiling using [Async Profiler](https://github.com/async-profiler/async-profiler), a low-overhead sampling profiler.  
You can find setup and usage instructions in the [Profiling Guide](docs/PROFILING.md).

Profiling can help identify performance bottlenecks in block processing, transaction validation, and EVM execution.  
Please ensure the profiler is run as the same user that started the Besu process.

## Release Notes

[Release Notes](CHANGELOG.md)

## Reference Tests and JSON Tracing

Besu includes support for running Ethereum reference tests and generating detailed EVM execution traces.

To learn how to run the tests and enable opcode-level JSON tracing for debugging and correctness verification, see the [Reference Test Execution and Tracing Guide](REFERENCE_TESTS.md).

[Besu Issues]: https://github.com/besu-eth/besu/issues
[Besu User Documentation]: https://besu.hyperledger.org
[Besu channel on Discord]: https://discord.com/invite/hyperledger
[Contributing Guidelines]: CONTRIBUTING.md
