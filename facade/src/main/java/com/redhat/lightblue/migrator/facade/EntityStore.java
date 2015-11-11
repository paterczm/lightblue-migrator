package com.redhat.lightblue.migrator.facade;

/**
 * A FIFO queue. Used to pass IDs and other objects to create apis in Lightblue DAO without changing the signatures.
 *
 *
 * @author mpatercz
 *
 */
public interface EntityStore {

    public void push(Object obj);

    public Object pop();

    /**
     * Copy all key-value pairs from one thread to the other.
     *
     */
    public void copyFromThread(long sourceThreadId);

}
