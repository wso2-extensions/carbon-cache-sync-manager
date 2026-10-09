### Version specific configuration

Apply the following configuration in addition to the [Common Setup for IS](IS_README.md) for the IS version you are using.

#### IS 7.3.0
- **Mode:** use non-hybrid mode (`hybrid_mode_enabled="false"`).
- **ActiveMQ client libraries:** copy the following jars from the `<ACTIVEMQ_HOME>/lib` directory of ActiveMQ 5.18.7 to the `<IS_HOME>/repository/components/lib` directory, instead of the libraries listed in the [ActiveMQ setup guide](../active-mq-resources/ACTIVEMQ_README.md).
  - activemq-broker-5.18.7.jar
  - activemq-client-5.18.7.jar
  - hawtbuf-1.11.jar

  The JMS API is provided by `jms-api-2.0.1.wso2v1.jar` and slf4j is provided by IS, so the JMS specification and slf4j jars are not required.
