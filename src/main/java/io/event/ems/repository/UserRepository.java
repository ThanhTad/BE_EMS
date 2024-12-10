package io.event.ems.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.event.ems.model.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.fullName LIKE %:keyword%")
    Page<User> searchUser(@Param("keyword") String keyword, Pageable pageable);

    Optional<User> findByUsernameOrEmail(String username, String email);

}
