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

/**
 * Util class for the ActiveMQ cache manager Service.
 */
public class CacheSyncUtils {

    public static final String BROKER_URL_PROPERTY = "CacheInvalidator.ActiveMQ.BrokerURL";
    public static final String ACTIVEMQ_INVALIDATOR_ENABLED_PROPERTY = "CacheInvalidator.ActiveMQ.Enabled";
    public static final String ACTIVEMQ_CACHE_TOPIC_PROPERTY = "CacheInvalidator.ActiveMQ.TopicName";
    public static final String ACTIVEMQ_PRODUCER_NAME_PROPERTY = "CacheInvalidator.ActiveMQ.ProducerName";

    // Cache name prefix of local cache.
    public static final String LOCAL_CACHE_PREFIX = "$__local__$.";

    // Cache name prefix of clear all.
    public static final String CLEAR_ALL_PREFIX = "$__clear__all__$.";

    // Default producer retry limit.
    public static final int PRODUCER_RETRY_LIMIT = 30;


    private CacheSyncUtils() {

    }

    public static String getActiveMQBrokerUrl() {

        if (StringUtils.isNotBlank(IdentityUtil.getProperty(BROKER_URL_PROPERTY))) {
            return IdentityUtil.getProperty(BROKER_URL_PROPERTY).trim();
        }
        return null;
    }

    public static String getProducerName() {

        if (StringUtils.isNotBlank(IdentityUtil.getProperty(ACTIVEMQ_PRODUCER_NAME_PROPERTY))) {
            return IdentityUtil.getProperty(ACTIVEMQ_PRODUCER_NAME_PROPERTY).trim();
        }
        return null;
    }

    public static String getCacheInvalidationTopic() {

        if (StringUtils.isNotBlank(IdentityUtil.getProperty(ACTIVEMQ_CACHE_TOPIC_PROPERTY))) {
            return IdentityUtil.getProperty(ACTIVEMQ_CACHE_TOPIC_PROPERTY).trim();
        }
        return null;
    }

    public static Boolean isActiveMQCacheInvalidatorEnabled() {

        if (StringUtils.isNotBlank(IdentityUtil.getProperty(ACTIVEMQ_INVALIDATOR_ENABLED_PROPERTY))) {
            return Boolean.parseBoolean(IdentityUtil.getProperty(ACTIVEMQ_INVALIDATOR_ENABLED_PROPERTY));
        }
        return null;
    }
}
