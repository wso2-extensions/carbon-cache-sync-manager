### Introduction
This connector supports cache invalidation message exchange across multi-cluster deployments, which enables the deployment of Identity Server (IS) in Active-Active mode. It establishes connections between IS servers and a designated ActiveMQ broker (to be deployed independently) through a topic using a publish/subscribe (pub/sub) mechanism.

### How to Build
1. Build the maven project and copy the jar file to the `<IS_HOME>/repository/components/dropins` directory.
2. Add the following identity.xml.j2 template configurations
```yaml
 {% if cache_invalidator.active_mq.broker_url is defined %}
   <CacheInvalidator>
   <ActiveMQ>
   <BrokerURL>{{cache_invalidator.active_mq.broker_url}}</BrokerURL>
   <Enabled>{{cache_invalidator.active_mq.enabled}}</Enabled>
   <TopicName>{{cache_invalidator.active_mq.topic_name}}</TopicName>
   <ProducerName>{{cache_invalidator.active_mq.producer_name}}</ProducerName>
   <HybridMode>{{cache_invalidator.active_mq.hybrid_mode_enabled}}</HybridMode>
   </ActiveMQ>
   </CacheInvalidator>
   {% endif %}
```

3. Then add the following config(update it with your values) in the `deployment.toml` file
```yaml
[cache_invalidator.active_mq]
broker_url="failover:tcp://localhost:61616"
enabled="true"
topic_name="ISCacheSyncTopic"
producer_name="producerA"
hybrid_mode_enabled="true"
```
**Note:**
- You need to use the **failover** transport in the broker URL to support retrying.
- topic_name refers to the topic in the broker.
- producer_name should be unique for each IS server. (Facilitates in identifying the server that sent the message.) This is optional.
- hybrid_mode_enabled is used to run along with Hazelcast. (This is optional. If not specified, hybrid mode is disabled by default.)
  To enabled hybrid mode, you need to add the following config in the `deployment.toml` file
```yaml
[server.cache]
propagation_enabled="true"
 ```
  
4. Restart the server.