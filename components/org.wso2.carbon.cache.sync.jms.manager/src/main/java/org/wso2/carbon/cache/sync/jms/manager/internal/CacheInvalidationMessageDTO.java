/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

/**
 * Data Transfer Object (DTO) representing a cache invalidation message
 * to be sent across clusters via JMS.
 * <p>
 * This class is used to serialize essential information from a
 * {@link org.wso2.carbon.caching.impl.clustering.ClusterCacheInvalidationRequest}
 * into a stable format, ensuring that all necessary fields are propagated
 * even when subclass fields are not directly exposed.
 * </p>
 */
public class CacheInvalidationMessageDTO {

    private String uuid;
    private long timestamp;
    private String tenantDomain;
    private int tenantId;

    private String cacheManagerName;
    private String cacheName;
    private String cacheKeyBase64;

    public CacheInvalidationMessageDTO() {
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public String getCacheManagerName() {
        return cacheManagerName;
    }

    public void setCacheManagerName(String cacheManagerName) {
        this.cacheManagerName = cacheManagerName;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public String getCacheKeyBase64() {
        return this.cacheKeyBase64;
    }

    public void setCacheKeyBase64(String cacheKeyBase64) {
        this.cacheKeyBase64 = cacheKeyBase64;
    }
}
