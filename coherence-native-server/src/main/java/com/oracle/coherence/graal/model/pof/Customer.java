/*
 * Copyright (c) 28/05/2025, 13:40, Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.graal.model.pof;

import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.io.pof.schema.annotation.PortableType;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

import java.util.Objects;

@PortableType(id = 1000)
public class Customer {
    private int id;
    private String name;
    private double balance;
    private Address homeAddress;
    private Address workAddress;
    private String customerType;

    public Customer() {}

    public Customer(int id, String name, double balance, Address homeAddress, Address workAddress, String customerType) {
        this.id = id;
        this.name = name;
        this.balance = balance;
        this.homeAddress = homeAddress;
        this.workAddress = workAddress;
        this.customerType = customerType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public Address getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(Address homeAddress) {
        this.homeAddress = homeAddress;
    }

    public Address getWorkAddress() {
        return workAddress;
    }

    public void setWorkAddress(Address workAddress) {
        this.workAddress = workAddress;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return id == customer.id && Double.compare(balance, customer.balance) == 0 && Objects.equals(name, customer.name) && Objects.equals(homeAddress, customer.homeAddress) && Objects.equals(workAddress, customer.workAddress) && Objects.equals(customerType, customer.customerType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, balance, homeAddress, workAddress, customerType);
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", balance=" + balance +
                ", homeAddress=" + homeAddress +
                ", workAddress=" + workAddress +
                ", customerType='" + customerType + '\'' +
                '}';
    }
}
