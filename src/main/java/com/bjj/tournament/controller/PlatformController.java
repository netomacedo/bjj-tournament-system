package com.bjj.tournament.controller;

import com.bjj.tournament.dto.CreateOrganizationDTO;
import com.bjj.tournament.dto.OrganizationCreatedDTO;
import com.bjj.tournament.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for platform-admin-only operations. Gated by
 * SecurityConfig's hasRole("PLATFORM_ADMIN") rule on /api/platform/** -
 * ordinary org admins can never reach any endpoint here.
 */
@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
@Slf4j
public class PlatformController {

    private final OrganizationService organizationService;

    /**
     * Create a brand-new organization and its first org-admin together
     * POST /api/platform/organizations
     */
    @PostMapping("/organizations")
    public ResponseEntity<OrganizationCreatedDTO> createOrganization(@Valid @RequestBody CreateOrganizationDTO dto) {
        log.info("REST request to create organization: {}", dto.getOrganizationName());
        OrganizationCreatedDTO created = organizationService.createOrganizationWithAdmin(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
