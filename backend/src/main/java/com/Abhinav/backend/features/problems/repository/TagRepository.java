package com.Abhinav.backend.features.problems.repository;

import com.Abhinav.backend.features.problems.models.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    Set<Tag> findByNameIn(List<String> names);
}