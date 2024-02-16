## Configure the WSO2 IS

1. Follow the instructions in the [IS_README.md](../common-resources/IS_README.md)
2. Then add the following config(update it with your values) in the deployment.toml file
  ```
  [cache_invalidator.mb]
  enabled="true"
  broker_type="jms"
  initial_naming_factory="com.sun.jndi.fscontext.RefFSContextFactory"
  provider_url="file:/path/to/bindings/directory"
  topic_name="this_not_considered_for_ibm"
  producer_name="producer1"
  hybrid_mode_enabled="true"
  username="guest"
  password="guest"
  ```
  In the above configs,
  * `provider_url` = this is the file location of the `.bindings` file which is created in the above step 
  * `topic_name` = for IBM MQ, the required topic is picked from the `.binding` config file. The topic configured here 
    is not considered. 
  * `username` and `password` = should be the credentials of the user who have permissions to publish and subscribe to a topic
3. Copy the following JAR files from the `<IBM_MQ_HOME>/java/lib/` directory (where <IBM_MQ_HOME> refers to the IBM MQ installation directory) to the `<IS_HOME>/repository/components/lib/` directory.
   * com.ibm.mq.allclient.jar
   * fscontext.jar
   * providerutil.jar
5. Restart the WSO2 identity server
