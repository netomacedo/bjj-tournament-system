package com.bjj.tournament.service;

import com.bjj.tournament.dto.CreateOrganizationDTO;
import com.bjj.tournament.dto.OrganizationCreatedDTO;
import com.bjj.tournament.entity.Organization;
import com.bjj.tournament.entity.User;
import com.bjj.tournament.repository.OrganizationRepository;
import com.bjj.tournament.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for platform-admin-only operations: onboarding brand-new
 * organizations. Ordinary org admins never touch this - their own
 * organization is fixed, derived from OrganizationAccessGuard everywhere
 * else in the app.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a new organization and its first org-admin user together.
     */
    @Transactional
    public OrganizationCreatedDTO createOrganizationWithAdmin(CreateOrganizationDTO dto) {
        log.info("Platform admin creating new organization: {}", dto.getOrganizationName());

        if (userRepository.existsByUsername(dto.getAdminUsername())) {
            throw new IllegalArgumentException("Username '" + dto.getAdminUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(dto.getAdminEmail())) {
            throw new IllegalArgumentException("Email '" + dto.getAdminEmail() + "' is already registered");
        }

        Organization organization = new Organization();
        organization.setName(dto.getOrganizationName());
        Organization savedOrganization = organizationRepository.save(organization);

        User admin = new User();
        admin.setOrganizationId(savedOrganization.getId());
        admin.setUsername(dto.getAdminUsername());
        admin.setEmail(dto.getAdminEmail());
        admin.setPassword(passwordEncoder.encode(dto.getAdminPassword()));
        admin.setFullName(dto.getAdminFullName());
        admin.setRole("ROLE_ADMIN");
        admin.setEnabled(true);
        admin.setAccountNonLocked(true);
        User savedAdmin = userRepository.save(admin);

        log.info("Created organization ID {} with admin user ID {}", savedOrganization.getId(), savedAdmin.getId());

        return new OrganizationCreatedDTO(
            savedOrganization.getId(),
            savedOrganization.getName(),
            savedAdmin.getId(),
            savedAdmin.getUsername()
        );
    }
}
