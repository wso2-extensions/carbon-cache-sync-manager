### Configure WSO2 Identity Server for RabbitMQ

1. Follow the instructions in the [IS_README.md](IS_README.md).

2. Add the following configuration (update it with your values) to the `deployment.toml` file.
```toml
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
