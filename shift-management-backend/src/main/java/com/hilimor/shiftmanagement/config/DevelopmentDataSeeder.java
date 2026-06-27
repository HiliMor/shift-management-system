package com.hilimor.shiftmanagement.config;

import java.time.Instant;

import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManager;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamMember;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;
import com.hilimor.shiftmanagement.team.TeamRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DevelopmentDataSeeder {

    @Bean
    CommandLineRunner seedInitialData(
            UserRepository userRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            TeamManagerRepository teamManagerRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            User manager = userRepository.save(new User(
                    "manager1",
                    passwordEncoder.encode("password"),
                    "Demo Manager",
                    "manager1@example.com",
                    ApplicationRole.MANAGER
            ));

            User employeeOne = userRepository.save(new User(
                    "employee1",
                    passwordEncoder.encode("password"),
                    "Demo Employee One",
                    "employee1@example.com",
                    ApplicationRole.EMPLOYEE
            ));

            User employeeTwo = userRepository.save(new User(
                    "employee2",
                    passwordEncoder.encode("password"),
                    "Demo Employee Two",
                    "employee2@example.com",
                    ApplicationRole.EMPLOYEE
            ));

            Team operationsTeam = teamRepository.save(new Team(
                    "Operations",
                    SwapApprovalPolicy.MANAGER,
                    8,
                    "Asia/Jerusalem"
            ));

            Instant joinedAt = Instant.now();
            teamMemberRepository.save(new TeamMember(employeeOne, operationsTeam, joinedAt, true));
            teamMemberRepository.save(new TeamMember(employeeTwo, operationsTeam, joinedAt, true));
            teamManagerRepository.save(new TeamManager(manager, operationsTeam));
        };
    }
}
