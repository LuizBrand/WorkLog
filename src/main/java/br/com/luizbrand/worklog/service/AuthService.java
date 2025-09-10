package br.com.luizbrand.worklog.service;

import br.com.luizbrand.worklog.dto.request.RegisterRequest;
import br.com.luizbrand.worklog.dto.response.RegisterResponse;
import br.com.luizbrand.worklog.entity.Role;
import br.com.luizbrand.worklog.entity.User;
import br.com.luizbrand.worklog.enums.RoleName;
import br.com.luizbrand.worklog.exception.EmailAlreadyExistsException;
import br.com.luizbrand.worklog.exception.RoleNotFoundException;
import br.com.luizbrand.worklog.mapper.UserMapper;
import br.com.luizbrand.worklog.repository.RoleRepository;
import br.com.luizbrand.worklog.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, UserMapper userMapper, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        Optional<User> userEmail = userRepository.findByEmail(request.email());
        if(userEmail.isPresent()) {
            throw new EmailAlreadyExistsException("Email '" + request.email() + "' já está em uso.");
        }

        Role userRole = roleRepository.findRoleByName(RoleName.USER)
                .orElseThrow(() -> new RoleNotFoundException("Error: Default role not found"));

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(userRole));

        User savedUser = userRepository.save(user);
        return userMapper.toRegisterResponse(savedUser);

    }

}
