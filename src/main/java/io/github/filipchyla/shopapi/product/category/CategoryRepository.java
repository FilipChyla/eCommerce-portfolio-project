package io.github.filipchyla.shopapi.product.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    @Query("SELECT c FROM Category c ORDER BY c.parent.id NULLS FIRST, c.name")
    List<Category> findAllOrdered();
}
