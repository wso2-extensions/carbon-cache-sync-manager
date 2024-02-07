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
**Note:** For **IS 5.11.0** this should have been already added. 

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
  
4. Copy the following client libraries from the `ACTIVEMQ_HOME/lib` directory to the `IS_HOME/repository/components/lib` directory.
- activemq-broker-5.16.3.jar
- activemq-client-5.16.3.jar
- geronimo-j2ee-management_1.1_spec-1.0.1.jar
- geronimo-jms_1.1_spec-1.1.1.jar
- hawtbuf-1.11.jar
- slf4j-api-1.7.31.jar

5. Restart the server.