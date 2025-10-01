package com.Abhinav.backend.features.authentication.controller;


import com.Abhinav.backend.features.authentication.dto.UserProfileDto;
import com.Abhinav.backend.features.authentication.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;


    @GetMapping("/{username}/profile")
    public ResponseEntity<UserProfileDto> getUserProfile(@PathVariable String username) {
        UserProfileDto userProfile = profileService.getUserProfile(username);
        return ResponseEntity.ok(userProfile);
    }
}