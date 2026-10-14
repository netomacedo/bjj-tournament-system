package com.bjj.tournament.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for a platform admin creating a brand-new organization together with
 * its first org-admin user, in one request - the one legitimate case where
 * the target organization isn't derived from the caller, since a platform
 * admin has no organization of their own to hand down.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrganizationDTO {

    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 200, message = "Organization name must be between 2 and 200 characters")
    private String organizationName;

    @NotBlank(message = "Admin username is required")
    @Size(min = 3, max = 50, message = "Admin username must be between 3 and 50 characters")
    private String adminUsername;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be valid")
    private String adminEmail;

    @NotBlank(message = "Admin password is required")
    @Size(min = 6, max = 100, message = "Admin password must be between 6 and 100 characters")
    private String adminPassword;

    @NotBlank(message = "Admin full name is required")
    @Size(min = 2, max = 100, message = "Admin full name must be between 2 and 100 characters")
    private String adminFullName;
}
