package com.aimedical.modules.commonmodule.permission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByCode(String code);

    boolean existsByCode(String code);

    List<Post> findByRoleId(Long roleId);

    void removeByRoleId(Long roleId);
}
