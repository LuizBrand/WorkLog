package br.com.luizbrand.worklog.user;

import br.com.luizbrand.worklog.auth.refreshtoken.RefreshToken;
import br.com.luizbrand.worklog.auth.refreshtoken.RefreshTokenService;
import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.exception.NotFound.UserNotFoundException;
import br.com.luizbrand.worklog.user.dto.ChangePasswordRequest;
import br.com.luizbrand.worklog.user.dto.UserResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       @Lazy PasswordEncoder passwordEncoder,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toUserResponse)
                .toList();

    }

    public UserResponse findByPublicId(UUID publicId) {

        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + publicId + " not found"));
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public void deactiveUser(UUID publicId) {
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + publicId + " not found"));
        user.setIsEnabled(false);
        userRepository.save(user);

    }

    public UserResponse getMe(User currentUser) {
        return userMapper.toUserResponse(currentUser);
    }

    @Transactional
    public void changeMyPassword(User currentUser, ChangePasswordRequest request) {

        if (!passwordEncoder.matches(request.currentPassword(), currentUser.getPassword())) {
            throw new BusinessException("Senha atual incorreta");
        }

        RefreshToken stored = refreshTokenService.findByToken(request.refreshToken())
                .filter(token -> token.getUserEmail().equals(currentUser.getEmail()))
                .orElseThrow(() -> new BusinessException("Refresh token inválido para o usuário"));

        currentUser.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(currentUser);
        refreshTokenService.deleteByToken(stored.getId());
    }

    public User findEntityByPublicId(UUID publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new UserNotFoundException("User with public ID: " + publicId + " not found"));
    }

    public User findActiveUser(UUID publicId) {
        User user = this.findEntityByPublicId(publicId);
        if(!user.getIsEnabled()) {
            throw new BusinessException("User is not active");
        }
        return user;
    }

    public Optional<User> findUserByEmail(String userEmail) {
        return userRepository.findByEmail(userEmail);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

}
