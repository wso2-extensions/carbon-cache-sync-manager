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

import com.rabbitmq.jms.admin.RMQConnectionFactory;
import org.apache.axiom.om.OMElement;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.namespace.QName;

/**
 * Util class for the JMS cache manager Service.
 */
public class JMSUtils {

    private static final Log log = LogFactory.getLog(JMSUtils.class);
    public static final String MB_TYPE = "CacheInvalidator.MB.Type";
    public static final String INVALIDATOR_ENABLED_PROPERTY = "CacheInvalidator.MB.Enabled";
    public static final String RUN_IN_HYBRID_MODE_PROPERTY = "CacheInvalidator.MB.HybridMode";
    public static final String PRODUCER_NAME_PROPERTY = "CacheInvalidator.MB.ProducerName";
    public static final String JNDI_INITIAL_FACTORY_PROP_NAME = "java.naming.factory.initial";
    public static final String JNDI_PROVIDER_URL_PROP_NAME = "java.naming.provider.url";
    public static final String JNDI_TOPIC_PROP_NAME = "topic.exampleTopic";
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
    public static final int PRODUCER_RETRY_LIMIT = 10;
    public static final String SENDER = "sender";
    public static final String BROKER_TYPE_RABBITMQ = "rabbitmq";
    public static final String BROKER_TYPE_JMS = "jms";
    public static final String LOOKUP_CONNECTION_FACTORY = "ConnectionFactory";
    public static final String LOOKUP_TOPIC = "exampleTopic";
    public static final String CACHE_INVALIDATOR_ELEMENT = "CacheInvalidator";
    public static final String CACHE_MANAGER_ELEMENT = "CacheManager";

    private static Map<String, List<String>> mbCacheListConfigurationHolder;

    private JMSUtils() {

    }

    private static UnaryOperator<String> getConfiguredStringValue = (String config) -> {
        String propertyValue = IdentityUtil.getProperty(config);
        return StringUtils.isNotBlank(propertyValue) ? propertyValue.trim() : null;
    };

    private static BiFunction<String, Boolean, Boolean> getConfiguredBooleanValue = (String config,
                                                                                     Boolean defaultValue) -> {
        String propertyValue = IdentityUtil.getProperty(config);
        return StringUtils.isNotBlank(propertyValue) ? Boolean.valueOf(propertyValue.trim()) : defaultValue;
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

        return getConfiguredBooleanValue.apply(RUN_IN_HYBRID_MODE_PROPERTY, false);
    }

    /**
     * Checks if the Cache Invalidator is enabled.
     *
     * @return Boolean representing the enabled state, or null if the property is not set.
     */
    public static Boolean isMBCacheInvalidatorEnabled() {

        return getConfiguredBooleanValue.apply(INVALIDATOR_ENABLED_PROPERTY, null);
    }

    /**
     * Creates initial context for the JMS message broker.
     *
     * @return InitialContext initialContext.
     */
    public static InitialContext createInitialContext() throws NamingException, IOException {

        Properties properties = new Properties();
        String brokerType = getBrokerType();;
        if (BROKER_TYPE_JMS.equalsIgnoreCase(brokerType)) {
            properties.put(JNDI_INITIAL_FACTORY_PROP_NAME,
                    getConfiguredStringValue.apply(JNDI_INITIAL_FACTORY_PROP_VALUE));
        }
        properties.put(JNDI_PROVIDER_URL_PROP_NAME, getConfiguredStringValue.apply(JNDI_PROVIDER_URL_PROP_VALUE));
        properties.put(JNDI_TOPIC_PROP_NAME, getConfiguredStringValue.apply(JNDI_TOPIC_PROP_NAME_VALUE));
        return new InitialContext(properties);
    }

    /**
     * Creates connections using the given connection factory for JMS message brokers.
     *
     * @param conFactory connection factory.
     * @return Connection jms connection
     * @throws JMSException
     */
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

