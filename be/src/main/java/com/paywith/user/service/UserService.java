package com.paywith.user.service;

import com.paywith.auth.service.PhoneVerificationService;
import com.paywith.common.PhoneNumberNormalizer;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.dto.UserCreateRequest;
import com.paywith.user.dto.UserResponse;
import com.paywith.user.dto.UserUpdateRequest;
import com.paywith.exception.BusinessException;
import com.paywith.user.mapper.UserMapper;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.mapper.WalletMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final WalletMapper walletMapper;
    private final PasswordEncoder passwordEncoder;
    private final PhoneVerificationService phoneVerificationService;

    public UserService(
        UserMapper userMapper,
        WalletMapper walletMapper,
        PasswordEncoder passwordEncoder,
        PhoneVerificationService phoneVerificationService
    ) {
        this.userMapper = userMapper;
        this.walletMapper = walletMapper;
        this.passwordEncoder = passwordEncoder;
        this.phoneVerificationService = phoneVerificationService;
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
        String phone = PhoneNumberNormalizer.normalize(request.getPhone());
        phoneVerificationService.requireValidToken(request.getVerificationToken(), phone);

        if (userMapper.findByPhone(phone) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 사용 중인 전화번호입니다.");
        }

        User user = new User();
        user.setRole(parseRole(request.getRole()));
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setBirthDate(parseBirthDate(request.getBirthDate()));
        user.setGender(request.getGender());
        user.setAvatarId(request.getAvatarId() != null ? request.getAvatarId() : 1);
        user.setPin(passwordEncoder.encode(request.getPaymentPassword()));
        userMapper.insert(user);

        if (user.getRole() == Role.WARD) {
            Wallet wallet = new Wallet();
            wallet.setUserId(user.getId());
            walletMapper.insert(wallet);
        }

        phoneVerificationService.invalidateToken(request.getVerificationToken(), phone);

        return new UserResponse(findUser(user.getId()));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findUser(id);
        user.setName(request.getName());
        if (request.getAvatarId() != null) {
            user.setAvatarId(request.getAvatarId());
        }
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
            throw new BusinessException(HttpStatus.BAD_REQUEST, "role은 WARD 또는 GUARD여야 합니다.");
        }
    }

    private LocalDate parseBirthDate(String birthDate8) {
        try {
            return LocalDate.parse(
                birthDate8.substring(0, 4) + "-" + birthDate8.substring(4, 6) + "-" + birthDate8.substring(6, 8)
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
