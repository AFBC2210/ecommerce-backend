package com.parquesoft.ecommerce.service;

import com.parquesoft.ecommerce.entity.OrderEntity;
import com.parquesoft.ecommerce.repository.OrderRepository;
// Importaciones de JUnit 5 (el motor que ejecuta las pruebas y verifica resultados)
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
// Importaciones de Mockito (para crear objetos falsos o simulados)
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

//@ExtendWith(MockitoExtension.class) activa Mockito para esta clase.
// Al NO usar @SpringBootTest, la prueba corre en milisegundos y nunca toca la base de datos real.
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    // @Mock crea un "doble de acción" (un repositorio falso) que simula responder sin ir a PostgreSQL
    @Mock
    private OrderRepository orderRepository;

    // @InjectMocks crea una instancia real de OrderService y le inyecta adentro el repositorio falso (@Mock)
    @InjectMocks
    private OrderService orderService;

    // @Test marca este metodo como una prueba automatizada ejecutable
    @Test
    // @DisplayName muestra una descripción legible en español en el panel de resultados de IntelliJ
    @DisplayName("createOrder: guarda con estado PENDING cuando el monto es positivo")
    void testCreateOrder_Success() {
        //(Preparar): creamos los datos de entrada de prueba
        OrderEntity newOrder = new OrderEntity();
        newOrder.setCustomerId(1L);
        newOrder.setTotalAmount(185000.50);
        newOrder.setStatus("PAID"); // Simulamos que alguien intenta hacer trampa enviando estado "PAID"

        // Entrenamos al repositorio falso: "Cuando te llamen a guardar (save), simula que la BD le puso id = 1"
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        //(Actuar): ejecutamos el metodo real que queremos probar
        OrderEntity result = orderService.createOrder(newOrder);

        //(Verificar): comprobamos que el resultado cumpla todas nuestras reglas de negocio
        assertNotNull(result);                          // Verifica que no devolvió null
        assertEquals(1L, result.getId());               // Verifica que recibió el ID 1
        assertEquals("PENDING", result.getStatus());    // verifica que ignoró "PAID" y forzó "PENDING"
        assertNotNull(result.getOrderDate());           // verifica que asignó la fecha automática
        // Verifica que el servicio llamó exactamente 1 vez al metodo save() del repositorio
        verify(orderRepository, times(1)).save(newOrder);
    }

    // @ParameterizedTest ejecuta esta misma prueba varias veces, una por cada valor de @ValueSource
    @ParameterizedTest
    // @ValueSource inyecta primero 0.0 y luego -50.0 en el parámetro 'invalidAmount'
    @ValueSource(doubles = {0.0, -50.0})
    @DisplayName("createOrder: lanza IllegalArgumentException si el monto es <= 0 y nunca guarda")
    void testCreateOrder_ThrowsException_WhenTotalIsZeroOrNegative(double invalidAmount) {
        //preparamos una orden con monto inválido (0.0 en la primera vuelta, -50.0 en la segunda)
        OrderEntity invalidOrder = new OrderEntity();
        invalidOrder.setCustomerId(1L);
        invalidOrder.setTotalAmount(invalidAmount);

        //verificamos que al llamar createOrder se lance obligatoriamente la excepción esperada
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(invalidOrder));
        // Verificamos que el mensaje de error sea exactamente el que programamos
        assertEquals("El monto total del pedido debe ser mayor a cero", ex.getMessage());

        // Garantizamos que, como falló la validación, NUNCA (never) se intentó guardar en la base de datos
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    @DisplayName("getOrderById: retorna la orden cuando existe")
    void testGetOrderById_Success() {
        // creamos una orden ficticia y entrenamos al mock para que la devuelva al buscar ID 1
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setCustomerId(1L);
        order.setTotalAmount(50000.0);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // pedimos la orden 1 al servicio
        Optional<OrderEntity> result = orderService.getOrderById(1L);

        // comprobamos que la caja Optional viene llena y tiene el ID 1
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(orderRepository).findById(1L);
    }

    @Test
    @DisplayName("getOrderById: retorna Optional.empty() cuando el ID no existe")
    void testGetOrderById_NotFound() {
        // entrenamos al mock para que devuelva vacío cuando busquen el ID 99
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // pedimos la orden 99
        Optional<OrderEntity> result = orderService.getOrderById(99L);

        // comprobamos que el resultado viene vacío
        assertTrue(result.isEmpty());
        verify(orderRepository).findById(99L);
    }
}
