package com.ethiorental.backend.agreement.service;

import com.ethiorental.backend.agreement.dto.AgreementResponse;

public interface AgreementService {
    /** Generate agreement for an approved lease request - landlord only. */
    AgreementResponse generateAgreement(String requestCode, String landlordEmail);

    /** Sign agreement by landlord - landlord only. */
    AgreementResponse signAgreement(String requestCode, String otp, String landlordEmail);

    /** Sign agreement by tenant - tenant only. */
    AgreementResponse signAgreementByTenant(String requestCode, String otp, String tenantEmail);

    /** Get agreement by request code. */
    AgreementResponse getAgreementByRequestCode(String requestCode);

    /** Get agreement by agreement code. */
    AgreementResponse getAgreementByAgreementCode(String agreementCode);
}
