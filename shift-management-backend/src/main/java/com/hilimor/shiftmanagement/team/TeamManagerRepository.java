package com.hilimor.shiftmanagement.team;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamManagerRepository extends JpaRepository<TeamManager, Long> {

    boolean existsByManager_IdAndTeam_Id(Long managerId, Long teamId);

    boolean existsByManager_UsernameAndTeam_Id(String username, Long teamId);

    List<TeamManager> findByManager_Id(Long managerId);
}
