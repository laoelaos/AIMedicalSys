package com.aimedical.modules.commonmodule.permission;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByPhone(String phone);

    @EntityGraph(attributePaths = {"roles", "posts", "posts.functions"})
    Optional<User> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"roles", "posts", "posts.functions"})
    Optional<User> findWithDetailsForMenuById(Long id);

    @Query("SELECT u.tokenVersion FROM User u WHERE u.id = :id")
    Optional<Integer> findTokenVersionById(@Param("id") Long id);
}
