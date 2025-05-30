/*
 * Copyright (c) 28/05/2025, 13:40, Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */


package com.oracle.coherence.graal.model.java;

import java.io.Serializable;
import java.util.Objects;

public class OrderLine implements Serializable {
    private int orderId;
    private int orderLineId;
    private String product;
    private int quantity;
    private double price;

    public OrderLine(int orderId, int orderLineId, String product, int quantity, double price) {
        this.orderId = orderId;
        this.orderLineId = orderLineId;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getOrderLineId() {
        return orderLineId;
    }

    public void setOrderLineId(int orderLineId) {
        this.orderLineId = orderLineId;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        OrderLine orderLine = (OrderLine) o;
        return orderId == orderLine.orderId && orderLineId == orderLine.orderLineId && quantity == orderLine.quantity && Double.compare(price, orderLine.price) == 0 && Objects.equals(product, orderLine.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, orderLineId, product, quantity, price);
    }

    @Override
    public String toString() {
        return "OrderLine{" +
                "orderId=" + orderId +
                ", orderLineId=" + orderLineId +
                ", product='" + product + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}
