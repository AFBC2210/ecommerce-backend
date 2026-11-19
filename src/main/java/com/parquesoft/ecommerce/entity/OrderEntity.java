package com.parquesoft.ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long id;

    @Column(name = "customer_id", nullable = false)
    @Schema(example = "1")
    private Long customerId;

    @Column(name = "order_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(example = "2026-11-13T19:30:00")
    private LocalDateTime orderDate;

    @Column(name = "total_amount", nullable = false, columnDefinition = "numeric(12,2)")
    @Schema(example = "185000.50")
    private Double totalAmount;

    @Column(name = "status", nullable = false, length = 20)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "PENDING")
    private String status = "PENDING";

    public OrderEntity() {
    }

    // generamos los Getters y Setters de los 5 campos
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}