package com.Abhinav.backend.features.authentication.repository;

import com.Abhinav.backend.features.authentication.model.Role;
import com.Abhinav.backend.features.authentication.model.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(RoleType name);
}