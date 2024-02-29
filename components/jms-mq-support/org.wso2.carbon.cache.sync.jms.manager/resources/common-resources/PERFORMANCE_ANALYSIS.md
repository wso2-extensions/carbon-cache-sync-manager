### Introduction

To evaluate the performance impact of using an additional connector that would pass the cache invalidation messages 
to a third-party message broker, we performed several performance test rounds. 

This is the base deployment for performance tests where there are two clusters each having two nodes of Identity 
Server. All the IS nodes are connected to a shared database.

![perf_test_deployment.png](./perf_test_deployment.png)

For the first test round, we selected a deployment that will have only one cluster active all the time and with the
following configurations. This performance test is intended to validate, whether
there is any performance impact for a given data center with and without the connector in place.

Setup 1: One cluster serving the traffic and the connector is disabled
Setup 2: One cluster serving the traffic, the connector is enabled and connected with Active MQ
Setup 3: One cluster serving the traffic, the connector is enabled and connected with IBM MQ

![perf_test_round1.png](resources/common-resources/perf_test_round1_setup.png)

Here are the test results,


Setup 1: Without the cache invalidation message broker connector

| Scenario Name                                  | Concurrency | # Samples | Throughput (transactions/sec) | Average Response Time (ms) | error % |
|------------------------------------------------|-------------|-----------|-------------------------------|----------------------------|---------|
| OIDC Auth Code Grant Redirect With Consent     | 50          | 135343    | 451.27                        | 107.71                     | 0.00%   |
|                                                | 150         | 144232    | 480.13                        | 303.86                     | 0.00%   |
|                                                | 300         | 141126    | 470.31                        | 612.12                     | 0.00%   |
|                                                | 500         | 142079    | 473.3                         | 992.95                     | 1.79%   |
| Password Grant Token Request and revocation   | 50          | 71736     | 239.1                         | 205.32                     | 0.00%   |
|                                                | 150         | 71862     | 239.2                         | 615.47                     | 0.00%   |
|                                                | 300         | 71183     | 236.81                        | 1242.13                    | 0.00%   |
|                                                | 500         | 69209     | 229.6                         | 2130.84                    | 1.47%   |

Setup 2: Active MQ as the message broker for the cache invalidation messages.

| Scenario Name                                  | Concurrency | # Samples | Throughput (Requests/sec) | Average Response Time (ms) | error % |
|------------------------------------------------|-------------|-----------|----------------------------|----------------------------|---------|
| OIDC Auth Code Grant Redirect With Consent     | 50          | 133414    | 444.83                     | 109.21                     | 0.00%   |
|                                                | 150         | 144186    | 480.23                     | 304.02                     | 0.00%   |
|                                                | 300         | 142084    | 473.08                     | 603.25                     | 0.00%   |
|                                                | 500         | 143606    | 477.53                     | 961.88                     | 2.35%   |
| Password Grant Token Request and revocation   | 50          | 71674     | 238.94                     | 205.48                     | 0.00%   |
|                                                | 150         | 71744     | 238.81                     | 616.52                     | 0.00%   |
|                                                | 300         | 71261     | 237.05                     | 1240.82                    | 0.00%   |
|                                                | 500         | 69764     | 231.11                     | 2116.43                    | 0.51%   |

Setup 3: IBM MQ as the message broker for the cache invalidation messages.

| Scenario Name                                  | Concurrency | # Samples | Throughput (transactions/sec) | Average Response Time (ms) | error % |
|------------------------------------------------|-------------|-----------|-------------------------------|----------------------------|---------|
| OIDC Auth Code Grant Redirect With Consent     | 50          | 135343    | 451.27                        | 107.71                     | 0.00%   |
|                                                | 150         | 144232    | 480.13                        | 303.86                     | 0.00%   |
|                                                | 300         | 141126    | 470.31                        | 612.12                     | 0.00%   |
|                                                | 500         | 142079    | 473.3                         | 992.95                     | 1.79%   |
| Password Grant Token Request and revocation   | 50          | 71736     | 239.1                         | 205.32                     | 0.00%   |
|                                                | 150         | 71862     | 239.2                         | 615.47                     | 0.00%   |
|                                                | 300         | 71183     | 236.81                        | 1242.13                    | 0.00%   |
|                                                | 500         | 69209     | 229.6                         | 2130.84                    | 1.47%   |
