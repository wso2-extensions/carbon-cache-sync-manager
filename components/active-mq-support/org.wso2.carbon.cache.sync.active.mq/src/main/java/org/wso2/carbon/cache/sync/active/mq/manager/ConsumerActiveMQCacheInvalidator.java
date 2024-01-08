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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.caching.impl.CacheImpl;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

import static org.wso2.carbon.cache.sync.active.mq.manager.CacheInvalidatorUtils.getActiveMQBrokerUrl;
import static org.wso2.carbon.cache.sync.active.mq.manager.CacheInvalidatorUtils.getCacheInvalidationTopic;

public class ConsumerActiveMQCacheInvalidator {

    private static Log log = LogFactory.getLog(ConsumerActiveMQCacheInvalidator.class);

    private static volatile ConsumerActiveMQCacheInvalidator instance;

//    private ExecutorService executorService = Executors.newFixedThreadPool(5); // Adjust thread pool size as needed

    private ConsumerActiveMQCacheInvalidator() {

    }

    public static ConsumerActiveMQCacheInvalidator getInstance() {
        if (instance == null) {
            synchronized (ConsumerActiveMQCacheInvalidator.class) {
                if (instance == null) {
                    instance = new ConsumerActiveMQCacheInvalidator();
                }
            }
        }
        return instance;
    }

    public void startService() {

        if (!CacheInvalidatorUtils.isActiveMQCacheInvalidatorEnabled()) {
//            log.debug("ActiveMQ based cache invalidation is not enabled");
            log.info(".........................ActiveMQ broker consumer is not enabled..............");
            return;
        }

        int numberOfRetries = 0;

//        while (numberOfRetries <= 30) {
//            try {
//                // Create a connection factory
//                ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(getActiveMQBrokerUrl());
//
//                // Create a connection
//                Connection connection = connectionFactory.createConnection();
//                connection.start();
//
//                // Create a session
//                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//
//                // Create a topic
//                Topic topic = session.createTopic(getCacheInvalidationTopic());
//
//                // Create a message consumer
//                MessageConsumer consumer = session.createConsumer(topic);
//
//                // Create a message listener for the subscriber
//                consumer.setMessageListener(message -> {
//                    if (message instanceof TextMessage) {
//                        try {
//                            System.out.println("Consumer Received message: " + ((TextMessage) message).getText());
//                            invalidateCache(((TextMessage) message).getText());
//                        } catch (JMSException e) {
//                            log.error("Error in reading the cache invalidation message. " + e);
//                        }
//                    }
////                System.out.println(getActiveMQBrokerUrl());
////                System.out.println(isActiveMQCacheInvalidatorEnabled());
////                System.out.println(getCacheInvalidationTopic());
////                System.out.println(getProducerName());
//                });
//
//            } catch (Exception e) {
//                log.error("Something went wrong with ActiveMQ consumer " + e);
//                try {
//                    numberOfRetries++;
//                    Thread.sleep(2000);
//                } catch (InterruptedException ignored) {
//                    log.debug("Thread sleeping interrupted for ActiveMQ producer.");
//                }
//            }
//        }

//        if (!CacheInvalidatorUtils.isActiveMQCacheInvalidatorEnabled()) {
//            log.info("ActiveMQ broker consumer is not enabled");
//            return;
//        }
//        int maxRetries = 30;
//        int retryInterval= 2000;
//        Runnable consumerTask = () -> {
//            int numberOfRetries = 0;
//            while (numberOfRetries < maxRetries) {
//                Connection connection = null;
//                Session session = null;
//                MessageConsumer consumer = null;
//                try {
//                    ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(getActiveMQBrokerUrl());
//                    connection = connectionFactory.createConnection();
//                    session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//                    Topic topic = session.createTopic(getCacheInvalidationTopic());
//                    consumer = session.createConsumer(topic);
//                    consumer.setMessageListener(message -> {
//                        if (message instanceof TextMessage) {
//                            try {
//                                System.out.println("Consumer Received message: " + ((TextMessage) message).getText());
//                                invalidateCache(((TextMessage) message).getText());
//                            } catch (JMSException e) {
//                                log.error("Error in reading the cache invalidation message. " + e);
//                            }
//                        }
////                        System.out.println(getActiveMQBrokerUrl());
////                        System.out.println(isActiveMQCacheInvalidatorEnabled());
////                        System.out.println(getCacheInvalidationTopic());
////                        System.out.println(getProducerName());
//                    });
//                    connection.start();
//                    return; // Successful setup, exit the loop
//                } catch (Exception e) {
//                    log.error("Something went wrong with ActiveMQ consumer: ", e);
//                    closeResources(connection, session, consumer);
//                    numberOfRetries++;
//                    try {
//                        Thread.sleep(retryInterval);
//                    } catch (InterruptedException ie) {
//                        Thread.currentThread().interrupt();
//                        log.debug("Thread sleeping interrupted for ActiveMQ producer.");
//                        return;
//                    }
//                }
//            }
//            log.error("Exceeded maximum retries for ActiveMQ consumer");
//        };
//
//        executorService.submit(consumerTask);

        // Create a connection factory
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(getActiveMQBrokerUrl());

        try {
            // Create a connection
            Connection connection = connectionFactory.createConnection();
            connection.start();

            // Create a session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create a topic
            Topic topic = session.createTopic(getCacheInvalidationTopic());

            // Create a message consumer
            MessageConsumer consumer = session.createConsumer(topic);

            // Create a message listener for the subscriber
            consumer.setMessageListener(message -> {
                if (message instanceof TextMessage) {
                    try {
                        System.out.println("Consumer Received message: " + ((TextMessage) message).getText());
                        invalidateCache(((TextMessage) message).getText());
                    }  catch (JMSException e) {
                        log.error("Error in reading the cache invalidation message. " + e);
                    }
                }
            });

        } catch (Exception e) {
            log.error("Something went wrong with ActiveMQ consumer " + e);
        }
    }

