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

package org.wso2.carbon.cache.sync.active.mq.manager.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cache.sync.active.mq.manager.ConsumerActiveMQCacheInvalidator;
import org.wso2.carbon.cache.sync.active.mq.manager.ProducerActiveMQCacheInvalidator;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.cache.sync.active.mq.manager.CacheInvalidatorUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.cache.CacheInvalidationRequestSender;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;

@Component(
        name = "org.wso2.carbon.cache.sync.active.mq.manager.CacheSyncActiveMQManagerServiceComponent",
        immediate = true
)
public class CacheSyncActiveMQManagerServiceComponent {

    private static Log log = LogFactory.getLog(CacheSyncActiveMQManagerServiceComponent.class);
    private ServiceRegistration serviceRegistration1 = null;
    private ServiceRegistration serviceRegistration2 = null;
    private ServiceRegistration serviceRegistration3 = null;
    private ServiceRegistration serviceRegistration4 = null;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


    @Activate
    protected void activate(ComponentContext context) {

        //register the custom listener as an OSGI service.

        ProducerActiveMQCacheInvalidator producer = new ProducerActiveMQCacheInvalidator();

        serviceRegistration1 = context.getBundleContext().registerService(CacheEntryListener.class.getName(),
                producer,null);
        serviceRegistration2 = context.getBundleContext().registerService(CacheInvalidationRequestSender.class.getName(),
                producer,null);
        serviceRegistration3 = context.getBundleContext().registerService(CacheEntryRemovedListener.class.getName(),
                producer,null);
        serviceRegistration4 = context.getBundleContext().registerService(CacheEntryUpdatedListener.class.getName(),
                producer,null);

        // Start polling for ActiveMQCacheInvalidatorEnabled
        startPollingForActiveMQCacheInvalidator();
    }

    private void startPollingForActiveMQCacheInvalidator() {

        scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (CacheInvalidatorUtils.isActiveMQCacheInvalidatorEnabled() != null) {
                    ConsumerActiveMQCacheInvalidator.getInstance().startService();
                    log.info("ActiveMQ Cache Invalidator Service bundle activated successfully.");
                    // Stop polling once activated.
                    scheduler.shutdown();
                }
            } catch (Exception e) {
                log.error("Error while checking ActiveMQ Cache Invalidator status", e);
            }
            // Check every 10 seconds, start immediately.
        }, 0, 10, TimeUnit.SECONDS);
    }

    protected void deactivate(ComponentContext context) {

        // Cleanup resources.
        ConsumerActiveMQCacheInvalidator.getInstance().cleanup();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        // Unregistering the listener service.
        if (serviceRegistration1 != null) {
            serviceRegistration1.unregister();
        }
        if (serviceRegistration2 != null) {
            serviceRegistration2.unregister();
        }
        if (serviceRegistration3 != null) {
            serviceRegistration3.unregister();
        }
        if (serviceRegistration4 != null) {
            serviceRegistration4.unregister();
        }
        if (log.isDebugEnabled()) {
            log.debug("ActiveMQ Cache Invalidator Service bundle is deactivated.");
        }
    }

}
