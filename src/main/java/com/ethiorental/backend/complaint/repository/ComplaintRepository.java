package com.ethiorental.backend.complaint.repository;

import com.ethiorental.backend.complaint.entity.Complaint;
import com.ethiorental.backend.complaint.enums.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

    /** All complaints filed by a specific citizen. */
    List<Complaint> findByComplainant_IdOrderByCreatedAtDesc(UUID citizenId);

    /** Complaints filtered by status — for officer queue. */
    List<Complaint> findByStatusOrderByCreatedAtAsc(ComplaintStatus status);
}
