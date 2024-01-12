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
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.cache.sync.active.mq.manager.ActiveMQConsumer;
import org.wso2.carbon.cache.sync.active.mq.manager.ActiveMQProducer;
import org.wso2.carbon.cache.sync.active.mq.manager.CacheSyncUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.cache.CacheInvalidationRequestSender;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;

/**
 * Service component for the ActiveMQ cache manager.
 */
@Component(
        name = "org.wso2.carbon.cache.sync.active.mq.manager.CacheSyncActiveMQManagerServiceComponent",
        immediate = true
)
public class CacheSyncActiveMQManagerServiceComponent {

    private static final Log log = LogFactory.getLog(CacheSyncActiveMQManagerServiceComponent.class);
    private ServiceRegistration serviceRegistrationForCacheEntry = null;
    private ServiceRegistration serviceRegistrationForRequestSend = null;
    private ServiceRegistration serviceRegistrationForCacheRemoval = null;
    private ServiceRegistration serviceRegistrationForCacheUpdate = null;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Activate
    protected void activate(ComponentContext context) {

        ActiveMQProducer producer = new ActiveMQProducer();
        serviceRegistrationForCacheEntry = context.getBundleContext().registerService(
                CacheEntryListener.class.getName(), producer, null);
        serviceRegistrationForRequestSend = context.getBundleContext().registerService(
                CacheInvalidationRequestSender.class.getName(), producer, null);
        serviceRegistrationForCacheRemoval = context.getBundleContext().registerService(
                CacheEntryRemovedListener.class.getName(), producer, null);
        serviceRegistrationForCacheUpdate = context.getBundleContext().registerService(
                CacheEntryUpdatedListener.class.getName(), producer, null);

        // Continue polling until ActiveMQ Manager configurations are fully updated.
        startPollingForActiveMQCacheInvalidator();
    }

    private void startPollingForActiveMQCacheInvalidator() {

        scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (CacheSyncUtils.isActiveMQCacheInvalidatorEnabled() != null) {
                    ActiveMQConsumer.getInstance().startService();
                    log.info("Cache Sync ActiveMQ Manager Service bundle activated successfully.");
                    scheduler.shutdown();
                }
            } catch (Exception e) {
                log.error("Error while checking Cache Sync ActiveMQ Manager status", e);
            }
            // Check every 10 seconds, start immediately.
        }, 0, 10, TimeUnit.SECONDS);
    }

    protected void deactivate(ComponentContext context) {

        // Cleanup resources.
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        ActiveMQProducer.shutdownExecutorService();

        // Unregistering the listener service.
        if (serviceRegistrationForCacheEntry != null) {
            serviceRegistrationForCacheEntry.unregister();
        }
        if (serviceRegistrationForRequestSend != null) {
            serviceRegistrationForRequestSend.unregister();
        }
        if (serviceRegistrationForCacheRemoval != null) {
            serviceRegistrationForCacheRemoval.unregister();
        }
        if (serviceRegistrationForCacheUpdate != null) {
            serviceRegistrationForCacheUpdate.unregister();
        }
        if (log.isDebugEnabled()) {
            log.debug("Cache Sync ActiveMQ Manager Service bundle is deactivated.");
        }
    }
}
