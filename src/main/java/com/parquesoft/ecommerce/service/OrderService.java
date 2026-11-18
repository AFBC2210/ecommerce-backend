package com.parquesoft.ecommerce.service;

import com.parquesoft.ecommerce.entity.OrderEntity;
import com.parquesoft.ecommerce.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;


@Service
public class OrderService {
    //creamos  4 estados unicos válidos en el sistema
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_SHIPPED = "SHIPPED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    private static final Set<String> VALID_STATUSES =
            Set.of(STATUS_PENDING, STATUS_PAID, STATUS_SHIPPED, STATUS_CANCELLED);

    // Declaramos el repositorio como 'final' para asegurar que no cambie una vez asignado
    private final OrderRepository orderRepository;

    // Spring detecta que OrderService necesita un OrderRepository y se lo entrega automáticamente.
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Transactional garantiza  toda la operacion
    // Si ocurre un error a mitad del metodo, revierte cualquier cambio en la BD (Rollback).
    @Transactional
    public OrderEntity createOrder(OrderEntity order) {
        if (order.getTotalAmount() == null || order.getTotalAmount() <= 0) {
            throw new IllegalArgumentException("El monto total del pedido debe ser mayor a cero");
        }
        if (order.getCustomerId() == null) {
            throw new IllegalArgumentException("El identificador del cliente es obligatorio");
        }
        // Forzamos el ID a null para garantizar que Hibernate haga un INSERT (nuevo) y no un UPDATE
        order.setId(null);
        order.setStatus(STATUS_PENDING); //pedidos nuevos se les asigna el estado PENDING
        //Si la fecha de la orden llega null, se le asigna la fecha y hora actual del servidor
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }
        return orderRepository.save(order);
    }

    //optimizamos el rendimiento de las consultas de solo lectura con readOnly=true
    @Transactional(readOnly = true)
    public List<OrderEntity> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    //traer la orden pero tambien puede venir vacía si el ID no existe
    public Optional<OrderEntity> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    // El Trim() Limpia espacios y toUpperCase() convierte a mayúsculas antes de buscar en la BD
    public List<OrderEntity> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status.trim().toUpperCase());
    }

    @Transactional(readOnly = true)
    // Llama a la consulta personalizada JPQL que escribimos en el repositorio
    public List<OrderEntity> getOrdersAboveAmount(double minAmount) {
        return orderRepository.findOrdersAboveAmount(minAmount);
    }

    // Metodo para cambiar el estado de una orden existente
    @Transactional
    public OrderEntity updateOrderStatus(Long id, String newStatus) {
        //Verifica que el texto del nuevo estado no venga vacío
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("El estado del pedido es obligatorio");
        }
        //Normaliza el texto (ej. " paid " -> "PAID") y verifica que esté dentro de los 4 permitidos
        String normalized = newStatus.trim().toUpperCase();
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Estado no valido: " + newStatus);
        }
        //Busca la orden en BD; si no existe, lanza una excepción inmediatamente
        OrderEntity existing = orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Orden no encontrada con ID: " + id));
        // Regla de negocio: si ya fue enviada (SHIPPED), no se puede pasar a CANCELLED
        if (STATUS_CANCELLED.equals(normalized) && STATUS_SHIPPED.equals(existing.getStatus())) {
            throw new IllegalStateException("No se puede cancelar una orden que ya fue enviada (SHIPPED)");
        }
        //Actualiza el estado y guarda los cambios en PostgreSQL
        existing.setStatus(normalized);
        return orderRepository.save(existing);
    }

    // Borrado lógico (Soft Delete). En lugar de eliminar la fila de la BD con deleteById(),
    // cambia su estado a "CANCELLED" para conservar el historial contable.
    @Transactional
    public void cancelOrder(Long id) {
        OrderEntity existing = orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Orden no encontrada con ID: " + id));
        if (STATUS_SHIPPED.equals(existing.getStatus())) {
            throw new IllegalStateException("No se puede cancelar una orden que ya fue enviada (SHIPPED)");
        }
        existing.setStatus(STATUS_CANCELLED);
        orderRepository.save(existing);
    }
}