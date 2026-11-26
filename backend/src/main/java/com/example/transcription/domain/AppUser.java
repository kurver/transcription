package com.example.transcription.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    public Long id;
    public String name;
    @Enumerated(EnumType.STRING)
    public Role role;
}
