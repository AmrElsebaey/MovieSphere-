package com.fawry.moviesphere.auth;

import com.fawry.moviesphere.user.User;
import com.fawry.moviesphere.user.UserRepository;
import com.fawry.moviesphere.exception.ResourceAlreadyExistsException;
import com.fawry.moviesphere.exception.ResourceNotFoundException;
import com.fawry.moviesphere.role.RoleRepository;
import com.fawry.moviesphere.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public void createUser(RegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already exists.");
        }
        var userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("User role was not initiated."));


        User user = User.builder().
                firstName(request.getFirstName()).
                lastName(request.getLastName()).
                email(request.getEmail()).
                password(passwordEncoder.encode(request.getPassword())).
                roles(List.of(userRole)).
                build();

        userRepository.save(user);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            var claims = new HashMap<String, Object>();
            var user = ((User) auth.getPrincipal());
            claims.put("fullName", user.fullName());
            String token = jwtService.generateToken(claims, user);

            return AuthenticationResponse.builder()
                    .token(token).build();
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid email or password.");
        }
    }
}
