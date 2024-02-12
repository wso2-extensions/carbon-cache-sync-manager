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
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.caching.impl.CachingConstants;
import org.wso2.carbon.caching.impl.clustering.ClusterCacheInvalidationRequest;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.IOException;
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
import javax.naming.InitialContext;
import javax.naming.NamingException;

import static org.wso2.carbon.cache.sync.jms.manager.JMSUtils.PRODUCER_RETRY_LIMIT;
import static org.wso2.carbon.cache.sync.jms.manager.JMSUtils.isAllowedToPropagate;

/**
 * This class contains the logic for sending cache invalidation message.
 */
public class JMSProducer implements CacheEntryRemovedListener, CacheEntryUpdatedListener, CacheEntryCreatedListener,
        CacheInvalidationRequestSender {

    private static final Log log = LogFactory.getLog(JMSProducer.class);
    private static final ExecutorService executorService = Executors.newFixedThreadPool(15);
    private final ConnectionFactory connectionFactory;
    private final InitialContext initialContext;
    private Topic topic;
    private Connection connection;
    private Session session;
    private MessageProducer producer;
    private static volatile JMSProducer instance;

    private JMSProducer() {

        try {
            this.initialContext = JMSUtils.createInitialContext();
            this.connectionFactory = JMSUtils.getConnectionFactory(initialContext);;
        } catch (NamingException | JMSException | IOException e) {
            throw new RuntimeException("Error initializing JMS client resources", e);
        }
    }

    public static JMSProducer getInstance() {

        if (instance == null) {
            synchronized (JMSProducer.class) {
                if (instance == null) {
                    instance = new JMSProducer();
                }
            }
        }
        return instance;
    }

    public void startService() {

        if (!JMSUtils.isMBCacheInvalidatorEnabled()) {
            log.debug("JMS MB based cache invalidation is not enabled.");
            return;
        }

        try {
            startConnection();
        } catch (JMSException | NamingException e) {
            throw new RuntimeException("Error starting JMS connection ", e);
        }
    }

    @SuppressFBWarnings
    @Override
    public void send(CacheEntryInfo cacheEntryInfo) {

        String tenantDomain = cacheEntryInfo.getTenantDomain();
        int tenantId = cacheEntryInfo.getTenantId();

        if (!JMSUtils.isMBCacheInvalidatorEnabled()) {
            log.debug("MB based cache invalidation is not enabled");
            return;
        }

        if (connection == null || session == null) {
            log.debug("JMS Producer connection is not initialized");
            retryConnection();
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

        if (!isAllowedToPropagate(cacheEntryInfo.getCacheManagerName(), cacheEntryInfo.getCacheName())) {
            if (log.isDebugEnabled()) {
                log.debug("Cache " + cacheEntryInfo.getCacheKey() + " is not allowed to propagate to " +
                        "other clusters as per configurations.");
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Sending cache invalidation message to other cluster nodes for '" +
                    cacheEntryInfo.getCacheKey() + "' of the cache '" + cacheEntryInfo.getCacheName()
                    + "' of the cache manager '" + cacheEntryInfo.getCacheManagerName() + "'");
        }

        // Send the cluster message.
        ClusterCacheInvalidationRequest.CacheInfo cacheInfo =
                new ClusterCacheInvalidationRequest.CacheInfo(
                        cacheEntryInfo.getCacheManagerName(),
                        cacheEntryInfo.getCacheName(),
                        cacheEntryInfo.getCacheKey());

        ClusterCacheInvalidationRequest clusterCacheInvalidationRequest = new ClusterCacheInvalidationRequest(
                cacheInfo, tenantDomain, tenantId);

        // Send invalidation message in async manner.
        sendAsyncInvalidation(clusterCacheInvalidationRequest);
    }

    @SuppressFBWarnings
    public void sendAsyncInvalidation(ClusterCacheInvalidationRequest clusterCacheInvalidationRequest) {

        // Send cache invalidation message asynchronously.
        executorService.submit(() -> {
            sendInvalidationMessage(clusterCacheInvalidationRequest);
        });
    }

    @SuppressFBWarnings
    private void sendInvalidationMessage(ClusterCacheInvalidationRequest clusterCacheInvalidationRequest) {

        try {
            TextMessage message = session.createTextMessage(clusterCacheInvalidationRequest.toString());
            if (StringUtils.isNotBlank(JMSUtils.getProducerName())) {
                message.setStringProperty(JMSUtils.SENDER, JMSUtils.getProducerName());
            }
            producer.send(message);
        } catch (JMSException e) {
            log.error("Something went wrong with ActiveMQ producer connection." + e);
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

    public void shutdownExecutorService() {

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            closeResources();
        }
    }

    public void closeResources() {

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
            log.error("Error closing ActiveMQ resources.", e);
        }
    }

    private void startConnection() throws JMSException, NamingException {

        this.connection = JMSUtils.createConnection(connectionFactory);;
        this.connection.start();
        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        this.topic = JMSUtils.getCacheTopic(initialContext, session);
        this.producer = session.createProducer(topic);
    }

    private void retryConnection() {

        int retryCount = 0;
        while ((connection == null || session == null) && retryCount <= PRODUCER_RETRY_LIMIT) {
            try {
                startConnection();
                log.debug("Attempting retry JMS Producer connection.");
            } catch (JMSException | NamingException e) {
                retryCount++;
            }
        }
    }
}
