package com.hilimor.shiftmanagement.staffing;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberStaffingRoleRepository extends JpaRepository<TeamMemberStaffingRole, Long> {

    List<TeamMemberStaffingRole> findByTeamMember_Id(Long teamMemberId);

    boolean existsByTeamMember_IdAndStaffingRole_Id(Long teamMemberId, Long staffingRoleId);

    boolean existsByTeamMember_User_IdAndTeamMember_Team_IdAndStaffingRole_Id(
            Long userId,
            Long teamId,
            Long staffingRoleId
    );
}
