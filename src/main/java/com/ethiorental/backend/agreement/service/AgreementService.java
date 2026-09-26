package com.ethiorental.backend.agreement.service;

import com.ethiorental.backend.agreement.dto.AgreementResponse;
import com.ethiorental.backend.lease.entity.LeaseRequest;

import java.util.List;

public interface AgreementService {

    AgreementResponse createAgreementFromLeaseRequest(LeaseRequest leaseRequest, String supervisorEmail);

    List<AgreementResponse> getMyAgreements(String userEmail);

    List<AgreementResponse> getLandlordAgreements(String userEmail);

    List<AgreementResponse> getTenantAgreements(String userEmail);

    List<AgreementResponse> getTenantActiveAgreements(String userEmail);

    AgreementResponse getAgreementByNumber(String agreementNumber);

    AgreementResponse getAgreementByRequestCode(String requestCode);

    List<AgreementResponse> getAllActiveAgreements();

    List<AgreementResponse> getAllAgreements();

    void expireAgreement(java.util.UUID agreementId);

    void renewAgreement(String agreementNumber, String userEmail);

    void requestCancellation(String agreementNumber, String landlordEmail);

    void acceptCancellation(String agreementNumber, String tenantEmail);

    void tenantCancelAgreement(String agreementNumber, String tenantEmail);
}
