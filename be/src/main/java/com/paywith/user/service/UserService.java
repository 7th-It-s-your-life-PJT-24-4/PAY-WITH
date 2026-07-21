package com.paywith.user.service;

import com.paywith.exception.BusinessException;
import com.paywith.user.domain.User;
import com.paywith.user.dto.UserCreateRequest;
import com.paywith.user.dto.UserResponse;
import com.paywith.user.dto.UserUpdateRequest;
import com.paywith.user.mapper.UserMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> findAll() {
        return userMapper.findAll().stream()
            .map(UserResponse::new)
            .collect(Collectors.toList());
    }

    public UserResponse findById(Long id) {
        return new UserResponse(findUser(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userMapper.findByEmail(request.getEmail()) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        userMapper.insert(user);
        return new UserResponse(findUser(user.getId()));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findUser(id);
        user.setName(request.getName());
        userMapper.update(user);
        return new UserResponse(findUser(id));
    }

    @Transactional
    public void delete(Long id) {
        int deleted = userMapper.delete(id);
        if (deleted == 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
    }

    private User findUser(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return user;
    }
}
