package com.ethiorental.backend.IAM.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    private String firstName;
    private String middleName;
    private String lastName;
    private String phone;
    private String city;
    private String subCity;
    private String woreda;
    private String houseNumber;
    private String worksOn;
    private String tinNumber;
    private String emergencyContactName;
    private String emergencyContactPhone;
}
