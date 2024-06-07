### How to Build
1. Follow the instructions in the [IS_README.md](../common-resources/IS_README.md)

2. Then add the following config(update it with your values) in the `deployment.toml` file
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

3. Copy the following client libraries from the `ACTIVEMQ_HOME/lib` directory to the `IS_HOME/repository/components/lib` directory.
- activemq-broker-5.16.3.jar
- activemq-client-5.16.3.jar
- geronimo-j2ee-management_1.1_spec-1.0.1.jar
- geronimo-jms_1.1_spec-1.1.1.jar
- hawtbuf-1.11.jar
- slf4j-api-1.7.31.jar

4. Restart the server.