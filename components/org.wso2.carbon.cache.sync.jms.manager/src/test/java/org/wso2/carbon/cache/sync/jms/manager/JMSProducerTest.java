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
package org.wso2.carbon.cache.sync.jms.manager;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.cache.CacheEntryInfo;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class JMSProducerTest {

    private JMSProducer jmsProducer;
    private ExecutorService executorService;

    @Mock
    private InitialContext initialContext;
    @Mock
    private ConnectionFactory connectionFactory;
    @Mock
    private Connection connection;
    @Mock
    private Session session;
    @Mock
    private Topic topic;
    @Mock
    private MessageProducer producer;
    @Mock
    private CacheEntryEvent<Object, Object> cacheEntryEvent;

    private MockedStatic<JMSUtils> mockedJMSUtils;

    @BeforeMethod
    public void setUp() throws NamingException, JMSException, IOException {

        MockitoAnnotations.initMocks(this);
        mockedJMSUtils = mockStatic(JMSUtils.class);
        jmsProducer = spy(JMSProducer.getInstance());

        when(JMSUtils.createInitialContext()).thenReturn(initialContext);
        when(JMSUtils.getConnectionFactory(initialContext)).thenReturn(connectionFactory);
        when(JMSUtils.createConnection(connectionFactory)).thenReturn(connection);
        when(JMSUtils.getCacheTopic(initialContext, session)).thenReturn(topic);

        executorService = Executors.newFixedThreadPool(15);
    }

    @AfterMethod
    public void tearDown() {

        executorService.shutdown();
        jmsProducer.shutdownExecutorService();
        if (mockedJMSUtils != null) {
            mockedJMSUtils.close();
        }
    }

    @Test
    public void testStartServiceWhenCacheInvalidationIsDisabled() throws JMSException, NamingException {

        mockedJMSUtils.when(JMSUtils::isMBCacheInvalidatorEnabled).thenReturn(false);
        jmsProducer.startService();
        verify(jmsProducer, never()).startConnection();
    }

    @Test
    public void testSendWhenCacheInvalidationIsDisabled() throws JMSException {

        mockedJMSUtils.when(JMSUtils::isMBCacheInvalidatorEnabled).thenReturn(false);
        jmsProducer.send(createCacheEntryInfo());
        verify(producer, never()).send(any(Message.class));
    }

    @Test
    public void testEntryCreated() throws CacheEntryListenerException {

        jmsProducer.entryCreated(cacheEntryEvent);
        verifyNoInteractions(producer);
    }

    private CacheEntryInfo createCacheEntryInfo() {

        return new CacheEntryInfo("myCacheManager", "myCache",
                "myKey", "example.com", 1);
    }
}
