package com.Abhinav.backend.features.problemManagement.PMRepository;

import com.Abhinav.backend.features.problemManagement.PMModel.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByName(String name);
}
