### Introduction

This will guide you to setup the ActiveMQ Message Broker to communicate over a topic using a publish/subscribe (pub/sub) mechanism.

### Steps to follow:

1. Download ActiveMQ(classic): [ActiveMQ 5.18.3](https://activemq.apache.org/activemq-5018003-release)
2. Extract the downloaded zip file.
3. Start the ActiveMQ broker by executing the following command from the extracted directory.
    - `./bin/activemq console` - to run in interactive mode
    - `./bin/activemq start` - to run in background
4. By default the following ports are exposed:
    - ActiveMQ console is exposed on port 8161: `http://localhost:8161/admin/`
    - Openwire protocol is exposed on port 61616: `tcp://localhost:61616`
      - You can change this by editing the `conf/activemq.xml` file.

### Support

ActiveMQ is free and open source. For commercial support, please refer to the [ActiveMQ community support resources](https://activemq.apache.org/support.html).