package com.hilimor.shiftmanagement.staffing;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffingRoleRepository extends JpaRepository<StaffingRole, Long> {

    List<StaffingRole> findByTeam_IdOrderByName(Long teamId);

    boolean existsByTeam_IdAndName(Long teamId, String name);
}
