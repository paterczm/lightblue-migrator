package com.redhat.lightblue.migrator.facade;

import net.sf.ehcache.CacheManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.redhat.lightblue.migrator.facade.EntityStoreImpl;
import com.redhat.lightblue.migrator.facade.model.Country;

public class EntityStoreImplTest {

    @Test
    public void testSingle() {
        EntityStore store = new EntityStoreImpl(EntityStoreImplTest.class);

        store.push(101l);
        Assert.assertEquals((Long)101l, store.pop());
    }

    @Test
    public void testList() {
        EntityStore store = new EntityStoreImpl(EntityStoreImplTest.class);

        store.push(101l);
        store.push(102l);
        store.push(103l);
        Assert.assertEquals((Long)101l, store.pop());
        Assert.assertEquals((Long)102l, store.pop());
        Assert.assertEquals((Long)103l, store.pop());
    }

    @Test
    public void testDifferentCaches() {
        EntityStore store1 = new EntityStoreImpl(EntityStoreImplTest.class);
        EntityStore store2 = new EntityStoreImpl(Country.class);

        store1.push(101l);
        store1.push(102l);
        store2.push(104l);
        store2.push(105l);
        Assert.assertEquals((Long)101l, store1.pop());
        Assert.assertEquals((Long)104l, store2.pop());
        Assert.assertEquals((Long)102l, store1.pop());
        Assert.assertEquals((Long)105l, store2.pop());
    }

    @Test(expected=RuntimeException.class)
    public void noId() {
        EntityStore store = new EntityStoreImpl(EntityStoreImplTest.class);
        store.pop();
    }

    @Test(expected=RuntimeException.class)
    public void noId2() {
        EntityStore store = new EntityStoreImpl(EntityStoreImplTest.class);
        store.push(1l);
        store.pop();
        store.pop();
    }

    @Test
    public void testCopy() {
        EntityStore store = new EntityStoreImpl(EntityStoreImplTest.class);

        store.push(101l);
        store.push(102l);
        store.push(103l);

        TestThread t = new TestThread(store, Thread.currentThread().getId());

        t.start();
        try {
            t.join();
            Assert.assertTrue(t.isChecksPassed());
        } catch (InterruptedException e) {
            e.printStackTrace();
            Assert.fail();
        }

    }

    @Test
    public void testObject() {
        EntityStore store = new EntityStoreImpl(EntityStoreImplTest.class);

        store.push("foo");
        store.push("bar");
        store.push("foobar");

        Assert.assertEquals("foo", store.pop());
        Assert.assertEquals("bar", store.pop());
        Assert.assertEquals("foobar", store.pop());
    }

    class TestThread extends Thread {

        private EntityStore store;
        private Long parentThreadId;
        private boolean checksPassed = false;

        public TestThread(EntityStore store, Long parentThreadId) {
            super();
            this.store = store;
            this.parentThreadId = parentThreadId;
        }

        @Override
        public void run() {
            store.copyFromThread(parentThreadId);

            checksPassed = 101l == (Long)store.pop() && 102l == (Long)store.pop() && 103l == (Long)store.pop();
        }

        public boolean isChecksPassed() {
            return checksPassed;
        }
    }

    @After
    public void clearAll() {
        CacheManager.create().clearAll();
    }

}
