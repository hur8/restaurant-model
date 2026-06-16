package com.ck.model;

import java.util.concurrent.ConcurrentLinkedQueue;

public class OrderQueueProvider {
    private ConcurrentLinkedQueue<Order> queue;

    public OrderQueueProvider() {
        queue = new ConcurrentLinkedQueue<>();
    }

    public ConcurrentLinkedQueue<Order> getQueue() {
        return this.queue;
    }
}
