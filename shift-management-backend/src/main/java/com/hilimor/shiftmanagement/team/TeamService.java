package com.hilimor.shiftmanagement.team;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {

    private final TeamManagerRepository teamManagerRepository;

    public TeamService(TeamManagerRepository teamManagerRepository) {
        this.teamManagerRepository = teamManagerRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listManagedTeams(String username) {
        return teamManagerRepository.findByManager_Username(username)
                .stream()
                .map(TeamManager::getTeam)
                .sorted(Comparator.comparing(Team::getName))
                .map(TeamResponse::from)
                .toList();
    }
}
