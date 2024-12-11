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
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.caching.impl.CacheImpl;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.nio.file.Paths;

import javax.cache.CacheManager;
import javax.cache.CacheManagerFactory;
import javax.cache.Caching;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertSame;

public class JMSConsumerTest {

    @Mock
    private ConnectionFactory connectionFactory;
    @Mock
    private InitialContext initialContext;
    @Mock
    private Session session;
    @Mock
    private Topic topic;
    @Mock
    private Connection connection;
    @Mock
    private MessageConsumer consumer;

    private MockedStatic<JMSUtils> mockedJMSUtils;
    private JMSConsumer jmsConsumer;

    @BeforeMethod
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        mockedJMSUtils = mockStatic(JMSUtils.class);

        when(JMSUtils.createInitialContext()).thenReturn(initialContext);
        when(JMSUtils.getConnectionFactory(initialContext)).thenReturn(connectionFactory);
        when(JMSUtils.createConnection(connectionFactory)).thenReturn(connection);
        when(JMSUtils.getCacheTopic(initialContext, session)).thenReturn(topic);
        initPrivilegedCarbonContext();

        jmsConsumer = spy(new JMSConsumer());

        // Initialize JMSConsumer fields
        doNothing().when(jmsConsumer).startConnection();
        jmsConsumer.session = session;
        jmsConsumer.consumer = consumer;
        jmsConsumer.connection = connection;
    }

    @Test
    public void testGetInstance() {

        JMSConsumer instance1 = JMSConsumer.getInstance();
        JMSConsumer instance2 = JMSConsumer.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    public void testStartServiceWhenCacheInvalidationIsDisabled() throws JMSException, NamingException {

        mockedJMSUtils.when(JMSUtils::isMBCacheInvalidatorEnabled).thenReturn(false);
        jmsConsumer.startService();

        verify(jmsConsumer, never()).startConnection();
        verify(jmsConsumer, never()).invalidateCache(anyString());
    }

    @Test
    public void testInvalidateCache() {

        String message = "ClusterCacheInvalidationRequest{tenantId=1, tenantDomain='example.com', messageId=123, " +
                "cacheManager=myCacheManager, cache=myCache, cacheKey=myKey}";

        try (MockedStatic<PrivilegedCarbonContext> mockedPrivilegedCarbonContext =
                     mockStatic(PrivilegedCarbonContext.class);
             MockedStatic<Caching> mockedCaching = mockStatic(Caching.class)) {

            mockedJMSUtils.when(JMSUtils::getRunInHybridModeProperty).thenReturn(false);

            CacheManager cacheManager = mock(CacheManager.class);
            CacheImpl<Object, Object> cacheImpl = mock(CacheImpl.class);
            CacheManagerFactory cacheManagerFactory = mock(CacheManagerFactory.class);

            mockedCaching.when(Caching::getCacheManagerFactory).thenReturn(cacheManagerFactory);
            when(cacheManagerFactory.getCacheManager("myCacheManager")).thenReturn(cacheManager);
            when(cacheManager.getCache("myCache")).thenReturn(cacheImpl);

            PrivilegedCarbonContext carbonContext = mock(PrivilegedCarbonContext.class);
            mockedPrivilegedCarbonContext.when(PrivilegedCarbonContext::getThreadLocalCarbonContext)
                    .thenReturn(carbonContext);

            jmsConsumer.invalidateCache(message);

            verify(carbonContext, times(1)).setTenantId(1);
            verify(carbonContext, times(1)).setTenantDomain("example.com");
            verify(cacheImpl, times(1)).removeLocal("myKey");
        }
    }

    @AfterMethod
    public void tearDown() {

        jmsConsumer.closeResources();
        // Close static mocks
        if (mockedJMSUtils != null) {
            mockedJMSUtils.close();
        }
    }

    private void initPrivilegedCarbonContext() {

        System.setProperty(
                CarbonBaseConstants.CARBON_HOME,
                Paths.get(System.getProperty("user.dir"), "src", "test", "resources").toString()
        );
        PrivilegedCarbonContext.startTenantFlow();
    }
}
