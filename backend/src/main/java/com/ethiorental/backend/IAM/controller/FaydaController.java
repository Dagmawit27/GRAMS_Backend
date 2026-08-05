package com.ethiorental.backend.IAM.controller;

import com.ethiorental.backend.IAM.adapter.FaydaAdapterService;
import com.ethiorental.backend.IAM.dto.FaydaCitizenResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fayda")
@RequiredArgsConstructor
public class FaydaController {

    private final FaydaAdapterService faydaAdapterService;

    @GetMapping("/verify/{faydaId}")
    public ResponseEntity<FaydaCitizenResponseDto> verifyFaydaId(@PathVariable Long faydaId) {
        FaydaCitizenResponseDto response = faydaAdapterService.fetchCitizenIdentity(faydaId);
        return ResponseEntity.ok(response);
    }
}
