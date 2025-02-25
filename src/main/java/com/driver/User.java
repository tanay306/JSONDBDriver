package com.driver;

public class User {
    public String name, age, contact, company;
    public Address address;

    public User() {}  
    public User(String name, String age, String contact, String company, Address address) {
        this.name = name;
        this.age = age;
        this.contact = contact;
        this.company = company;
        this.address = address;
    }
}
