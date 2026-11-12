package com.paynest.domain;

import java.util.regex.Pattern;

/** The person placing an order. */
public class Customer {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    private final int id;
    private final String name;
    private final String email;

/**
* @param id the unique identifier for the customer
* @param name the customer's name
* @param email the customer's email address
 */
    public Customer(int id, String name, String email) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank.");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("email is not valid: " + email);
        }

        this.id = id;
        this.name = name.trim();
        this.email = email.trim();
    }

    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }
}