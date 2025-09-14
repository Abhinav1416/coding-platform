package com.Abhinav.backend.features.problems.repository;

import com.Abhinav.backend.features.problems.models.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    Optional<Problem> findBySlug(String slug);
}
