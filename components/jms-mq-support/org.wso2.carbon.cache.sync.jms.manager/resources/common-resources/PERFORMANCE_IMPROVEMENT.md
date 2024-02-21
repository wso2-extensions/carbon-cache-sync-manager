### Configure

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
list=["AuthenticationResultCache","AuthenticationRequestCache"]

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