### Pre-request for Configuration

We can use configs to exclude the cache that we don't need to sync. 

1. Add the following config to deployment.toml file.
```yaml
[[cache_invalidator.sync_block_list.cache_manager]]
name="xxxCacheManager"
list=["xxxCache","zzzCache"]
```
For different cache manager we can add multiple blocks.

Eg:
```yaml
[[cache_invalidator.sync_block_list.cache_manager]]
name="IdentityApplicationManagementCacheManager"
list=["AuthenticationResultCache","OAuthSessionDataCache"]

[[cache_invalidator.sync_block_list.cache_manager]]
name="AUTHORIZATION_CACHE_MANAGER"
list=["AUTHORIZATION_CACHE"]
```

2. Add the following identity.xml.j2 template configurations(Improve the existing `CacheInvalidator` tag)
```j2
<CacheInvalidator>
       ...
     {% for cache_manager in cache_invalidator.sync_block_list.cache_manager %}
     <CacheManager name="{{cache_manager.name}}">
          {% for cache_name in cache_manager.list %}
         <Cache name="{{cache_name}}"/>
          {% endfor %}
     </CacheManager>
     {% endfor %}
</CacheInvalidator>
```

### Identified Cache Managers and Caches

1. The following caches in the login flow could be exempted from synchronization across clusters
   - **IdentityApplicationManagementCacheManager**
     - **OAuthSessionDataCache**
       - This contains the data that is related to the protocols such as OAuth2 Parameters, redirect URI, client ID, and a few other similar data.
     - **AuthenticationContextCache**
       - Until the authentication request is successfully authenticated, all authentication information is stored in the AuthenticationContextCache object, which needs to be shared across all nodes in the cluster. Once the user is authenticated successfully, this object will be removed from the cache, and the required information will be stored in the SessionContext cache.
     - **AuthenticationResultCache**
       - Holds the authentication result that contains the authenticated user details, claim mappings, and other authentication specific results, and stores this information in the cache. Once the user gets authenticated through the authentication framework, it stores this object in the cache and reads the response from the inbound protocol handler once the response is built.

   **Note:** This is based on the premise that a single data cluster will oversee the entire authentication flow i.e. while authentication is happening subsequent requests won't jumble between different data clusters for different authentication steps in multi-step authentication.
  
   **Configuration** 
   ```yaml
   [[cache_invalidator.sync_block_list.cache_manager]]
    name="IdentityApplicationManagementCacheManager"
    list=["OAuthSessionDataCache","AuthenticationContextCache","AuthenticationResultCache"]
   ```