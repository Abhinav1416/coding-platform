package com.Abhinav.backend.features.problemManagement.PMRepository;


import com.Abhinav.backend.features.problemManagement.PMModel.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {
}