package com.bjj.tournament.repository;

import com.bjj.tournament.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Organization entity
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}
