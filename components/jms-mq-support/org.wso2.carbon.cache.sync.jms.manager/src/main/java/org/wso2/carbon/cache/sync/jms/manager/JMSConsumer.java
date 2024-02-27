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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.caching.impl.CacheImpl;
import org.wso2.carbon.caching.impl.clustering.ClusterCacheInvalidationRequestSender;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.cache.Cache;
import javax.cache.CacheEntryInfo;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import static org.wso2.carbon.cache.sync.jms.manager.JMSUtils.PRODUCER_RETRY_LIMIT;
import static org.wso2.carbon.cache.sync.jms.manager.JMSUtils.getProducerName;

/**
 * This class contains the logic for receiving cache invalidation message.
 */
public class JMSConsumer {

    private static final Log log = LogFactory.getLog(JMSConsumer.class);

    private final ConnectionFactory connectionFactory;
    private final InitialContext initialContext;
    private Session session;
    private Topic topic;
    private Connection connection;
    private MessageConsumer consumer;
    private static volatile JMSConsumer instance;

    private JMSConsumer() {

        try {
            this.initialContext = JMSUtils.createInitialContext();
            this.connectionFactory = JMSUtils.getConnectionFactory(initialContext);
        } catch (NamingException | JMSException | IOException e) {
            throw new RuntimeException("Error initializing JMS client resources", e);
        }
    }

    public static JMSConsumer getInstance() {

        if (instance == null) {
            synchronized (JMSConsumer.class) {
                if (instance == null) {
                    instance = new JMSConsumer();
                }
            }
        }
        return instance;
    }

    @SuppressFBWarnings
    public void startService() {

        if (!JMSUtils.isMBCacheInvalidatorEnabled()) {
            log.debug("JMS MB based cache invalidation is not enabled.");
            return;
        }
        int retryCount = 0;
        while (session == null && retryCount <= PRODUCER_RETRY_LIMIT) {
            try {
                // establish the connection over specified topic.
                startConnection();
                // Message listener for the subscriber.
                consumer.setMessageListener(message -> {
                    if (!(message instanceof TextMessage)) {
                        // Ignore non-TextMessage.
                        return;
                    }
                    try {
                        String sender = message.getStringProperty(JMSUtils.SENDER);
                        // Skip processing if the sender is the same as the producer.
                        if (JMSUtils.getProducerName() != null && StringUtils.equals(
                                JMSUtils.getProducerName(), sender)) {
                            return;
                        }
                        invalidateCache(((TextMessage) message).getText());
                    } catch (JMSException e) {
                        log.error("Error in reading the cache invalidation message.", e);
                    }
                });
            } catch (JMSException | NamingException e) {
                log.error("Error while listening to JMS message broker. ", e);
                retryCount++;
            }
        }
    }

    @SuppressFBWarnings
    public void invalidateCache(String message) {

        String regexPattern = "ClusterCacheInvalidationRequest\\{tenantId=(?<tenantId>-?\\d+), " +
                "tenantDomain='(?<tenantDomain>[\\w.]+)', messageId=(?<messageId>[\\w-]+), " +
                "cacheManager=(?<cacheManager>[\\w.]+), cache=(?<cache>.*?), cacheKey=(?<cacheKey>.*?)\\}";

        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            String tenantId = matcher.group("tenantId");
            String tenantDomain = matcher.group("tenantDomain");
            String cacheManager = matcher.group("cacheManager");
            String cache = matcher.group("cache");
            String cacheKey = matcher.group("cacheKey");

            if (log.isDebugEnabled()) {
                log.debug("Received cache invalidation message from other cluster nodes for '" + cacheKey +
                        "' of the cache '" + cache + "' of the cache manager '" + cacheManager + "'.");
            }

            try {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                carbonContext.setTenantId(Integer.valueOf(tenantId));
                carbonContext.setTenantDomain(tenantDomain);

                CacheManager cacheMgr = Caching.getCacheManagerFactory().getCacheManager(cacheManager);
                Cache<Object, Object> cacheObject = cacheMgr.getCache(cache);
                if (cacheObject instanceof CacheImpl) {
                    if (JMSUtils.CLEAR_ALL_PREFIX.equals(cacheKey)) {
                        ((CacheImpl) cacheObject).removeAllLocal();
                    } else {
                        ((CacheImpl) cacheObject).removeLocal(cacheKey);
                    }
                }
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }

            // If hybrid mode is enabled pass invalidation msg to local cluster.
            if (JMSUtils.getRunInHybridModeProperty()) {

                CacheEntryInfo cacheEntryInfo = new CacheEntryInfo(cacheManager,
                        cache, cacheKey, tenantDomain, Integer.valueOf(tenantId));
                log.debug("Sending cache invalidation message for local clustering: " + cacheEntryInfo);
                ClusterCacheInvalidationRequestSender cacheInvalidationRequestSender =
                        new ClusterCacheInvalidationRequestSender();
                cacheInvalidationRequestSender.send(cacheEntryInfo);
            }
        } else {
            log.debug("Input doesn't match the expected msg pattern.");
        }
    }

    public void closeResources() {

        try {
            if (consumer != null) {
                consumer.close();
            }
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (JMSException e) {
            log.error("Error closing JMS resources", e);
        }
    }

    private void startConnection() throws JMSException, NamingException {

        this.connection = JMSUtils.createConnection(connectionFactory);
        boolean isDurableSubscription = JMSUtils.isDurableSubscriber();
        if (isDurableSubscription) {
            connection.setClientID(JMSUtils.DURABLE_CON_CLIENT_ID_PREFIX + getProducerName());
        }
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        this.topic = JMSUtils.getCacheTopic(initialContext, session);
        if (isDurableSubscription) {
            consumer = session.createDurableSubscriber(topic, JMSUtils.DURABLE_SUB_NAME_PREFIX + getProducerName());
        } else {
            consumer = session.createConsumer(topic);
        }
    }
}
