package br.com.luizbrand.worklog.auth;

import br.com.luizbrand.worklog.auth.dto.AuthResponse;
import br.com.luizbrand.worklog.auth.dto.LoginRequest;
import br.com.luizbrand.worklog.auth.dto.LoginResponse;
import br.com.luizbrand.worklog.auth.dto.RegisterRequest;
import br.com.luizbrand.worklog.role.Role;
import br.com.luizbrand.worklog.role.enums.RoleName;
import br.com.luizbrand.worklog.user.User;
import br.com.luizbrand.worklog.exception.Conflict.EmailAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.RoleNotFoundException;
import br.com.luizbrand.worklog.user.UserMapper;
import br.com.luizbrand.worklog.role.RoleRepository;
import br.com.luizbrand.worklog.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, UserMapper userMapper, RoleRepository roleRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

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
        return userMapper.toAuthResponse(savedUser);

    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        UsernamePasswordAuthenticationToken userAndPass =
                new UsernamePasswordAuthenticationToken(request.email(), request.password());

        var auth = authenticationManager.authenticate(userAndPass);
        User user = (User) auth.getPrincipal();
        String acessToken = jwtService.generateAcessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new LoginResponse(acessToken, refreshToken);
    }

}
