package com.example.transcription.service;

import com.example.transcription.domain.Role;

public record CurrentUser(Long id, String name, Role role) {
    public void require(Role r) {
        if (role != r) throw new Errors.Forbidden("Requires role " + r);
    }
}
