package io.event.ems.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.event.ems.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsByName(String name);

}
