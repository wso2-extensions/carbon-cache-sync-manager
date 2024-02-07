### How to Build
1. Build the maven project and copy the jar file to the `<IS_HOME>/repository/components/dropins` directory.
2. Add the following identity.xml.j2 template configurations
```yaml
  {% if cache_invalidator.mb.enabled is defined %}
    <CacheInvalidator>
      <MB>
        <Enabled>{{cache_invalidator.mb.enabled}}</Enabled>
        <Type>{{cache_invalidator.mb.broker_type}}</Type>
        <InitialNamingFactory>{{cache_invalidator.mb.initial_naming_factory}}</InitialNamingFactory>
        <ProviderURL>{{cache_invalidator.mb.provider_url}}</ProviderURL>
        <TopicName>{{cache_invalidator.mb.topic_name}}</TopicName>
        <ProducerName>{{cache_invalidator.mb.producer_name}}</ProducerName>
        <Username>{{cache_invalidator.mb.username}}</Username>
        <Password>{{cache_invalidator.mb.password}}</Password>
        <HybridMode>{{cache_invalidator.mb.hybrid_mode_enabled}}</HybridMode>
      </MB>
    </CacheInvalidator>
  {% endif %}
```
**Note:** For **IS 5.11.0** this should have been already added. 

3. Then add the following config(update it with your values) in the `deployment.toml` file
```yaml
[cache_invalidator.mb]
enabled="true"
broker_type="jms"
initial_naming_factory="org.apache.activemq.jndi.ActiveMQInitialContextFactory"
provider_url="failover:tcp://localhost:61616"
topic_name="CacheTopic"
producer_name="producer1"
hybrid_mode_enabled="true"
username="guest"
password="guest"
```
**Note:**
- You need to use the **failover** transport in the provider URL to support retrying.
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