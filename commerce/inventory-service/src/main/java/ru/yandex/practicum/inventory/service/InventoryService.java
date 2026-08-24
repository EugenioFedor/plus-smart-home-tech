package ru.yandex.practicum.inventory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.Inventory;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.InvalidStateException;
import ru.yandex.practicum.inventory.exception.NotFoundException;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryDto createInventory(UpdateInventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.productId())) {
            throw new InvalidStateException(
                    "Inventory for product " + request.productId() + " already exists"
            );
        }

        Inventory inventory = new Inventory();
        inventory.setProductId(request.productId());
        inventory.setQuantity(request.quantity());
        inventory.setReservedQuantity(0);

        return toDto(inventoryRepository.save(inventory));
    }

    public InventoryDto updateInventory(UpdateInventoryRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Inventory for product " + request.productId() + " not found"
                        )
                );

        if (request.quantity() < inventory.getReservedQuantity()) {
            throw new InvalidStateException(
                    "Quantity cannot be less than reserved quantity"
            );
        }

        inventory.setQuantity(request.quantity());

        return toDto(inventoryRepository.save(inventory));
    }

    public List<InventoryDto> getAllInventory() {
        return inventoryRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public InventoryDto getByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Inventory for product " + productId + " not found"
                        )
                );

        return toDto(inventory);
    }

    public ReserveResponse reserveStock(ReserveRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Inventory for product " + request.productId() + " not found"
                        )
                );

        int available = inventory.getAvailableQuantity();

        if (available < request.quantity()) {
            throw new InsufficientStockException(
                    "Not enough stock for product " + request.productId()
            );
        }

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() + request.quantity()
        );

        Inventory saved = inventoryRepository.save(inventory);

        return new ReserveResponse(
                true,
                saved.getAvailableQuantity(),
                "Товар успешно зарезервирован"
        );
    }

    private InventoryDto toDto(Inventory inventory) {
        return new InventoryDto(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity()
        );
    }
}
