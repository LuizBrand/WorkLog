package br.com.luizbrand.worklog.service;

import br.com.luizbrand.worklog.dto.response.UserResponse;
import br.com.luizbrand.worklog.entity.User;
import br.com.luizbrand.worklog.exception.UserNotFoundException;
import br.com.luizbrand.worklog.mapper.UserMapper;
import br.com.luizbrand.worklog.repository.UserRepository;
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
        user.setUserEnabled(false);
        userRepository.save(user);

    }
}
