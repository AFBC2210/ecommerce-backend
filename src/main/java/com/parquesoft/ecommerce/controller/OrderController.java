package com.parquesoft.ecommerce.controller;

import com.parquesoft.ecommerce.entity.OrderEntity;
import com.parquesoft.ecommerce.service.OrderService;
// Importaciones de Swagger/OpenAPI para documentar visualmente cada endpoint
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
// Importaciones de Spring Web para manejar peticiones y respuestas HTTP
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

// @RestController combina @Controller + @ResponseBody: indica que esta clase recibe peticiones web
// y convierte automáticamente todas las respuestas de Java a formato JSON.
@RestController
// @RequestMapping define la ruta base para todos los métodos de esta clase: http://localhost:8081/api/orders
@RequestMapping("/api/orders")
// @Tag agrupa todos estos endpoints bajo la sección "3. Pedidos" en la interfaz de Swagger
@Tag(name = "3. Pedidos", description = "Endpoints para la gestion de pedidos de clientes")
// @SecurityRequirement le avisa a Swagger que estos endpoints requieren un token JWT (candado de seguridad)
@SecurityRequirement(name = "BearerAuth")
public class OrderController {

    private final OrderService orderService;

    // Inyección de dependencias por constructor: Spring conecta service.OrderService.java
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    //@PostMapping atiende peticiones HTTP POST en la ruta base: POST /api/orders
    @PostMapping
    @Operation(summary = "Crear una nueva orden de compra",
            description = "El estado se fuerza a PENDING y la fecha se asigna automaticamente si no se envia")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Orden creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Monto menor o igual a cero o datos invalidos")
    })
    // @RequestBody toma el JSON que envía el usuario en el cuerpo de la petición y lo convierte en un objeto OrderEntity
    public ResponseEntity<OrderEntity> createOrder(@RequestBody OrderEntity order) {
        // Devuelve el código HTTP 201 (CREATED) junto con la orden ya guardada y su ID generado
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(order));
    }

    //@GetMapping atiende peticiones HTTP GET en la ruta base: GET /api/orders
    @GetMapping
    @Operation(summary = "Listar todas las ordenes registradas")
    @ApiResponse(responseCode = "200", description = "Lista obtenida con exito")
    public ResponseEntity<List<OrderEntity>> getAllOrders() {
        // ResponseEntity.ok(...) envuelve la lista en una respuesta con código HTTP 200 (OK)
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    //{id} es una variable dentro de la URL. Ejemplo: GET /api/orders/5
    @GetMapping("/{id}")
    @Operation(summary = "Obtener el detalle de una orden por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden encontrada"),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    // @PathVariable extrae el número "5" de la URL y lo mete en la variable 'id' de Java
    public ResponseEntity<OrderEntity> getOrderById(
            @Parameter(description = "ID de la orden", example = "1") @PathVariable Long id) {
        // Si el Optional trae una orden (.map), responde 200 OK con la orden;
        // si viene vacío (.orElse), responde 404 Not Found sin cuerpo.
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //Ruta personalizada para filtrar por cliente. Ejemplo: GET /api/orders/customer/1
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Consultar pedidos de un cliente especifico")
    @ApiResponse(responseCode = "200", description = "Historial del cliente")
    public ResponseEntity<List<OrderEntity>> getOrdersByCustomer(
            @Parameter(description = "ID del cliente", example = "1") @PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomerId(customerId));
    }

    //Ruta para filtrar por estado. Ejemplo: GET /api/orders/status/PENDING
    @GetMapping("/status/{status}")
    @Operation(summary = "Filtrar pedidos segun su estado")
    @ApiResponse(responseCode = "200", description = "Pedidos filtrados por estado")
    public ResponseEntity<List<OrderEntity>> getOrdersByStatus(
            @Parameter(description = "PENDING, PAID, SHIPPED o CANCELLED", example = "PENDING")
            @PathVariable String status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    //@PutMapping se usa para actualizar recursos existentes. Ejemplo: PUT /api/orders/1/status?status=PAID
    @PutMapping("/{id}/status")
    @Operation(summary = "Actualizar el estado del pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Estado no valido"),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada"),
            @ApiResponse(responseCode = "409", description = "La orden enviada (SHIPPED) no puede cancelarse")
    })
    // Aquí combinamos dos formas de recibir datos en la URL:
    // - @PathVariable para el {id} que va dentro de la ruta (/1/)
    // - @RequestParam para el parámetro después del signo de interrogación (?status=PAID)
    public ResponseEntity<OrderEntity> updateOrderStatus(
            @Parameter(description = "ID de la orden", example = "1") @PathVariable Long id,
            @Parameter(description = "Nuevo estado", example = "PAID") @RequestParam String status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    /*
     MANEJADORES DE EXCEPCIONES LOCALES (@ExceptionHandler)
     Atrapan los errores que lanza OrderService y los convierten en respuestas JSON limpias
    */

    // Si el servicio lanza IllegalArgumentException (ej. monto <= 0), responde HTTP 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    // Si el servicio lanza NoSuchElementException (ej. ID no existe), responde HTTP 404 Not Found
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    // Si el servicio lanza IllegalStateException (ej. cancelar orden SHIPPED), responde HTTP 409 Conflict
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}
