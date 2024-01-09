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

package org.wso2.carbon.cache.sync.active.mq.manager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.caching.impl.CacheImpl;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import static org.wso2.carbon.cache.sync.active.mq.manager.CacheSyncUtils.getActiveMQBrokerUrl;
import static org.wso2.carbon.cache.sync.active.mq.manager.CacheSyncUtils.getCacheInvalidationTopic;

/**
 * This class contains the logic for receiving cache invalidation message.
 */
public class ActiveMQConsumer {

    private static final Log log = LogFactory.getLog(ActiveMQConsumer.class);

    private static volatile ActiveMQConsumer instance;

    private ActiveMQConsumer() {

    }

    public static ActiveMQConsumer getInstance() {
        if (instance == null) {
            synchronized (ActiveMQConsumer.class) {
                if (instance == null) {
                    instance = new ActiveMQConsumer();
                }
            }
        }
        return instance;
    }

    public void startService() {

        if (!CacheSyncUtils.isActiveMQCacheInvalidatorEnabled()) {
            log.debug("ActiveMQ based cache invalidation is not enabled");
            // Todo remove this info log.
            log.info(".........................ActiveMQ broker consumer is not enabled..............");
            return;
        }

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(getActiveMQBrokerUrl());

        try {
            Connection connection = connectionFactory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Topic topic = session.createTopic(getCacheInvalidationTopic());

            MessageConsumer consumer = session.createConsumer(topic);

            // Message listener for the subscriber.
            consumer.setMessageListener(message -> {
                if (message instanceof TextMessage) {
                    try {
                        // Todo remove this info log.
                        // log.info("Consumer Received message: " + ((TextMessage) message).getText());
                        invalidateCache(((TextMessage) message).getText());
                    }  catch (JMSException e) {
                        String sanitizedErrorMessage = e.getMessage().replace("\n", "")
                                .replace("\r", "");
                        log.error("Error in reading the cache invalidation message. " + sanitizedErrorMessage);
                    }
                }
            });

        } catch (Exception e) {
            String sanitizedErrorMessage = e.getMessage().replace("\n", "")
                    .replace("\r", "");
            log.error("Something went wrong with ActiveMQ consumer " + sanitizedErrorMessage);
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

            try {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                carbonContext.setTenantId(Integer.valueOf(tenantId));
                carbonContext.setTenantDomain(tenantDomain);

                CacheManager cacheMgr = Caching.getCacheManagerFactory().getCacheManager(cacheManager);
                Cache<Object, Object> cacheObject = cacheMgr.getCache(cache);
                if (cacheObject instanceof CacheImpl) {
                    if (CacheSyncUtils.CLEAR_ALL_PREFIX.equals(cacheKey)) {
                        ((CacheImpl) cacheObject).removeAllLocal();
                    } else {
                        ((CacheImpl) cacheObject).removeLocal(cacheKey);
                    }
                }
                // Todo remove this info log.
                log.info("Cache invalidated for tenant " + tenantId + " for manager " + cacheManager +
                        " with cacheKey " + cacheKey);

            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        } else {
            // Todo remove this info log.
            log.info("Input does not match the pattern.");
            log.debug("Input doesn't match the expected msg pattern.");
        }
    }
}
