package com.paywith.user.service;

import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.dto.UserCreateRequest;
import com.paywith.user.dto.UserResponse;
import com.paywith.user.dto.UserUpdateRequest;
import com.paywith.exception.BusinessException;
import com.paywith.user.mapper.UserMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Set<String> VALID_GENDER_CODES = Set.of("1", "2", "3", "4");

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
        if (userMapper.findByPhone(request.getPhone()) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 사용 중인 전화번호입니다.");
        }

        if (!VALID_GENDER_CODES.contains(request.getGender())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "gender는 1/2/3/4 중 하나여야 합니다.");
        }

        User user = new User();
        user.setRole(parseRole(request.getRole()));
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setBirthDate(parseBirthDate(request.getBirthDate(), request.getGender()));
        user.setGender(request.getGender());
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

    private Role parseRole(String role) {
        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "role은 SENIOR 또는 GUARD여야 합니다.");
        }
    }

    private LocalDate parseBirthDate(String birthDate6, String genderCode) {
        String century;
        switch (genderCode) {
            case "3":
            case "4":
                century = "20";
                break;
            default:
                century = "19";
                break;
        }

        try {
            return LocalDate.parse(
                century + birthDate6.substring(0, 2) + "-" + birthDate6.substring(2, 4) + "-" + birthDate6.substring(4, 6)
            );
        } catch (DateTimeParseException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "생년월일이 올바르지 않습니다.");
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
