package com.hilimor.shiftmanagement.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_role", nullable = false, length = 30)
    private ApplicationRole applicationRole;

    @Version
    @Column(nullable = false)
    private Long version;

    protected User() {
    }

    public User(String username, String passwordHash, String fullName, String email, ApplicationRole applicationRole) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.applicationRole = applicationRole;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public ApplicationRole getApplicationRole() {
        return applicationRole;
    }

    public Long getVersion() {
        return version;
    }
}
