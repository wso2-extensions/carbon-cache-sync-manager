### Introduction


In addition to the JMS connector that ensures cache coherence across multiple clusters, performing a periodic cache cleanup mechanism can significantly enhance the reliability and efficiency of our cache management strategy. 


### How to configure


1. Add the following config to deployment.toml file to enable the periodic cleanup.
```yaml
[server.cache]
periodic_cleanup_enabled=true
```
2. To change the default cache timeout/clean-up period add the following config
```yaml
[server]
...
default_cache_timeout="10m"
```
3. To change specific cache timeout/clean-up refer [configure-cache-layers](https://is.docs.wso2.com/en/latest/deploy/performance/configure-cache-layers/#configure-cache-layers_1)