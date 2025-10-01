package com.Abhinav.backend.features.authentication.repository;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthenticationUserRepository extends JpaRepository<AuthenticationUser, Long> {
    Optional<AuthenticationUser> findByEmail(String email);

    @Query("SELECT u FROM users u WHERE u.email LIKE :username || '@%'")
    Optional<AuthenticationUser> findByUsername(@Param("username") String username);
}
