package com.bjj.tournament.security;

import com.bjj.tournament.entity.User;
import com.bjj.tournament.exception.OrganizationAccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Central multi-tenancy check: every by-ID lookup across the services calls
 * checkAccess() right after loading the entity, to confirm the caller
 * actually belongs to the organization that resource belongs to.
 *
 * JwtAuthenticationFilter reloads the full User from the database on every
 * request (not just trusting JWT claims), so the User set as the Spring
 * Security principal always carries a live, current organizationId - no
 * separate lookup or JWT claim is needed here.
 */
@Component
public class OrganizationAccessGuard {

    /**
     * The current authenticated user's organization, or null for a platform
     * admin (who belongs to no single organization).
     */
    public Long currentOrganizationId() {
        return currentUser().getOrganizationId();
    }

    public boolean isPlatformAdmin() {
        return "ROLE_PLATFORM_ADMIN".equals(currentUser().getRole());
    }

    /**
     * Throws unless the given resource's organization matches the caller's
     * own, or the caller is a platform admin (who can access everything).
     */
    public void checkAccess(Long resourceOrganizationId) {
        if (isPlatformAdmin()) {
            return;
        }

        Long callerOrganizationId = currentOrganizationId();
        if (callerOrganizationId == null || !Objects.equals(callerOrganizationId, resourceOrganizationId)) {
            throw new OrganizationAccessDeniedException("Resource not found");
        }
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
