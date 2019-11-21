package org.rainyheart.distributed.lock.api.annotation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.NestedServletException;

import org.rainyheart.distributed.lock.api.DistributedLockManager;
import org.rainyheart.distributed.lock.api.Lock;

@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:/test-distributed-lock-spring.xml" })
public class DistributedLockTestControllerTest {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    DistributedLockManager distributedLockManager;

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        distributedLockManager = (DistributedLockManager) wac.getBean("mockDistributedLockManager");
        Mockito.reset(distributedLockManager);
    }

    @Test
    public void testTryLockSuccess() throws Exception {
        Mockito.when(distributedLockManager.tryLock(Matchers.any(Lock.class))).thenReturn(true);

        ResultActions result = mockMvc.perform(get("/aop/tryLock"));

        MvcResult mvnResult = result.andExpect(status().isOk()).andReturn();

        String value = mvnResult.getResponse().getContentAsString();
        System.out.println(value);
        Mockito.verify(distributedLockManager, Mockito.times(1)).tryLock(Matchers.any(Lock.class));
        Mockito.verify(distributedLockManager, Mockito.times(1)).unlock(Matchers.any(Lock.class));
    }

    @Test
    public void testLockSuccess() throws Exception {
        Mockito.when(distributedLockManager.lock(Matchers.any(Lock.class), Matchers.anyLong())).thenReturn(true);

        ResultActions result = mockMvc.perform(get("/aop/lock"));

        MvcResult mvnResult = result.andExpect(status().isOk()).andReturn();

        String value = mvnResult.getResponse().getContentAsString();
        System.out.println(value);

        Mockito.verify(distributedLockManager, Mockito.times(1)).lock(Matchers.any(Lock.class), Matchers.anyLong());
        Mockito.verify(distributedLockManager, Mockito.times(1)).unlock(Matchers.any(Lock.class));
    }

    @Test
    public void testtryLockFailedDueToDuplicatedLockAnnotated() throws Exception {
        thrown.expect(NestedServletException.class);
        Mockito.when(distributedLockManager.tryLock(Matchers.any(Lock.class))).thenReturn(true);
        mockMvc.perform(get("/aop/tryLockFailedDueToDuplicatedLockAnnotated"));
        // below codes should never reach
        System.out.println("This line should not be printed");
        Assert.assertTrue(false);
    }
    
    @Test
    public void testDynamicLockSuccess() throws Exception {
        Mockito.when(distributedLockManager.lock(Matchers.any(Lock.class), Matchers.anyLong())).thenReturn(true);
        String lockId = "helloId";
        ResultActions result = mockMvc.perform(get("/aop/dynamicLock").param("lock", lockId));

        MvcResult mvnResult = result.andExpect(status().isOk()).andReturn();

        String value = mvnResult.getResponse().getContentAsString();
        System.out.println(value);
        assertTrue(value.contains(lockId));

        Mockito.verify(distributedLockManager, Mockito.times(1)).lock(Matchers.any(Lock.class), Matchers.anyLong());
        Mockito.verify(distributedLockManager, Mockito.times(1)).unlock(Matchers.any(Lock.class));
    }

    @Test
    public void testDynamicLockPostWithJsonBodySuccess() throws Exception {
        Mockito.when(distributedLockManager.lock(Matchers.any(Lock.class), Matchers.anyLong())).thenReturn(true);
        String lockId = "helloId";
        JsonBody parm = new JsonBody("myLockKey", lockId);
        ResultActions result = mockMvc
                .perform(post("/aop/dynamicLockParm").contentType(MediaType.APPLICATION_JSON).content(parm.toString()));

        MvcResult mvnResult = result.andExpect(status().isOk()).andReturn();

        String value = mvnResult.getResponse().getContentAsString();
        System.out.println(value);
        assertTrue(value.contains(lockId));

        Mockito.verify(distributedLockManager, Mockito.times(1)).lock(Matchers.any(Lock.class), Matchers.anyLong());
        Mockito.verify(distributedLockManager, Mockito.times(1)).unlock(Matchers.any(Lock.class));
    }

    @Test
    public void testLockFail() throws Exception {
        Mockito.when(distributedLockManager.lock(Matchers.any(Lock.class), Matchers.anyLong())).thenReturn(false);

        try {
            mockMvc.perform(get("/aop/lock"));
        } catch (Exception e) {
            Mockito.verify(distributedLockManager, Mockito.times(1)).lock(Matchers.any(Lock.class), Matchers.anyLong());
            Mockito.verify(distributedLockManager, Mockito.times(0)).unlock(Matchers.any(Lock.class));
        }
    }

    @Test
    public void testException() throws Exception {
        Mockito.when(distributedLockManager.lock(Matchers.any(Lock.class), Matchers.anyLong())).thenReturn(true);

        try {
            mockMvc.perform(get("/aop/exception").param("lock", "abc"));
        } catch (Exception e) {
            Mockito.verify(distributedLockManager, Mockito.times(1)).lock(Matchers.any(Lock.class), Matchers.anyLong());
            Mockito.verify(distributedLockManager, Mockito.times(1)).unlock(Matchers.any(Lock.class));
        }
    }

    @Test
    public void testTryLockFail() throws Exception {
        Mockito.when(distributedLockManager.tryLock(Matchers.any(Lock.class))).thenReturn(false);

        try {
            mockMvc.perform(get("/aop/tryLock"));
        } catch (Exception e) {
            Mockito.verify(distributedLockManager, Mockito.times(1)).tryLock(Matchers.any(Lock.class));
            Mockito.verify(distributedLockManager, Mockito.times(0)).unlock(Matchers.any(Lock.class));
        }
    }

    @Test
    public void testDoubleAnnotation() throws Exception {
        Mockito.when(distributedLockManager.lock(Matchers.any(Lock.class), Matchers.anyLong())).thenReturn(true);

        try {
            mockMvc.perform(get("/aop/testDoubleAnnotation").param("lock", "123"));
        } catch (Exception e) {
            assertNotNull(e);
        }
        Mockito.verify(distributedLockManager, Mockito.times(1)).lock(Matchers.any(Lock.class), Matchers.anyLong());
        Mockito.verify(distributedLockManager, Mockito.times(1)).unlock(Matchers.any(Lock.class));
    }
}
