### How to Build
1. Follow the instructions in the [IS_README.md](IS_README.md)

2. Then add the following config(update it with your values) in the `deployment.toml` file
```yaml
[cache_invalidator.mb]
enabled="true"
broker_type="rabbitmq"
provider_url="amqp://localhost:5672"
topic_name="CacheTopic"
producer_name="producer1"
hybrid_mode_enabled="true"
username="guest"
password="guest"
```

3. Restart the server.