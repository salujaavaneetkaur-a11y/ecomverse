package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Order;
import com.ecommerce.project.model.OrderStatus;
import com.ecommerce.project.model.OrderStatusHistory;
import com.ecommerce.project.payload.OrderTrackingDTO;
import com.ecommerce.project.repositories.OrderRepository;
import com.ecommerce.project.repositories.OrderStatusHistoryRepository;
import com.ecommerce.project.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderTrackingService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStatusHistoryRepository historyRepository;

    @Autowired
    private AuthUtil authUtil;

    @Autowired(required = false)
    private EmailService emailService;

    public OrderTrackingDTO getOrderTracking(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        verifyOrderAccess(order);

        List<OrderStatusHistory> history = historyRepository.findByOrderOrderIdOrderByTimestampDesc(orderId);

        return mapToDTO(order, history);
    }

    public OrderTrackingDTO updateOrderStatus(Long orderId, OrderStatus newStatus, String notes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        OrderStatus currentStatus = OrderStatus.valueOf(order.getOrderStatus().replace(" ", "_").toUpperCase());

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new APIException(String.format(
                    "Cannot transition from %s to %s",
                    currentStatus.getDisplayName(),
                    newStatus.getDisplayName()
            ));
        }

        order.setOrderStatus(newStatus.getDisplayName());
        orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(newStatus);
        history.setNotes(notes);
        history.setUpdatedBy(authUtil.loggedInUser().getUserName());
        historyRepository.save(history);

        if (emailService != null) {
            emailService.sendOrderStatusUpdateEmail(order.getEmail(), order, newStatus);
        }

        List<OrderStatusHistory> fullHistory = historyRepository.findByOrderOrderIdOrderByTimestampDesc(orderId);
        return mapToDTO(order, fullHistory);
    }

    public OrderTrackingDTO addTrackingInfo(Long orderId, String trackingNumber, String carrier) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.SHIPPED);
        history.setTrackingNumber(trackingNumber);
        history.setCarrier(carrier);
        history.setNotes("Tracking information added");
        history.setUpdatedBy(authUtil.loggedInUser().getUserName());
        historyRepository.save(history);

        if (!order.getOrderStatus().equals(OrderStatus.SHIPPED.getDisplayName())) {
            order.setOrderStatus(OrderStatus.SHIPPED.getDisplayName());
            orderRepository.save(order);
        }

        if (emailService != null) {
            emailService.sendShippingNotificationEmail(order.getEmail(), order, trackingNumber, carrier);
        }

        List<OrderStatusHistory> fullHistory = historyRepository.findByOrderOrderIdOrderByTimestampDesc(orderId);
        return mapToDTO(order, fullHistory);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByOrderStatus(status.getDisplayName());
    }

    public OrderTrackingDTO cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        verifyOrderAccess(order);

        OrderStatus currentStatus = parseStatus(order.getOrderStatus());

        if (!currentStatus.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new APIException("Order cannot be cancelled in current status: " + currentStatus.getDisplayName());
        }

        return updateOrderStatus(orderId, OrderStatus.CANCELLED, reason);
    }

    private void verifyOrderAccess(Order order) {
        String userEmail = authUtil.loggedInEmail();
        boolean isAdmin = authUtil.loggedInUser().getRoles().stream()
                .anyMatch(role -> role.getRoleName().name().equals("ROLE_ADMIN"));

        if (!isAdmin && !order.getEmail().equals(userEmail)) {
            throw new APIException("You don't have access to this order");
        }
    }

    private OrderStatus parseStatus(String statusStr) {
        try {
            return OrderStatus.valueOf(statusStr.replace(" ", "_").toUpperCase());
        } catch (IllegalArgumentException e) {
            return OrderStatus.PENDING;
        }
    }

    private OrderTrackingDTO mapToDTO(Order order, List<OrderStatusHistory> history) {
        OrderTrackingDTO dto = new OrderTrackingDTO();
        dto.setOrderId(order.getOrderId());
        dto.setEmail(order.getEmail());
        dto.setOrderDate(order.getOrderDate());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCurrentStatus(order.getOrderStatus());

        history.stream()
                .filter(h -> h.getTrackingNumber() != null)
                .findFirst()
                .ifPresent(h -> {
                    dto.setTrackingNumber(h.getTrackingNumber());
                    dto.setCarrier(h.getCarrier());
                });

        dto.setStatusHistory(history.stream()
                .map(h -> {
                    OrderTrackingDTO.StatusHistoryDTO historyDTO = new OrderTrackingDTO.StatusHistoryDTO();
                    historyDTO.setStatus(h.getStatus().getDisplayName());
                    historyDTO.setDescription(h.getStatus().getDescription());
                    historyDTO.setNotes(h.getNotes());
                    historyDTO.setTimestamp(h.getTimestamp());
                    historyDTO.setLocation(h.getLocation());
                    return historyDTO;
                })
                .collect(Collectors.toList()));

        return dto;
    }
}
