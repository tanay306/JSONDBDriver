package com.driver;

/**
 * Represents an address associated with a user.
 */
public class Address {
    public String city, state, country, pincode;

    public Address() {}  

    public Address(String city, String state, String country, String pincode) {
        this.city = city;
        this.state = state;
        this.country = country;
        this.pincode = pincode;
    }
}
