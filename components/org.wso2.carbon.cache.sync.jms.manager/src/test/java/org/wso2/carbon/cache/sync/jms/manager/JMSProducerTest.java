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

import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.caching.impl.clustering.ClusterCacheInvalidationRequest;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.cache.CacheEntryInfo;
import javax.cache.CacheInvalidationRequestSender;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

public class JMSProducerTest {

    private static final String LOCAL_CACHE = JMSUtils.LOCAL_CACHE_PREFIX + "myCache";
    private static final String CACHE_MANAGER = "myCacheManager";
    private static final String TENANT_DOMAIN = "example.com";
    private static final int TENANT_ID = 1;

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
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CacheEntryEvent<Object, Object> cacheEntryEvent;
    @Mock
    private CacheInvalidationRequestSender localClusterSender;

    private MockedStatic<JMSUtils> mockedJMSUtils;

    @BeforeMethod
    public void setUp() throws NamingException, JMSException, IOException {

        MockitoAnnotations.initMocks(this);
        mockedJMSUtils = mockStatic(JMSUtils.class);
        jmsProducer = spy(JMSProducer.getInstance());
        jmsProducer.localClusterSender = localClusterSender;
        // The executor is static and shut down in tearDown, so the real async publish must never be reached.
        doNothing().when(jmsProducer).sendAsyncInvalidation(any());

        when(JMSUtils.createInitialContext()).thenReturn(initialContext);
        when(JMSUtils.getConnectionFactory(initialContext)).thenReturn(connectionFactory);
        // The singleton is constructed before these stubs exist, so its connection factory is null.
        when(JMSUtils.createConnection(any())).thenReturn(connection);
        when(JMSUtils.getCacheTopic(any(), any())).thenReturn(topic);
        when(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session);
        when(session.createProducer(topic)).thenReturn(producer);

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
        CacheEntryInfo cacheEntryInfo = createCacheEntryInfo();
        jmsProducer.send(cacheEntryInfo);
        verify(producer, never()).send(any(Message.class));
        verify(jmsProducer, never()).sendAsyncInvalidation(any());
        // The local cluster is still served: the connector being disabled must not break Hazelcast invalidation.
        verify(localClusterSender).send(cacheEntryInfo);
    }

    @Test
    public void testSendForwardsToLocalClusterBeforePublishing() {

        enableBrokerPublishing();
        CacheEntryInfo cacheEntryInfo = createCacheEntryInfo();
        jmsProducer.send(cacheEntryInfo);

        InOrder inOrder = inOrder(localClusterSender, jmsProducer);
        inOrder.verify(localClusterSender).send(cacheEntryInfo);
        ArgumentCaptor<ClusterCacheInvalidationRequest> captor =
                ArgumentCaptor.forClass(ClusterCacheInvalidationRequest.class);
        inOrder.verify(jmsProducer).sendAsyncInvalidation(captor.capture());
        assertEquals(captor.getValue().getCacheInfo().getCacheKey(), "myKey");
        assertEquals(captor.getValue().getCacheInfo().getCacheName(), LOCAL_CACHE);
        assertEquals(captor.getValue().getTenantId(), TENANT_ID);
    }

    @Test
    public void testSendClearAllForwardsAndPublishes() {

        enableBrokerPublishing();
        CacheEntryInfo clearAll = new CacheEntryInfo(CACHE_MANAGER, LOCAL_CACHE, JMSUtils.CLEAR_ALL_PREFIX,
                TENANT_DOMAIN, TENANT_ID);
        jmsProducer.send(clearAll);

        verify(localClusterSender).send(clearAll);
        ArgumentCaptor<ClusterCacheInvalidationRequest> captor =
                ArgumentCaptor.forClass(ClusterCacheInvalidationRequest.class);
        verify(jmsProducer).sendAsyncInvalidation(captor.capture());
        assertEquals(captor.getValue().getCacheInfo().getCacheKey(), JMSUtils.CLEAR_ALL_PREFIX);
    }

    @Test
    public void testSendDenyListedCacheForwardsButDoesNotPublish() {

        enableBrokerPublishing();
        mockedJMSUtils.when(() -> JMSUtils.isAllowedToPropagate(anyString(), anyString())).thenReturn(false);
        CacheEntryInfo cacheEntryInfo = createCacheEntryInfo();
        jmsProducer.send(cacheEntryInfo);

        verify(localClusterSender).send(cacheEntryInfo);
        verify(jmsProducer, never()).sendAsyncInvalidation(any());
    }

    @Test
    public void testSendInvalidTenantStillForwards() {

        enableBrokerPublishing();
        CacheEntryInfo cacheEntryInfo = new CacheEntryInfo(CACHE_MANAGER, LOCAL_CACHE, "myKey", TENANT_DOMAIN, -1);
        jmsProducer.send(cacheEntryInfo);

        verify(localClusterSender).send(cacheEntryInfo);
        verify(jmsProducer, never()).sendAsyncInvalidation(any());
    }

