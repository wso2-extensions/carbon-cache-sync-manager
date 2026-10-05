### Configure WSO2 Identity Server

1. Add the following jars to the `<IS_HOME>/repository/components/dropins` directory.
   - `org.wso2.carbon.cache.sync.jms.manager-<version>.jar`
   - `jms-api-2.0.1.wso2v1.jar`

   Both jars are available in the release artifacts of the [connector releases](https://github.com/wso2-extensions/carbon-cache-sync-manager/releases). Alternatively, build the project and find the connector jar in the `components/org.wso2.carbon.cache.sync.jms.manager/target` directory.

2. Add the following configuration (update it with your values) to the `deployment.toml` file.
```toml
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
   #### Description
   - **enabled**: Enables or disables the cache invalidation feature.
   - **broker_type**: The broker type. Use `jms` for ActiveMQ and other JMS brokers, or `rabbitmq` for RabbitMQ.
   - **initial_naming_factory**: The initial naming factory. Not required for RabbitMQ.
   - **provider_url**: The provider URL of the broker.
   - **topic_name**: The topic name.
   - **producer_name**: (optional) A unique name for each IS server, used to identify the server that sent a message.
   - **hybrid_mode_enabled**: (optional) Runs the connector along with Hazelcast. Hybrid mode is disabled by default. To enable hybrid mode, also add the following configuration to the `deployment.toml` file.
```toml
[server.cache]
propagation_enabled="true"
```

3. Apply the [version specific configuration](VERSION_SPECIFIC_CONFIG.md) for your IS version, if any.

4. Complete the broker specific setup ([ActiveMQ](../active-mq-resources/ACTIVEMQ_README.md), [RabbitMQ](RABBITMQ_README.md) or [IBM MQ](../ibm-mq-resources/IBMMQ_README.md)), restart the server, and verify that the following log line is printed.
```
INFO {org.wso2.carbon.cache.sync.jms.manager.JMSUtils} - Cache Sync JMS Manager Service bundle activated successfully.
```
