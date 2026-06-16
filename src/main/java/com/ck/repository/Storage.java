package com.ck.repository;

import java.util.Optional;

import com.ck.model.Order;

public interface Storage {
	abstract String /*orderId*/ storeOrder( Order order);
	abstract Optional<Order> retrieveOrder(String orderId);

}

