package com.ethiorental.backend.lease.service;

import com.ethiorental.backend.lease.dto.LeaseRequestRequest;
import com.ethiorental.backend.lease.dto.LeaseRequestResponse;
import com.ethiorental.backend.lease.dto.LeaseStatusUpdateRequest;
import com.ethiorental.backend.lease.enums.LeaseRequestStatus;

import java.util.List;
import java.util.UUID;

public interface LeaseRequestService {

    /** Submit a new lease application for a property or unit. */
    LeaseRequestResponse submitLeaseRequest(LeaseRequestRequest request, String applicantEmail);

    /** Get all lease requests for the authenticated applicant. */
    List<LeaseRequestResponse> getMyLeaseRequests(String applicantEmail);

    /** Get all lease requests for a landlord's properties. */
    List<LeaseRequestResponse> getLandlordLeaseRequests(String landlordEmail);

    /** Get a single lease request by request code. */
    LeaseRequestResponse getLeaseRequestByCode(String requestCode);

    /** Update lease request status (approve/reject) - landlord only. */
    LeaseRequestResponse updateLeaseRequestStatus(String requestCode, LeaseStatusUpdateRequest request, String landlordEmail);

    /** Cancel a lease request - applicant only. */
    void cancelLeaseRequest(String requestCode, String applicantEmail);

    /** Delete a cancelled lease request - applicant only. */
    void deleteLeaseRequest(String requestCode, String applicantEmail);

    /** Get pending lease requests for a property. */
    List<LeaseRequestResponse> getPendingRequestsForProperty(UUID propertyId, String landlordEmail);

    /** Get pending lease requests for a specific unit. */
    List<LeaseRequestResponse> getPendingRequestsForUnit(UUID unitId, String landlordEmail);
}
