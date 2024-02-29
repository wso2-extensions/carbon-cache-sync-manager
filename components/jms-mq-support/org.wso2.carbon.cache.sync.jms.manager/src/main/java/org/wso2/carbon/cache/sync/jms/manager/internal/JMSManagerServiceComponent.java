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
package org.wso2.carbon.cache.sync.jms.manager.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.cache.sync.jms.manager.HybridModeCoordinatorListener;
import org.wso2.carbon.cache.sync.jms.manager.JMSConsumer;
import org.wso2.carbon.cache.sync.jms.manager.JMSProducer;
import org.wso2.carbon.cache.sync.jms.manager.JMSUtils;
import org.wso2.carbon.core.clustering.api.CoordinatedActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service component for the JMS cache manager.
 */
@Component(
        name = "org.wso2.carbon.cache.sync.jms.manager.JMSManagerServiceComponent",
        immediate = true
)
public class JMSManagerServiceComponent {

    private static final Log log = LogFactory.getLog(JMSManagerServiceComponent.class);
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Activate
    protected void activate(ComponentContext context) {

        // Continue polling until JMS Manager configurations are fully loaded.
        startCacheInvalidatorOnConfigLoaded(context);
    }

    protected void deactivate(ComponentContext context) {

        // Cleanup resources.
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        // Shutdown JMS producer.
        JMSProducer.getInstance().shutdownExecutorService();
        JMSConsumer.getInstance().closeResources();

        if (log.isDebugEnabled()) {
            log.debug("Cache Sync JMS Manager Service bundle is deactivated.");
        }
    }

    private void startCacheInvalidatorOnConfigLoaded(ComponentContext context) {

        scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (JMSUtils.isMBCacheInvalidatorEnabled() != null) {
                    startClient(context);
                    scheduler.shutdown();
                }
            } catch (Exception e) {
                log.error("Error while checking Cache Sync JMS Manager Configurations", e);
            }
            // Check every 10 seconds, start immediately.
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void startClient(ComponentContext context) {

        // Start the listener for coordinator if hybrid mode is enabled; otherwise, start the JMS service directly.
        if (JMSUtils.getRunInHybridModeProperty()) {
             HybridModeCoordinatorListener coordinatorListener = HybridModeCoordinatorListener.getInstance(context);
             context.getBundleContext().registerService(CoordinatedActivity.class.getName(), coordinatorListener, null);
             log.info("Cache Sync JMS Manager Service Hybrid Mode Listener activated successfully.");
         } else {
             JMSUtils.startOSGIService(context);
         }
    }
}
