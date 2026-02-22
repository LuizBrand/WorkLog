package br.com.luizbrand.worklog.auth;

import br.com.luizbrand.worklog.auth.dto.RegisterResponse;
import br.com.luizbrand.worklog.auth.dto.LoginRequest;
import br.com.luizbrand.worklog.auth.dto.AuthenticationResponse;
import br.com.luizbrand.worklog.auth.dto.RegisterRequest;
import br.com.luizbrand.worklog.auth.refreshtoken.RefreshToken;
import br.com.luizbrand.worklog.auth.refreshtoken.RefreshTokenService;
import br.com.luizbrand.worklog.exception.Business.RefreshTokenException;
import br.com.luizbrand.worklog.role.Role;
import br.com.luizbrand.worklog.role.enums.RoleName;
import br.com.luizbrand.worklog.user.User;
import br.com.luizbrand.worklog.exception.Conflict.EmailAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.RoleNotFoundException;
import br.com.luizbrand.worklog.user.UserMapper;
import br.com.luizbrand.worklog.role.RoleRepository;
import br.com.luizbrand.worklog.user.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, UserMapper userMapper, RoleRepository roleRepository, JwtService jwtService, RefreshTokenService refreshTokenService, UserDetailsService userDetailsService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        Optional<User> userEmail = userService.findUserByEmail(request.email());
        if(userEmail.isPresent()) {
            throw new EmailAlreadyExistsException("Email '" + request.email() + "' já está em uso.");
        }

        Role userRole = roleRepository.findRoleByName(RoleName.USER)
                .orElseThrow(() -> new RoleNotFoundException("Error: Default role not found"));

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(userRole));

        User savedUser = userService.saveUser(user);
        return userMapper.toAuthResponse(savedUser);

    }

    @Transactional
    public AuthenticationResponse login(LoginRequest request) {
        UsernamePasswordAuthenticationToken userAndPass =
                new UsernamePasswordAuthenticationToken(request.email(), request.password());

        var auth = authenticationManager.authenticate(userAndPass);
        User user = (User) auth.getPrincipal();
        String acessToken = jwtService.generateAcessToken(user);
        RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user.getEmail());

        return new AuthenticationResponse(acessToken, refreshToken.getId());
    }

    public AuthenticationResponse refreshToken(String requestRefreshToken) {
        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshToken -> {
                    UserDetails user = userDetailsService.loadUserByUsername(refreshToken.getUserEmail());

                    String newAcessToken = jwtService.generateAcessToken(user);
                    refreshTokenService.deleteByToken(requestRefreshToken);
                    RefreshToken newRefreshToken = refreshTokenService.generateRefreshToken(user.getUsername());

                    return new AuthenticationResponse(newAcessToken, newRefreshToken.getId());
                })
                .orElseThrow(() -> new RefreshTokenException("Invalid or expired session. Please log in again."));

    }

    public void logout(String refreshToken) {
        refreshTokenService.deleteByToken(refreshToken);
    }
}
