package com.financetracker.model;

import java.util.Objects;

/**
 * Entity model representing a transaction category.
 */
public class Category {
    private int id;
    private String name;
    private TransactionType type;

    public Category() {
    }

    public Category(int id, String name, TransactionType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public Category(String name, TransactionType type) {
        this(0, name, type);
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

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id == category.id || (name != null && name.equalsIgnoreCase(category.name));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name != null ? name.toLowerCase() : "");
    }

    @Override
    public String toString() {
        return name;
    }
}
