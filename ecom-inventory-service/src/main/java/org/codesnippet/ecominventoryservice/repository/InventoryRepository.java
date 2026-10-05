package org.codesnippet.ecominventoryservice.repository;

import org.codesnippet.ecominventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory,Long> {
}
