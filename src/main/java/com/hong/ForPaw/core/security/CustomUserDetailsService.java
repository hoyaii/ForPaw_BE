package com.hong.ForPaw.core.security;

import com.hong.ForPaw.domain.User.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> userOP = userRepository.findByEmail(email);

        if (userOP.isEmpty()) {
            return null;
        } else {
            User userPS = userOP.get();
            return new CustomUserDetails(userPS);
        }
    }
}
