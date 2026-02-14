package com.ecommerce.project.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Tracking DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingDTO {

    private Long orderId;
    private String email;
    private LocalDate orderDate;
    private Double totalAmount;
    private String currentStatus;

    // Tracking info
    private String trackingNumber;
    private String carrier;
    private String estimatedDelivery;

    // Status history (newest first)
    private List<StatusHistoryDTO> statusHistory = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistoryDTO {
        private String status;
        private String description;
        private String notes;
        private LocalDateTime timestamp;
        private String location;
    }
}
