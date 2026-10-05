package org.codesnippet.ecominventoryservice.service;

import org.codesnippet.ecominventoryservice.model.Inventory;
import org.codesnippet.ecominventoryservice.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class InventoryService {
    @Autowired
    InventoryRepository repository;
    public Inventory checkStock(Long productId) {
        Optional<Inventory> inv = repository.findById(productId);
        return inv.get();
    }

    public String addProduct(Inventory inventory) {
        repository.save(inventory);
        return "Product Added";
    }

    public String updateProduct( Inventory inventory) {
        repository.save( inventory);
        return "Product Updated";
    }

    public String deleteProduct(Long productId) {
        repository.deleteById(productId);
        return "Product Deleted";
    }
}