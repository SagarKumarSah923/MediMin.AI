package com.medimin.profile;

import com.medimin.common.ApiModels;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal")
public class ProfileController {
    private final ProfileRepository profiles;

    public ProfileController(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    @PostConstruct
    void seedDemoProfile() {
        try {
            if (profiles.count() == 0) {
                profiles.save(demoDocument());
            }
        } catch (RuntimeException ignored) {
            // Atlas connectivity is reported by the API health path; keep demo mode available.
        }
    }

    @GetMapping("/profile")
    public ApiModels.Profile getProfile() {
        ProfileDocument profile;
        try {
            profile = profiles.findById(1L).orElseGet(this::demoDocument);
        } catch (RuntimeException ignored) {
            profile = demoDocument();
        }
        return new ApiModels.Profile(
                profile.id(), profile.firstName(), profile.lastName(), profile.email(),
                profile.dateOfBirth(), profile.bloodType(), profile.allergies(),
                profile.conditions(), profile.medicationCount()
        );
    }

    private ProfileDocument demoDocument() {
        return new ProfileDocument(
                1L, "Maya", "Chen", "maya.chen@example.com", "1992-04-18",
                "O+", List.of("Pollen"), List.of("Seasonal allergies"), 2
        );
    }
}