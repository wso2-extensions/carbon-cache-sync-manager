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
**Note:** For latest **IS 5.11.0** this should have been already added. 

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
#### Description:
- **enabled**: This property is used to enable or disable the cache invalidation feature.
- **broker_type**: This property is used to specify the broker type. It can be either `jms` (for ActiveMQ & other JMS brokers) or `rabbitmq` (for RabbitMQ).
- **initial_naming_factory** : This property is used to specify the initial naming factory.
  - note: this not required for RabbitMQ
- **provider_url**: This property is used to specify the provider URL.
- **topic_name**: This property is used to specify the topic name.
- **producer_name**: (optional property) This should be unique for each IS server. (Facilitates in identifying the server that sent the message.) This is .
- **hybrid_mode_enabled**: (optional property) This is used to run along with Hazelcast. (If not specified, hybrid mode is disabled by default.)
  To enabled hybrid mode, you need to add the following config in the `deployment.toml` file
```yaml
[server.cache]
propagation_enabled="true"
 ```