    public void invalidateCache(String message) {

        String regexPattern = "ClusterCacheInvalidationRequest\\{tenantId=(?<tenantId>-?\\d+), " +
                "tenantDomain='(?<tenantDomain>[\\w.]+)', messageId=(?<messageId>[\\w-]+), " +
                "cacheManager=(?<cacheManager>[\\w.]+), cache=(?<cache>.*?), cacheKey=(?<cacheKey>.*?)\\}";

        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            String tenantId = matcher.group("tenantId");
            String tenantDomain = matcher.group("tenantDomain");
            String messageId = matcher.group("messageId");
            String cacheManager = matcher.group("cacheManager");
            String cache = matcher.group("cache");
            String cacheKey = matcher.group("cacheKey");

            try {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                carbonContext.setTenantId(Integer.valueOf(tenantId));
                carbonContext.setTenantDomain(tenantDomain);

                CacheManager cacheMgr = Caching.getCacheManagerFactory().getCacheManager(cacheManager);
                Cache<Object, Object> cache2 = cacheMgr.getCache(cache);
                if (cache2 instanceof CacheImpl) {
                    if (CacheInvalidatorUtils.CLEAR_ALL_PREFIX.equals(cacheKey)) {
                        ((CacheImpl) cache2).removeAllLocal();
                    } else {
                        ((CacheImpl) cache2).removeLocal(cacheKey);
                    }
                }
                System.out.println("Cache invalidated for tenant " + tenantId + " for manager " + cacheManager +
                        " with cacheKey " + cacheKey);

            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        } else {
            System.out.println("Input does not match the pattern.");
            log.debug("Input doesn't match the expected msg pattern.");
        }
    }

    private void closeResources(Connection connection, Session session, MessageConsumer consumer) {
        try {
            if (consumer != null) consumer.close();
            if (session != null) session.close();
            if (connection != null) connection.close();
        } catch (JMSException e) {
            log.error("Error closing ActiveMQ resources: ", e);
        }
    }

    public void cleanup() {
//        executorService.shutdown();
    }
}