    /**
     * Return connection factory based on the broker type.
     *
     * @param context Initial JNDI context.
     * @return ConnectionFactory
     * @throws NamingException
     * @throws JMSException
     */
    public static ConnectionFactory getConnectionFactory(InitialContext context) throws NamingException, JMSException {

        String brokerType = getBrokerType();;
        if (BROKER_TYPE_RABBITMQ.equalsIgnoreCase(brokerType)) {
            return createRabbitMQConnectionFactory();
        } else if (BROKER_TYPE_JMS.equalsIgnoreCase(brokerType)) {
            return (ConnectionFactory) context.lookup(LOOKUP_CONNECTION_FACTORY);
        }
        return null;
    }

    /**
     * Returns topic for the pub/sub connection.
     *
     * @param context Initial JNDI context.
     * @param session Session of the connection.
     * @return Topic used to communicate messages.
     * @throws NamingException
     * @throws JMSException
     */
    public static Topic getCacheTopic(InitialContext context, Session session) throws NamingException, JMSException {

        String brokerType = getBrokerType();;
        if (BROKER_TYPE_JMS.equalsIgnoreCase(brokerType)) {
            return (Topic) context.lookup(LOOKUP_TOPIC);
        }
        return session.createTopic(getConfiguredStringValue.apply(JNDI_TOPIC_PROP_NAME_VALUE));
    }

    /**
     * Check the cache in the denylist before sync across clusters.
     *
     * @param cacheManager cacheManager name
     * @param cacheName cacheName
     * @return whether the cache is allowed or not.
     */
    public static boolean isAllowedToPropagate(String cacheManager, String cacheName) {

        Map<String, List<String>> cacheDenyList = getMessageBrokerCacheList();
        if (cacheDenyList.size() == 0) {
            return true;
        }
        String cache = getCacheName(cacheName);
        for (Map.Entry<String, List<String>> entry : cacheDenyList.entrySet()) {
            if (StringUtils.equalsIgnoreCase(cacheManager, entry.getKey())) {
                if (entry.getValue().contains(cache)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Map<String, List<String>> getMessageBrokerCacheList() {

        if (mbCacheListConfigurationHolder == null) {
            buildMBCacheListConfig();
        }
        return mbCacheListConfigurationHolder;
    }

    private static ConnectionFactory createRabbitMQConnectionFactory() throws JMSException {

        RMQConnectionFactory factory = new RMQConnectionFactory();
        factory.setUri(getConfiguredStringValue.apply(JNDI_PROVIDER_URL_PROP_VALUE));
        return factory;
    }

    private static String getCacheName(String cacheName) {

        // Cache names are by default prefixed with "$__local__$." or "$__clear__all__$."
        String[] cacheNameParts = cacheName.split("\\.");
        if (cacheNameParts.length >= 2) {
            return cacheNameParts[1];
        }
        return null;
    }

    private static void buildMBCacheListConfig() {

        mbCacheListConfigurationHolder = new HashMap<>();
        OMElement cacheConfig = IdentityConfigParser.getInstance().getConfigElement(CACHE_INVALIDATOR_ELEMENT);
        if (cacheConfig != null) {
            Iterator<OMElement> cacheManagers = cacheConfig.getChildrenWithName(
                    new QName(IdentityCoreConstants.IDENTITY_DEFAULT_NAMESPACE, CACHE_MANAGER_ELEMENT));
            if (cacheManagers != null) {
                while (cacheManagers.hasNext()) {
                    OMElement cacheManager = cacheManagers.next();
                    String cacheManagerName = cacheManager.getAttributeValue(new QName(
                            IdentityConstants.CACHE_MANAGER_NAME));
                    if (StringUtils.isBlank(cacheManagerName)) {
                        log.warn("CacheManager name not defined correctly");
                        continue;
                    }
                    Iterator<OMElement> caches = cacheManager.getChildrenWithName(
                            new QName(IdentityCoreConstants.IDENTITY_DEFAULT_NAMESPACE, IdentityConstants.CACHE));
                    List<String> cacheNames = new ArrayList<>();
                    if (caches != null) {
                        while (caches.hasNext()) {
                            OMElement cache = caches.next();
                            String cacheName = cache.getAttributeValue(new QName(IdentityConstants.CACHE_NAME));
                            if (StringUtils.isBlank(cacheName)) {
                                log.warn("Cache name not defined correctly");
                                continue;
                            }
                            cacheNames.add(cacheName);
                        }
                    }
                    mbCacheListConfigurationHolder.put(cacheManagerName, cacheNames);
                }
            }
        }
    }
}
