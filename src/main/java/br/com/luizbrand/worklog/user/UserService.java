package br.com.luizbrand.worklog.user;

import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.exception.NotFound.UserNotFoundException;
import br.com.luizbrand.worklog.user.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
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

    public User findEntityByPublicId(UUID publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new UserNotFoundException("System with public ID: " + publicId + " not found"));
    }

    public User findActiveUser(UUID publicId) {
        User user = this.findEntityByPublicId(publicId);
        if(!user.getIsEnabled()) {
            throw new BusinessException("User is not active");
        }
        return user;
    }
}
