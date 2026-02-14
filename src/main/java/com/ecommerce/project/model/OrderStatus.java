package com.ecommerce.project.model;

public enum OrderStatus {

    PENDING("Pending", "Order has been placed, awaiting confirmation"),
    CONFIRMED("Confirmed", "Order has been confirmed"),
    PROCESSING("Processing", "Order is being prepared"),
    SHIPPED("Shipped", "Order has been shipped"),
    OUT_FOR_DELIVERY("Out for Delivery", "Order is out for delivery"),
    DELIVERED("Delivered", "Order has been delivered"),
    CANCELLED("Cancelled", "Order has been cancelled"),
    RETURNED("Returned", "Order has been returned"),
    REFUNDED("Refunded", "Order has been refunded");

    private final String displayName;
    private final String description;

    OrderStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED -> newStatus == PROCESSING || newStatus == CANCELLED;
            case PROCESSING -> newStatus == SHIPPED || newStatus == CANCELLED;
            case SHIPPED -> newStatus == OUT_FOR_DELIVERY || newStatus == DELIVERED;
            case OUT_FOR_DELIVERY -> newStatus == DELIVERED;
            case DELIVERED -> newStatus == RETURNED;
            case RETURNED -> newStatus == REFUNDED;
            case CANCELLED, REFUNDED -> false;
        };
    }
}
