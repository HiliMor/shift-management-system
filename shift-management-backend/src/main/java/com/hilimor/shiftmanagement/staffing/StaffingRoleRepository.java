package com.hilimor.shiftmanagement.staffing;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffingRoleRepository extends JpaRepository<StaffingRole, Long> {

    List<StaffingRole> findByTeam_IdOrderByName(Long teamId);

    Optional<StaffingRole> findByTeam_IdAndName(Long teamId, String name);

    boolean existsByTeam_IdAndName(Long teamId, String name);
}
