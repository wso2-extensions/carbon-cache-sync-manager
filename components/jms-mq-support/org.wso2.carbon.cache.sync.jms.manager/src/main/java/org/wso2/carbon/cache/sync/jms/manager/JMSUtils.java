/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.cache.sync.jms.manager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.io.IOException;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Util class for the ActiveMQ cache manager Service.
 */
public class JMSUtils {

//    public static final String BROKER_URL_PROPERTY = "CacheInvalidator.ActiveMQ.BrokerURL";
//    public static final String ACTIVEMQ_INVALIDATOR_ENABLED_PROPERTY = "CacheInvalidator.ActiveMQ.Enabled";
//    public static final String ACTIVEMQ_CACHE_TOPIC_PROPERTY = "CacheInvalidator.ActiveMQ.TopicName";
//    public static final String ACTIVEMQ_PRODUCER_NAME_PROPERTY = "CacheInvalidator.ActiveMQ.ProducerName";
//    public static final String RUN_IN_HYBRID_MODE_PROPERTY = "CacheInvalidator.ActiveMQ.HybridMode";


    public static final String MB_TYPE = "CacheInvalidator.MB.Type";
    public static final String INVALIDATOR_ENABLED_PROPERTY = "CacheInvalidator.MB.Enabled";
    public static final String RUN_IN_HYBRID_MODE_PROPERTY = "CacheInvalidator.MB.HybridMode";
    public static final String PRODUCER_NAME_PROPERTY = "CacheInvalidator.MB.ProducerName";

    public static final String JNDI_INITIAL_FACTORY_PROP_NAME = "java.naming.factory.initial";
    public static final String JNDI_PROVIDER_URL_PROP_NAME = "java.naming.provider.url";
    public static final String JNDI_TOPIC_PROP_NAME = "topic.exampleTopic";
    public static final String MB_USERNAME_PROP_NAME = "transport.jms.UserName";
    public static final String MB_PASSWORD_PROP_NAME = "transport.jms.Password";

    public static final String JNDI_INITIAL_FACTORY_PROP_VALUE = "CacheInvalidator.MB.InitialNamingFactory";
    public static final String JNDI_PROVIDER_URL_PROP_VALUE = "CacheInvalidator.MB.ProviderURL";
    public static final String JNDI_TOPIC_PROP_NAME_VALUE = "CacheInvalidator.MB.TopicName";
    public static final String MB_USERNAME_PROP_VALUE = "CacheInvalidator.MB.Username";
    public static final String MB_PASSWORD_PROP_VALUE = "CacheInvalidator.MB.Password";

    // Cache name prefix of local cache.
    public static final String LOCAL_CACHE_PREFIX = "$__local__$.";

    // Cache name prefix of clear all.
    public static final String CLEAR_ALL_PREFIX = "$__clear__all__$.";

    // Default producer retry limit.
    public static final int PRODUCER_RETRY_LIMIT = 30;

    public static final String SENDER = "sender";


    private JMSUtils() {

    }

    private static UnaryOperator<String> getConfiguredStringValue = (String config) -> {
        String propertyValue = IdentityUtil.getProperty(config);
        return StringUtils.isNotBlank(propertyValue) ? propertyValue.trim() : null;
    };

    private static BiFunction<String, Boolean, Boolean> getConfiguredBooleanValue = (String config,
                                                                                     Boolean defaultValue) -> {
        String propertyValue = IdentityUtil.getProperty(config);
        return StringUtils.isNotBlank(propertyValue) ? Boolean.parseBoolean(propertyValue.trim()) : defaultValue;
    };

    /**
     * Configured producer name for the node.
     *
     * @return String Producer name.
     */
    public static String getProducerName() {

        return getConfiguredStringValue.apply(PRODUCER_NAME_PROPERTY);
    }

    /**
     * Checks if the Hybrid mode is enabled.
     *
     * @return Boolean representing the enabled state of the hybrid mode.
     */
    public static boolean getRunInHybridModeProperty() {

        String propertyValue = IdentityUtil.getProperty(RUN_IN_HYBRID_MODE_PROPERTY);
        return StringUtils.isNotBlank(propertyValue) ? Boolean.parseBoolean(propertyValue) : false;
    }

    /**
     * Checks if the ActiveMQ Cache Invalidator is enabled.
     *
     * @return Boolean representing the enabled state, or null if the property is not set.
     */
    @SuppressFBWarnings
    public static Boolean isMBCacheInvalidatorEnabled() {

        String propertyValue = IdentityUtil.getProperty(INVALIDATOR_ENABLED_PROPERTY);
        return StringUtils.isNotBlank(propertyValue) ? Boolean.parseBoolean(propertyValue) : null;
    }

    public static InitialContext createInitialContext() throws NamingException, IOException {

        Properties properties = new Properties();
        String brokerType = getBrokerType();;
        if ("jms".equalsIgnoreCase(brokerType)) {
            properties.put(JNDI_INITIAL_FACTORY_PROP_NAME,
                    getConfiguredStringValue.apply(JNDI_INITIAL_FACTORY_PROP_VALUE));
        }
        properties.put(JNDI_PROVIDER_URL_PROP_NAME, getConfiguredStringValue.apply(JNDI_PROVIDER_URL_PROP_VALUE));
        properties.put(JNDI_TOPIC_PROP_NAME, getConfiguredStringValue.apply(JNDI_TOPIC_PROP_NAME_VALUE));
        return new InitialContext(properties);

//            String jndiPropertiesFilePath = "/Users/inthirakumaarantharmakulasingham/IAM/activeActive/staging/jndi.properties";
//
//            // Load the JNDI properties from the specified file
//            Properties properties = new Properties();
//            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(jndiPropertiesFilePath));
//
//
//            // Object obj = context.lookup("your/jndi/name");

//            return new InitialContext(properties);

//        else if ("rabbitmq".equalsIgnoreCase(brokerType)) {
//            properties.put("java.naming.factory.initial", "com.rabbitmq.jms.admin.RMQInitialContextFactory");
//            properties.put("java.naming.provider.url", "localhost:5672");
//            properties.put("java.naming.security.principal", "guest");
//            properties.put("java.naming.security.credentials", "guest");
//            properties.put("topic.exampleTopic", "CacheTopic");
//        }

//        if ("jms".equalsIgnoreCase(brokerType)) {
//            properties.put(JNDI_INITIAL_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
//        }
//        properties.put(JNDI_PROVIDER_URL, "failover:tcp://localhost:61616");
//        properties.put(JNDI_TOPIC, "CacheTopic");
//        properties.put(PARAM_JMS_USERNAME, "guest");
//        properties.put(PARAM_JMS_PASSWORD, "guest");
    }

    public static Connection createConnection(ConnectionFactory conFactory) throws JMSException {

        String username = getConfiguredStringValue.apply(MB_USERNAME_PROP_VALUE);
        String password = getConfiguredStringValue.apply(MB_PASSWORD_PROP_VALUE);
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            return conFactory.createConnection(username, password);
        }
        return conFactory.createConnection();
    }

    /**
     * Checks if the Hybrid mode is enabled.
     *
     * @return Boolean representing the enabled state of the hybrid mode.
     */
    public static String getBrokerType() {

        String propertyValue = IdentityUtil.getProperty(MB_TYPE);
        return StringUtils.isNotBlank(propertyValue) ? propertyValue.trim() : null;
    }
}
