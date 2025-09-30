package com.Abhinav.backend.features.authentication.service;

import com.Abhinav.backend.features.authentication.repository.AuthenticationUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final AuthenticationUserRepository repository;
    public UserDetailsServiceImpl(AuthenticationUserRepository repository) { this.repository = repository; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}