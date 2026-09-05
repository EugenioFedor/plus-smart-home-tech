package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.*;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderPersistenceService {

    private final OrderRepository orderRepository;

    @Transactional
    public OrderDto saveConfirmedOrder(
            CreateOrderRequest request,
            Map<Long, ProductResponse> products
    ) {
        Order order = new Order();

        order.setCustomerName(request.customerName());
        order.setCustomerEmail(request.customerEmail());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items()) {
            ProductResponse product =
                    products.get(itemRequest.productId());

            OrderItem item = new OrderItem();

            item.setProductId(product.id());
            item.setProductName(product.name());
            item.setQuantity(itemRequest.quantity());
            item.setPrice(product.price());
            item.setOrder(order);

            order.getItems().add(item);

            BigDecimal itemTotal = product.price()
                    .multiply(
                            BigDecimal.valueOf(
                                    itemRequest.quantity()
                            )
                    );

            totalPrice = totalPrice.add(itemTotal);
        }

        order.setTotalPrice(totalPrice);

        Order savedOrder = orderRepository.save(order);

        return toDto(savedOrder);
    }

    @Transactional
    public OrderDto savePendingOrder(
            CreateOrderRequest request,
            Map<Long, ProductResponse> products,
            String statusDetails
    ) {
        Order order = new Order();

        order.setCustomerName(request.customerName());
        order.setCustomerEmail(request.customerEmail());
        order.setStatus(OrderStatus.PENDING_CONFIRMATION);
        order.setStatusDetails(statusDetails);
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items()) {
            ProductResponse product =
                    products.get(itemRequest.productId());

            OrderItem item = new OrderItem();

            item.setProductId(itemRequest.productId());
            item.setQuantity(itemRequest.quantity());
            item.setOrder(order);

            if (product != null) {
                item.setProductName(product.name());
                item.setPrice(product.price());

                BigDecimal itemTotal = product.price()
                        .multiply(
                                BigDecimal.valueOf(
                                        itemRequest.quantity()
                                )
                        );

                totalPrice = totalPrice.add(itemTotal);

            } else {
                item.setProductName(
                        "Товар #" + itemRequest.productId()
                                + " (ожидает проверки)"
                );
                item.setPrice(BigDecimal.ZERO);
            }

            order.getItems().add(item);
        }

        order.setTotalPrice(totalPrice);

        Order savedOrder = orderRepository.save(order);

        return toDto(savedOrder);
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