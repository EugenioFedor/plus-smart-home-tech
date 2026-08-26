package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.client.InventoryClient;
import ru.yandex.practicum.order.client.ProductClient;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.InventoryRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.dto.ProductResponse;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log =
            LoggerFactory.getLogger(OrderService.class);

    private final OrderPersistenceService orderPersistenceService;
    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    public OrderDto createOrder(CreateOrderRequest request) {
        Map<Long, ProductResponse> products = new HashMap<>();
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        Map<Long, Integer> successfulReservations = new LinkedHashMap<>();

        try {
            collectProductsAndQuantities(
                    request,
                    products,
                    quantities
            );

            reserveProducts(
                    quantities,
                    successfulReservations
            );

            return orderPersistenceService.saveConfirmedOrder(
                    request,
                    products
            );

        } catch (ProductServiceUnavailableException e) {
            log.warn(
                    "Заказ будет сохранён в статусе PENDING_CONFIRMATION: product-service недоступен",
                    e
            );

            return orderPersistenceService.savePendingOrder(
                    request,
                    products,
                    "Каталог временно недоступен. Заказ требует ручной проверки."
            );

        } catch (InventoryServiceUnavailableException e) {
            log.warn(
                    "Заказ будет сохранён в статусе PENDING_CONFIRMATION: inventory-service недоступен",
                    e
            );

            releaseReservations(successfulReservations);

            return orderPersistenceService.savePendingOrder(
                    request,
                    products,
                    "Склад временно недоступен. Резервирование требует ручной проверки."
            );

        } catch (OrderProcessingException e) {
            releaseReservations(successfulReservations);
            throw e;

        } catch (Exception e) {
            releaseReservations(successfulReservations);

            throw new OrderProcessingException(
                    "Не удалось создать заказ",
                    e
            );
        }
    }

    private void collectProductsAndQuantities(
            CreateOrderRequest request,
            Map<Long, ProductResponse> products,
            Map<Long, Integer> quantities
    ) {
        for (OrderItemRequest itemRequest : request.items()) {
            Long productId = itemRequest.productId();

            ProductResponse product = products.get(productId);

            if (product == null) {
                product = getProduct(productId);

                if (!Boolean.TRUE.equals(product.active())) {
                    throw new OrderProcessingException(
                            "Товар " + productId + " снят с продажи"
                    );
                }

                products.put(productId, product);
            }

            quantities.merge(
                    productId,
                    itemRequest.quantity(),
                    Integer::sum
            );
        }
    }

    private void reserveProducts(
            Map<Long, Integer> quantities,
            Map<Long, Integer> successfulReservations
    ) {
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            reserveProduct(productId, quantity);

            successfulReservations.put(productId, quantity);
        }
    }

    private ProductResponse getProduct(Long productId) {
        try {
            ProductResponse product =
                    productClient.getProductById(productId);

            if (product == null) {
                throw new OrderProcessingException(
                        "Товар " + productId + " не найден"
                );
            }

            return product;

        } catch (ProductServiceUnavailableException e) {
            throw e;

        } catch (FeignException.NotFound e) {
            throw new OrderProcessingException(
                    "Товар " + productId + " не найден",
                    e
            );

        } catch (FeignException e) {
            throw new OrderProcessingException(
                    "Не удалось получить данные товара " + productId,
                    e
            );
        }
    }

    private void reserveProduct(Long productId, Integer quantity) {
        try {
            inventoryClient.reserve(
                    new InventoryRequest(productId, quantity)
            );

        } catch (InventoryServiceUnavailableException e) {
            throw e;

        } catch (FeignException.NotFound e) {
            throw new OrderProcessingException(
                    "Складская запись для товара " + productId + " не найдена",
                    e
            );

        } catch (FeignException.Conflict e) {
            throw new OrderProcessingException(
                    "Недостаточно товара " + productId + " на складе",
                    e
            );

        } catch (FeignException e) {
            throw new OrderProcessingException(
                    "Не удалось зарезервировать товар " + productId,
                    e
            );
        }
    }

    private void releaseReservations(
            Map<Long, Integer> successfulReservations
    ) {
        for (Map.Entry<Long, Integer> entry
                : successfulReservations.entrySet()) {

            try {
                inventoryClient.release(
                        new InventoryRequest(
                                entry.getKey(),
                                entry.getValue()
                        )
                );

            } catch (Exception e) {
                log.error(
                        "Не удалось снять резерв для товара id={}, quantity={}",
                        entry.getKey(),
                        entry.getValue(),
                        e
                );
            }
        }
    }

    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Order with id " + id + " not found"
                        )
                );

        return toDto(order);
    }

    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<OrderDto> getOrdersByEmail(String email) {
        return orderRepository.findByCustomerEmail(email)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private OrderDto toDto(Order order) {
        List<OrderItemDto> items = order.getItems()
                .stream()
                .map(item -> new OrderItemDto(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice()
                ))
                .toList();

        return new OrderDto(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus().name(),
                order.getTotalPrice(),
                order.getStatusDetails(),
                order.getCreatedAt(),
                items
        );
    }
}