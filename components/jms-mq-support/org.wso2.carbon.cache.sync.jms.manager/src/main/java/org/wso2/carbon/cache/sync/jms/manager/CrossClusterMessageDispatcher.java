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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.caching.impl.clustering.ClusterCacheInvalidationRequest;

import javax.cache.CacheInvalidationRequestPropagator;

/**
 * This class contains the logic for propagating cache invalidation message from local to multi cluster.
 */
public class CrossClusterMessageDispatcher implements CacheInvalidationRequestPropagator {

    private static final Log log = LogFactory.getLog(CrossClusterMessageDispatcher.class);

    @Override
    public void propagate(ClusterCacheInvalidationRequest clusterCacheInvalidationRequest) {

        if (JMSUtils.getRunInHybridModeProperty()) {
            log.debug("Sending cache invalidation message across multiple clustering.");
            JMSProducer producer = JMSProducer.getInstance();
            producer.sendAsyncInvalidation(clusterCacheInvalidationRequest);
        }
    }
}
