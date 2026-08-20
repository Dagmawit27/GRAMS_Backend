package com.ethiorental.backend.complaint.repository;

import com.ethiorental.backend.complaint.entity.ComplaintResolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplaintResolutionRepository extends JpaRepository<ComplaintResolution, UUID> {

    Optional<ComplaintResolution> findByComplaint_Id(UUID complaintId);
}
