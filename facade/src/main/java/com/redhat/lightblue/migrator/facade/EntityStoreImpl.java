package com.redhat.lightblue.migrator.facade;

import java.net.URL;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EntityStore implementation using ehcache. Creates a cache object per dao and uses thread id as key to avoid conflicts.
 * There is an assumption that both legacy and destination daos create entities in the same order.
 *
 * @author mpatercz
 *
 */
public class EntityStoreImpl implements EntityStore {

    private static final Logger log = LoggerFactory.getLogger(EntityStoreImpl.class);

    // singleton
    private CacheManager cacheManager;
    private Cache cache;

    public EntityStoreImpl(Class<?> daoClass) {
        this(daoClass, null);
    }

    public EntityStoreImpl(Class<?> daoClass, URL ehcacheConfigFile) {
        log.debug("Initializing id cache for "+daoClass.getCanonicalName());

        if (ehcacheConfigFile == null)
            cacheManager = CacheManager.create(EntityStoreImpl.class.getResourceAsStream("/ehcache.xml"));
        else
            cacheManager = CacheManager.create(ehcacheConfigFile);

        cacheManager.addCacheIfAbsent(daoClass.getCanonicalName());
        cache = cacheManager.getCache(daoClass.getCanonicalName());
    }

    @Override
    public void push(Object obj) {
        long threadId = Thread.currentThread().getId();
        if(log.isDebugEnabled())
            log.debug("Storing obj="+obj+" for "+cache.getName()+", thread="+threadId);

        Element el = cache.get(threadId);
        LinkedList<Object> list;

        if (el == null) {
            list = new LinkedList<Object>();
        }
        else {
            list = (LinkedList<Object>)el.getObjectValue();
        }

        list.add(obj);

        cache.put(new Element(threadId, list));
    }

    @Override
    public Object pop() {
        long threadId = Thread.currentThread().getId();
        log.debug("Restoring id for "+cache.getName()+" thread="+threadId);

        Element el = cache.get(threadId);

        if (el == null) {
            throw new EntityStoreException(cache.getName(), threadId);
        }

        @SuppressWarnings("unchecked")
        LinkedList<Long> list = (LinkedList<Long>)el.getObjectValue();

        try {
            return list.removeFirst();
        } catch (NoSuchElementException e) {
            throw new EntityStoreException(cache.getName(), threadId);
        }
    }

    @Override
    public void copyFromThread(long sourceThreadId) {
        long threadId = Thread.currentThread().getId();
        log.debug("Copying key value pairs from thread="+sourceThreadId+" to thread="+threadId);

        Element sourceEl = cache.get(sourceThreadId);

        if (sourceEl == null) {
            throw new EntityStoreException(cache.getName(), sourceThreadId);
        }

        @SuppressWarnings("unchecked")
        LinkedList<Long> list = (LinkedList<Long>)sourceEl.getObjectValue();

        // copy by reference
        cache.put(new Element(threadId, list));
    }

}
