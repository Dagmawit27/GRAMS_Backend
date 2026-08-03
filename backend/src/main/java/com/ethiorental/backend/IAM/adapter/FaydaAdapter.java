package com.ethiorental.backend.IAM.adapter;

import com.ethiorental.backend.IAM.dto.FaydaCitizenResponseDto;

public interface FaydaAdapter {
    /**
     * Verifies citizen identity against the Fayda National Digital Identity System.
     * When Fayda API integration is active, queries the external Fayda REST endpoint.
     * When pending, allows local citizen profile creation with Fayda ID tracking.
     */
    FaydaCitizenResponseDto fetchCitizenIdentity(Long faydaId);

    boolean isApiEnabled();
}
