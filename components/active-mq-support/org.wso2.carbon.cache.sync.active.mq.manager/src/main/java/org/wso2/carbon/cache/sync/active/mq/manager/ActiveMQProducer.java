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
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.caching.impl.CachingConstants;
import org.wso2.carbon.caching.impl.clustering.ClusterCacheInvalidationRequest;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.cache.CacheEntryInfo;
import javax.cache.CacheInvalidationRequestSender;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

/**
 * This class contains the logic for sending cache invalidation message.
 */
public class ActiveMQProducer implements CacheEntryRemovedListener, CacheEntryUpdatedListener,
        CacheEntryCreatedListener, CacheInvalidationRequestSender {

    private static final Log log = LogFactory.getLog(ActiveMQProducer.class);
    private static final String SENDER = "sender";
    private static final ExecutorService executorService = Executors.newFixedThreadPool(15);


    @SuppressFBWarnings
    @Override
    public void send(CacheEntryInfo cacheEntryInfo) {

        String tenantDomain = cacheEntryInfo.getTenantDomain();
        int tenantId = cacheEntryInfo.getTenantId();

        if (!CacheSyncUtils.isActiveMQCacheInvalidatorEnabled()) {
            log.debug("ActiveMQ based cache invalidation is not enabled");
            return;
        }

        if (MultitenantConstants.INVALID_TENANT_ID == tenantId) {
            if (log.isDebugEnabled()) {
                String stackTrace = ExceptionUtils.getStackTrace(new Throwable());
                log.debug("Tenant information cannot be found in the request. This originated from: \n" + stackTrace);
            }
            return;
        }

        if (!cacheEntryInfo.getCacheName().startsWith(CachingConstants.LOCAL_CACHE_PREFIX)) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Sending cache invalidation message to other cluster nodes for '" + cacheEntryInfo.getCacheKey() +
                    "' of the cache '" + cacheEntryInfo.getCacheName() + "' of the cache manager '" +
                    cacheEntryInfo.getCacheManagerName() + "'");
        }

        // Send the cluster message.
        ClusterCacheInvalidationRequest.CacheInfo cacheInfo =
                new ClusterCacheInvalidationRequest.CacheInfo(cacheEntryInfo.getCacheManagerName(),
                        cacheEntryInfo.getCacheName(),
                        cacheEntryInfo.getCacheKey());

        ClusterCacheInvalidationRequest clusterCacheInvalidationRequest = new ClusterCacheInvalidationRequest(
                cacheInfo, tenantDomain, tenantId);

        // Send cache invalidation message asynchronously.
        executorService.submit(() -> {
            sendInvalidationMessage(CacheSyncUtils.PRODUCER_RETRY_LIMIT, clusterCacheInvalidationRequest);
        });
    }

    @SuppressFBWarnings
    private void sendInvalidationMessage(int retryLimit,
                                  ClusterCacheInvalidationRequest clusterCacheInvalidationRequest) {

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(CacheSyncUtils.getActiveMQBrokerUrl());
        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;

        try {
            connection = connectionFactory.createConnection();
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(CacheSyncUtils.getCacheInvalidationTopic());
            producer = session.createProducer(topic);

            TextMessage message = session.createTextMessage(clusterCacheInvalidationRequest.toString());
            message.setStringProperty(SENDER, CacheSyncUtils.getProducerName());
            producer.send(message);
        } catch (JMSException e) {
            log.error("Something went wrong with ActiveMQ producer connection." + e);
        } finally {
            try {
                if (producer != null) {
                    producer.close();
                }
                if (session != null) {
                    session.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (JMSException e) {
                log.error("Error closing ActiveMQ resources", e);
            }
        }
    }

    @Override
    public void entryCreated(CacheEntryEvent cacheEntryEvent) throws CacheEntryListenerException {
      // No need to send invalidation message for new cache entries.
    }

    @Override
    public void entryRemoved(CacheEntryEvent cacheEntryEvent) throws CacheEntryListenerException {

        send(createCacheInfo(cacheEntryEvent));
    }

    @Override
    public void entryUpdated(CacheEntryEvent cacheEntryEvent) throws CacheEntryListenerException {

        send(createCacheInfo(cacheEntryEvent));
    }

    public static CacheEntryInfo createCacheInfo(CacheEntryEvent cacheEntryEvent) {

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain(true);
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        return new CacheEntryInfo(cacheEntryEvent.getSource().getCacheManager().getName(),
                cacheEntryEvent.getSource().getName(), cacheEntryEvent.getKey(), tenantDomain, tenantId);
    }

    // Shutdown the thread executor.
    public static void shutdownExecutorService() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
