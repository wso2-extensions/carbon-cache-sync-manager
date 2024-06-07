### Introduction

This repository contains the connector that supports cache invalidation message exchange across multi-cluster deployments, enabling the deployment of Identity Server (IS) in Active-Active mode. It establishes connections between IS servers and a designated central broker (to be deployed independently) through a topic using a publish/subscribe (pub/sub) mechanism.

### Why is it mandatory to use this connector?

Applications are often deployed across multiple centers to ensure high availability and better disaster recovery, aiming to achieve good performance and reliability. However, this multi-center deployment introduces challenges with caching, particularly the cache coherence problem.
Within a single cluster the cache coherence issue is fixed by relying on Hazelcast (to pass cache-invalidation/sync messages). However, Hazelcast cannot be used to transmit sync messages across separate clusters.

For example, let's consider a scenario where two clusters of WSO2 Identity Server (IS) are deployed, Cluster A and Cluster B. If Cluster A issues an access token and subsequently receives a token revocation request, Cluster A may not be aware of this event. Consequently, Cluster A continues to treat the previously issued access token as valid in its cache, even though it has been revoked by Cluster B. This inconsistency in cache states across clusters leads to the cache coherence problem. 

Attempting to deploy IS as multiple Active-Active clusters exacerbates the cache coherence problem. Inconsistent caches between clusters can lead to unexpected behaviors and potential security risks. While disabling all cache layers may seem like a solution, it comes at the cost of significant performance degradation

### Deployment

The Message Broker(MB) acts as a central point for cache invalidation message exchange across clusters. Need to deploy the MB in a central location, and configure the IS servers to connect to the MB. The MB is responsible for propagating cache invalidation messages across clusters, ensuring that the cache states are consistent across all clusters, whereas Hazlecast takes care of invalidation message propagation within a cluster.
- Here only one node from each data-center is connected to the MB and passes invalidation message across clusters
- Need to have Database in sync across data-centers. This is a must for this approach to work.
   
![hybrid_approach.png](components/org.wso2.carbon.cache.sync.jms.manager/resources/common-resources/hybrid_approach.png)

### Getting started

To get started with the connector, you can follow the below docs based on the Message Broker you are using.

- **Common**
  - Common Setup for IS: [Refer](components/org.wso2.carbon.cache.sync.jms.manager/resources/common-resources/IS_README.md)

- **Connect with ActiveMQ**
  1. Setup IS for ActiveMQ: [Refer](components/org.wso2.carbon.cache.sync.jms.manager/resources/active-mq-resources/ACTIVEMQ_README.md)
  2. Deploy ActiveMQ Message Broker:  [Refer](components/org.wso2.carbon.cache.sync.jms.manager/resources/active-mq-resources/ACITVMQ_MB_DEPLOYMENT.md)

- **Connect with RabbitMQ**
  - Setup IS for RabbitMQ: [Refer](components/org.wso2.carbon.cache.sync.jms.manager/resources/common-resources/RABBITMQ_README.md)

- **Connect with IBM MQ**
    1. Setup IS for IBM MQ: [Refer](components/org.wso2.carbon.cache.sync.jms.manager/resources/ibm-mq-resources/IBMMQ_README.md)
    2. Deploy IBM MQ Message Broker: [Refer](components/org.wso2.carbon.cache.sync.jms.manager/resources/ibm-mq-resources/IBMMQ_MB_DEPLOYMENT.md)

  
### Performance Analysis

We have done an analysis on the impact of this connector for the overall performance of the IS deployments. Based on 
the test results, it can be concluded that there is no significant impact to the performance.

Refer [Performance Analysis](components/org.wso2.carbon.cache.sync.jms.manager/resources/common-resources/PERFORMANCE_ANALYSIS.md) for more details on the performance 
tests and the results.

### Performance Improvement

We can improve the performance of Message Brokers by reducing the cache invalidation messages that needs to be send across. Ideally we don't need sync clusters for all the flows. Refer [Performance Improvement](components/org.wso2.carbon.cache.sync.jms.manager/resources/common-resources/PERFORMANCE_IMPROVEMENT.md) for more details.

### Periodic CleanUp

We can further enhance our cache management strategy by implementing a periodic cleanup mechanism for the local cache on each node. Refer [Periodic CleanUp](components/org.wso2.carbon.cache.sync.jms.manager/resources/common-resources/PERIODIC_CLEANUP.md) for more details.