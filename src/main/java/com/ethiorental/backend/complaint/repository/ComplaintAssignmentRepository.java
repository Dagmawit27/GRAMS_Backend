package com.ethiorental.backend.complaint.repository;

import com.ethiorental.backend.complaint.entity.ComplaintAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplaintAssignmentRepository extends JpaRepository<ComplaintAssignment, UUID> {

    /** All assignment history for a complaint, newest first. */
    List<ComplaintAssignment> findByComplaint_IdOrderByAssignedAtDesc(UUID complaintId);

    /** The currently active assignment (revokedAt is null). */
    Optional<ComplaintAssignment> findByComplaint_IdAndRevokedAtIsNull(UUID complaintId);

    /** All complaints currently assigned to a specific officer. */
    List<ComplaintAssignment> findByAssignedOfficer_IdAndRevokedAtIsNull(UUID officerId);
}
