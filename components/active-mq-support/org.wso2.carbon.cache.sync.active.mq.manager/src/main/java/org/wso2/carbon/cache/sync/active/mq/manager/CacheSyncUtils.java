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

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * Util class for the ActiveMQ cache manager Service.
 */
public class CacheSyncUtils {

    public static final String BROKER_URL_PROPERTY = "CacheInvalidator.ActiveMQ.BrokerURL";
    public static final String ACTIVEMQ_INVALIDATOR_ENABLED_PROPERTY = "CacheInvalidator.ActiveMQ.Enabled";
    public static final String ACTIVEMQ_CACHE_TOPIC_PROPERTY = "CacheInvalidator.ActiveMQ.TopicName";
    public static final String ACTIVEMQ_PRODUCER_NAME_PROPERTY = "CacheInvalidator.ActiveMQ.ProducerName";
    public static final String RUN_IN_HYBRID_MODE_PROPERTY = "CacheInvalidator.ActiveMQ.HybridMode";

    // Cache name prefix of local cache.
    public static final String LOCAL_CACHE_PREFIX = "$__local__$.";

    // Cache name prefix of clear all.
    public static final String CLEAR_ALL_PREFIX = "$__clear__all__$.";

    // Default producer retry limit.
    public static final int PRODUCER_RETRY_LIMIT = 30;

    public static final String SENDER = "sender";

    private CacheSyncUtils() {

    }

    /**
     * Configured activemq broker url.
     *
     * @return String ActiveMQ broker url.
     */
    public static String getActiveMQBrokerUrl() {

        return getConfiguredStringValue.apply(BROKER_URL_PROPERTY);
    }

    /**
     * Configured producer name for the node.
     *
     * @return String Producer name.
     */
    public static String getProducerName() {

        return getConfiguredStringValue.apply(ACTIVEMQ_PRODUCER_NAME_PROPERTY);
    }

    /**
     * Configured cache topic name in the activemq broker.
     *
     * @return String Cache topic name.
     */
    public static String getCacheInvalidationTopic() {

        return getConfiguredStringValue.apply(ACTIVEMQ_CACHE_TOPIC_PROPERTY);
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
     * Checks if the ActiveMQ Cache Invalidator is enabled.
     *
     * @return Boolean representing the enabled state, or null if the property is not set.
     */
    public static Boolean isActiveMQCacheInvalidatorEnabled() {

        return getConfiguredBooleanValue.apply(ACTIVEMQ_INVALIDATOR_ENABLED_PROPERTY, null);
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
}
