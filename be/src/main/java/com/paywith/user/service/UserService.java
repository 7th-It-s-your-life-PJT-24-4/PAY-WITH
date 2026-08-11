package com.paywith.user.service;

import com.paywith.auth.service.PhoneVerificationService;
import com.paywith.common.PhoneNumberNormalizer;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.domain.UserStatus;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

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

    /**
     * 기기 토큰은 재발급될 때마다 덮어쓴다. 한 사용자에 한 토큰만 두므로 다른 기기에서
     * 로그인하면 이전 기기로는 더 이상 발송되지 않는다.
     *
     * <p>저장 전에 같은 토큰을 쓰던 다른 사용자를 먼저 정리한다. 토큰은 사용자가 아니라 앱
     * 인스턴스를 가리켜서, 앞선 사용자가 로그아웃하지 않고 떠난 브라우저에서 다음 사용자가
     * 로그인하면 같은 토큰이 그대로 나온다. 두 행에 같은 토큰이 남으면 앞선 사용자의 거래
     * 알림이 지금 기기를 쓰는 사람에게 간다. 두 UPDATE 는 한 트랜잭션이어야 한다.
     */
    @Transactional
    public void updateFcmToken(Long id, String fcmToken) {
        requireNotWithdrawn(findUser(id));
        int released = userMapper.clearFcmTokenFromOtherUsers(id, fcmToken);
        if (released > 0) {
            // 기기를 공유했다는 뜻이라 흔한 일은 아니다. 남겨두면 알림 오배송 문의가 들어왔을 때
            // 경로를 되짚을 수 있다.
            log.info("같은 토큰을 쓰던 다른 계정 {}건의 등록을 해제했다. userId={}", released, id);
        }
        userMapper.updateFcmToken(id, fcmToken);
    }

    /**
     * 로그아웃 시 호출해 발송 대상에서 제외한다.
     *
     * <p>해제할 토큰을 함께 받아 현재 저장값과 같을 때만 지운다. 사용자 ID 만으로 지우면, 다른
     * 기기로 로그인해 토큰이 교체된 뒤 옛 기기의 로그아웃이 뒤늦게 도착했을 때 지금 쓰는 기기의
     * 토큰까지 지워져 알림이 조용히 끊긴다.
     */
    @Transactional
    public void deleteFcmToken(Long id, String fcmToken) {
        findUser(id);
        if (userMapper.clearFcmTokenIfMatches(id, fcmToken) == 0) {
            // 이미 다른 토큰으로 교체된 뒤 도착한 요청. 해제하려던 토큰은 이미 대상이 아니므로
            // 실패가 아니다.
            log.info("현재 등록된 토큰과 달라 해제하지 않았다. userId={}", id);
        }
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

    /**
     * 탈퇴 시 토큰을 지우지만, 그 전에 발급된 JWT 는 만료까지 살아 있어 다시 등록할 수 있다.
     * 등록되면 탈퇴한 사람이 남의 금융 알림을 계속 받게 되므로 여기서도 막는다.
     */
    private void requireNotWithdrawn(User user) {
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "탈퇴한 계정입니다.");
        }
    }
}