    @Test
    public void testSendNonLocalCacheStillForwards() {

        enableBrokerPublishing();
        CacheEntryInfo cacheEntryInfo = new CacheEntryInfo(CACHE_MANAGER, "myCache", "myKey", TENANT_DOMAIN,
                TENANT_ID);
        jmsProducer.send(cacheEntryInfo);

        verify(localClusterSender).send(cacheEntryInfo);
        verify(jmsProducer, never()).sendAsyncInvalidation(any());
    }

    @Test
    public void testSendPublishesAndRethrowsWhenLocalForwardFails() {

        enableBrokerPublishing();
        RuntimeException failure = new RuntimeException("cluster down");
        doThrow(failure).when(localClusterSender).send(any());
        CacheEntryInfo cacheEntryInfo = createCacheEntryInfo();

        try {
            jmsProducer.send(cacheEntryInfo);
            fail("The local cluster failure must propagate to the caller as it did before.");
        } catch (RuntimeException e) {
            assertSame(e, failure);
        }
        verify(jmsProducer).sendAsyncInvalidation(any());
    }

    @Test
    public void testEntryRemovedPublishesWithoutLocalForward() throws CacheEntryListenerException {

        enableBrokerPublishing();
        stubCacheEntryEvent();
        try (MockedStatic<PrivilegedCarbonContext> mockedContext = mockCarbonContext()) {
            jmsProducer.entryRemoved(cacheEntryEvent);
        }

        // The kernel already notified its own Hazelcast listener on this path.
        verifyNoInteractions(localClusterSender);
        ArgumentCaptor<ClusterCacheInvalidationRequest> captor =
                ArgumentCaptor.forClass(ClusterCacheInvalidationRequest.class);
        verify(jmsProducer).sendAsyncInvalidation(captor.capture());
        assertEquals(captor.getValue().getCacheInfo().getCacheKey(), "myKey");
        assertEquals(captor.getValue().getTenantDomain(), TENANT_DOMAIN);
    }

    @Test
    public void testEntryUpdatedPublishesWithoutLocalForward() throws CacheEntryListenerException {

        enableBrokerPublishing();
        stubCacheEntryEvent();
        try (MockedStatic<PrivilegedCarbonContext> mockedContext = mockCarbonContext()) {
            jmsProducer.entryUpdated(cacheEntryEvent);
        }

        verifyNoInteractions(localClusterSender);
        verify(jmsProducer).sendAsyncInvalidation(any());
    }

    @Test
    public void testEntryRemovedRespectsDenyList() throws CacheEntryListenerException {

        enableBrokerPublishing();
        mockedJMSUtils.when(() -> JMSUtils.isAllowedToPropagate(anyString(), anyString())).thenReturn(false);
        stubCacheEntryEvent();
        try (MockedStatic<PrivilegedCarbonContext> mockedContext = mockCarbonContext()) {
            jmsProducer.entryRemoved(cacheEntryEvent);
        }

        verifyNoInteractions(localClusterSender);
        verify(jmsProducer, never()).sendAsyncInvalidation(any());
    }

    @Test
    public void testEntryCreated() throws CacheEntryListenerException {

        jmsProducer.entryCreated(cacheEntryEvent);
        verifyNoInteractions(producer);
        verifyNoInteractions(localClusterSender);
    }

    private void enableBrokerPublishing() {

        mockedJMSUtils.when(JMSUtils::isMBCacheInvalidatorEnabled).thenReturn(true);
        mockedJMSUtils.when(() -> JMSUtils.isAllowedToPropagate(anyString(), anyString())).thenReturn(true);
    }

    private void stubCacheEntryEvent() {

        when(cacheEntryEvent.getSource().getCacheManager().getName()).thenReturn(CACHE_MANAGER);
        when(cacheEntryEvent.getSource().getName()).thenReturn(LOCAL_CACHE);
        when(cacheEntryEvent.getKey()).thenReturn("myKey");
    }

    private MockedStatic<PrivilegedCarbonContext> mockCarbonContext() {

        PrivilegedCarbonContext carbonContext = mock(PrivilegedCarbonContext.class);
        when(carbonContext.getTenantDomain(true)).thenReturn(TENANT_DOMAIN);
        when(carbonContext.getTenantId(true)).thenReturn(TENANT_ID);
        MockedStatic<PrivilegedCarbonContext> mockedContext = mockStatic(PrivilegedCarbonContext.class);
        mockedContext.when(PrivilegedCarbonContext::getThreadLocalCarbonContext).thenReturn(carbonContext);
        return mockedContext;
    }

    private CacheEntryInfo createCacheEntryInfo() {

        return new CacheEntryInfo(CACHE_MANAGER, LOCAL_CACHE, "myKey", TENANT_DOMAIN, TENANT_ID);
    }
}
