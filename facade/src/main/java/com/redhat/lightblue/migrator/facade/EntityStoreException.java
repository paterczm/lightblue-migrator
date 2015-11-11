package com.redhat.lightblue.migrator.facade;


public class EntityStoreException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EntityStoreException(String cacheName, long threadId) {
        super("No ids found for "+cacheName+" thread="+threadId+"!");
    }

    public EntityStoreException(String cacheName, long threadId, Throwable e) {
        super("No ids found for "+cacheName+" thread="+threadId+"!", e);
    }

}
