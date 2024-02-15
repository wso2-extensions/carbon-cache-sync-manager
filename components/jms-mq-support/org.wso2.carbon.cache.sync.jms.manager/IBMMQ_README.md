#### Configuring IBM MQ

* Setup IBM MQ using their official documentation. 
  * You may follow https://developer.ibm.com/learningpaths/ibm-mq-badge/create-configure-queue-manager
  * **Troubleshooting**: 
    * If you are using MAC with Apple chip, you may need to create the docker image yourself. Refer this link for 
      more information. 
      https://community.ibm.com/community/user/integration/blogs/richard-coppen/2023/06/30/ibm-mq-9330-container-image-now-available-for-appl 
    * When building the docker, the IBM MQ version is defined by [this config](https://github.com/ibm-messaging/mq-container/blob/master/config.env#L4). The given version should be available in [this 
      directory](https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/messaging/mqadv/) for download. 
      Otherwise, the image creation will fail.
    * If you are using Rancher, the IBM MQ console might not be accessible over 9443 port mapping.
      Hence, use 9243:9443 port mapping when creating the docker container. 
  * Make sure to configure the channel authentication. For the ease of use, run the following two commands to disable 
    channel authentication.
    
    `runmqsc QM1`

     `ALTER QMGR CHLAUTH(DISABLED)`

     `REFRESH SECURITY TYPE(CONNAUTH)`
  * Create a topic for the cache invalidation syncing and make sure the user who’s accessing the JMS application in 
    the next steps will have permission to publish and subscribe to the topic.
  * Create a `.bindings` file for the created topic and save it in a desired location in your computer. You need this 
    when configuring the Identity Server (You can refer [this document](https://ei.docs.wso2.com/en/latest/micro-integrator/setup/brokers/configure-with-IBM-websphereMQ/#generating-the-bindings-file) on creating the .binding file)

#### Configure the WSO2 IS

1. Follow the instructions in the [IS_README.md](IS_README.md)
2. Then add the following config(update it with your values) in the deployment.toml file
  ```
  [cache_invalidator.mb]
  enabled="true"
  broker_type="jms"
  initial_naming_factory="com.sun.jndi.fscontext.RefFSContextFactory"
  provider_url="file:/Documents/conf/jndi"
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
3. Build the maven project **[carbon-cache-sync-manager]** and copy **jms-api_2-2.0.1.wso2v1.jar** which resides inside 
  `carbon-cache-sync-manager/components/jms-mq-support/jms-api-orbit/2.0.1.wso2v1/target` to the `<IS_HOME>/repository/components/dropins/`
4. Copy the following JAR files from the `<IBM_MQ_HOME>/java/lib/` directory (where <IBM_MQ_HOME> refers to the IBM MQ installation directory) to the `<IS_HOME>/repository/components/lib/` directory.
   * com.ibm.mq.allclient.jar
   * fscontext.jar
   * providerutil.jar
5. Restart the WSO2 identity server
