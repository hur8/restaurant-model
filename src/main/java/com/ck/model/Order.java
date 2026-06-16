package com.ck.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/** Order is a json-friendly representation of an order. */
public class Order {
  private final String id; // order id
  private final String name; // food name
  private final String temp; // ideal temperature
  private final int price; // price in dollars
  private final int freshness; // freshness in seconds

  public Order(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("temp") String temp,
      @JsonProperty("price") int price,
      @JsonProperty("freshness") int freshness) {
    this.id = id;
    this.name = name;
    this.temp = temp;
    this.price = price;
    this.freshness = freshness;
  }

  static List<Order> parse(String json) throws JsonProcessingException {
    return new ObjectMapper().readValue(json, new TypeReference<List<Order>>() {});
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getTemp() {
    return temp;
  }

  public int getPrice() {
    return price;
  }

  public int getFreshness() {
    return freshness;
  }

  @Override
  public String toString() {
    return "{id: "
        + id
        + ", name: "
        + name
        + ", temp: "
        + temp
        + ", price: $"
        + price
        + ", freshness:"
        + freshness
        + "}";
  }
}

    

