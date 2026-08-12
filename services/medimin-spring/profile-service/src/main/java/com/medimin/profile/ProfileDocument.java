package com.medimin.profile;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("profiles")
public record ProfileDocument(
        @Id Long id,
        String firstName,
        String lastName,
        String email,
        String dateOfBirth,
        String bloodType,
        List<String> allergies,
        List<String> conditions,
        int medicationCount
) {}