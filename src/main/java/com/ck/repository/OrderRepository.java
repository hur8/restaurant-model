package com.ck.repository;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ck.model.Order;
import com.ck.util.*;



public class OrderRepository implements Storage {

    public static final String TEMP_COLD = "cold";
    public static final String TEMP_HOT  = "hot";
    public static final String TEMP_ROOM = "room";
    public static final int    CAPACITY_COOLER = 6;
    public static final int    CAPACITY_HEATER = 6;
    public static final int    CAPACITY_SHELF  = 12;

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderRepository.class);

    public ConcurrentHashMap<String, // orderId
                            StorageType> orderToStorageLookup;

    public HashMap<String, StorageType> temperatureToStorageLookup;

    public ConcurrentHashMap<StorageType, Integer> storageToSizeMap;

    public ConcurrentHashMap<StorageType, ConcurrentHashMap<String,Order>> storageMealMap;

    public OrderRepository() { 
        orderToStorageLookup       = buildOrderToStorageLookup();
        temperatureToStorageLookup = buildTemperatureToStorageLookup();
        storageMealMap             = buildStorageMealMap();
        storageToSizeMap           = buildStorageToSizeMap();
    }

    /**
     * Split this up
     * Mutation: delete Order orderToStorageLookup
     * Store/put:
     * storageMealMap
     * orderToStorageLookup
     */
    public String /*orderId*/ storeOrder( Order order) {
        StorageType idealStorageType = temperatureToStorageLookup.get(order.getTemp());
        Optional<StorageType> availableStorageOpt = findAvailableStorageType(idealStorageType);
        StorageType candidateStorageType;
        if (availableStorageOpt.isEmpty())
        {
                /*  find an order to delete.
                delete the order
                claim the space of the deleted order: -> the StorageType
                StorageType of the deleted order
                */
            Iterator<String> shelfKeyIt = storageMealMap.get(StorageType.SHELF).keySet().iterator();
            String orderIdToDelete = shelfKeyIt.next(); // TODO replace random delete with LRU

            Order deletedShelfOrder = storageMealMap.get(StorageType.SHELF).remove(orderIdToDelete);
            StorageType storageTypeEntryToDelete = orderToStorageLookup.remove(orderIdToDelete);
            LOGGER.info( orderIdToDelete + " ORDER STATUS: DISCARD from StorageType: " + storageTypeEntryToDelete + ".  Action: " + OrderState.DISCARD.getOrderState() + " order: " + deletedShelfOrder);
            candidateStorageType = StorageType.SHELF; // TODO
        } else {
            candidateStorageType = availableStorageOpt.get();
        }
        storageMealMap.get(candidateStorageType).put(order.getId(), order);
        LOGGER.info("[OR.storeOrder] Stored " + order.getId() + " in " + candidateStorageType, storageMealMap.toString() + "\n");
        orderToStorageLookup.put(order.getId(), candidateStorageType);
        LOGGER.info("[OR.storeOrder] orderToStorageLookup: " + orderToStorageLookup.toString() + "\n");
        return order.getId();
    };

    public Optional<StorageType> findAvailableStorageType(StorageType storageType) {
        Optional<StorageType> availableStorageOpt;
        ConcurrentHashMap<String, Order> currentStorage = storageMealMap.get(storageType);

        // check for space in ideal storage type
        if (currentStorage.keySet().size() <= storageToSizeMap.get(storageType)) {
            availableStorageOpt = Optional.ofNullable(storageType);

        // check for space on shelf
        } else if (storageMealMap.get(StorageType.SHELF).keySet().size() < storageToSizeMap.get(StorageType.SHELF)) { // TODO need structural enforcement, as another thread could put to the map
            availableStorageOpt = Optional.ofNullable(StorageType.SHELF);

        } else { // check the N - 2 last storage option.  Only works for N = 3
            EnumSet<StorageType> allStorageTypes = EnumSet.of(
                StorageType.COOLER,
                StorageType.HEATER,
                StorageType.SHELF);
            boolean isRemovedIdealStorageType = allStorageTypes.remove(storageType);
            boolean isRemovedStorageTypeShelf = allStorageTypes.remove(StorageType.SHELF);
            Iterator<StorageType> it = allStorageTypes.iterator();
            StorageType remainingStorageType = it.next();
            if (storageMealMap.get(remainingStorageType).keySet().size() < storageToSizeMap.get(remainingStorageType)) {
                availableStorageOpt = Optional.ofNullable(remainingStorageType);
            } else {
                // no storage.  must delete an existing order from Shelf
                availableStorageOpt = Optional.empty();
            }
        }
        return availableStorageOpt;
    }

	public Optional<Order> retrieveOrder(String orderId) {
        Optional<Order> pickupOrderOpt;
        if (orderId != null) {
            if (orderToStorageLookup.containsKey(orderId) ){
                StorageType pickupStorageType = orderToStorageLookup.get(orderId);
                Order foundOrder = storageMealMap.get(pickupStorageType).get(orderId);
                pickupOrderOpt = Optional.ofNullable(foundOrder);
            } else {
                pickupOrderOpt = Optional.empty();
            }
        }
        else {
            pickupOrderOpt = Optional.empty();
        }
        return pickupOrderOpt;
    };

    public ConcurrentHashMap<StorageType, Integer> buildStorageToSizeMap() {
        ConcurrentHashMap<StorageType, Integer> storageToSizeMap = new ConcurrentHashMap<>();
        storageToSizeMap.put(StorageType.COOLER, CAPACITY_COOLER);
        storageToSizeMap.put(StorageType.HEATER, CAPACITY_HEATER);
        storageToSizeMap.put(StorageType.SHELF, CAPACITY_SHELF);
        return storageToSizeMap;
    }
    // TODO temporarily make public for unit testing
    public final HashMap<String, StorageType> buildTemperatureToStorageLookup() {
        HashMap<String, StorageType> temperatureToStorageLocation = new HashMap<>();
        temperatureToStorageLocation.put(TEMP_COLD, StorageType.COOLER);
        temperatureToStorageLocation.put(TEMP_HOT, StorageType.HEATER);
        temperatureToStorageLocation.put(TEMP_ROOM, StorageType.SHELF);
        return temperatureToStorageLocation;
    }

    public ConcurrentHashMap<StorageType, ConcurrentHashMap<String,Order>> buildStorageMealMap() {
        ConcurrentHashMap<StorageType, ConcurrentHashMap<String,Order>> mealStorageMap = new ConcurrentHashMap<>();
        EnumMap          <StorageType, ConcurrentHashMap<String,Order>> enumMap        = new EnumMap<>(StorageType.class);

        ConcurrentHashMap<String,Order> coolerStorage = new ConcurrentHashMap<>(CAPACITY_COOLER);
        ConcurrentHashMap<String,Order> heaterStorage = new ConcurrentHashMap<>(CAPACITY_HEATER);
        ConcurrentHashMap<String,Order> shelfStorage  = new ConcurrentHashMap<>(CAPACITY_SHELF);

        enumMap.put(StorageType.COOLER, coolerStorage);
        enumMap.put(StorageType.HEATER, heaterStorage);
        enumMap.put(StorageType.SHELF,  shelfStorage);
        
        mealStorageMap.putAll(enumMap);
        return mealStorageMap;
    }


    public ConcurrentHashMap<String, // orderId
                            StorageType> buildOrderToStorageLookup() {
        ConcurrentHashMap<String, StorageType> orderToStorageLookup = new ConcurrentHashMap<>();
        return orderToStorageLookup;
    }


}