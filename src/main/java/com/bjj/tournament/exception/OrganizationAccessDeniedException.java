package com.bjj.tournament.exception;

/**
 * Thrown when the current user tries to access a resource belonging to a
 * different organization. Mapped to 404, not 403 - a cross-tenant caller
 * shouldn't be able to tell the resource exists at all.
 */
public class OrganizationAccessDeniedException extends RuntimeException {
    public OrganizationAccessDeniedException(String message) {
        super(message);
    }
}
