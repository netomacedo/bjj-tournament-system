package com.bjj.tournament.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response after a platform admin creates a new organization and its first
 * admin - deliberately not the raw User entity, so the password hash is
 * never serialized.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationCreatedDTO {
    private Long organizationId;
    private String organizationName;
    private Long adminUserId;
    private String adminUsername;
}
