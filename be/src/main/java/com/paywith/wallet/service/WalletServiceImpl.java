package com.paywith.wallet.service;

import com.paywith.exception.BusinessException;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.dto.WalletBalanceResponse;
import com.paywith.wallet.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;
    private final UserMapper userMapper;

    /**
     * 조회 전용이라 {@code readOnly} 로 둔다. 잔액은 충전·송금·결제가 각자의 트랜잭션에서 바꾸므로
     * 여기서는 그 시점의 확정 값을 읽기만 한다.
     *
     * <p>지갑은 WARD 에게만 생성되므로(회원가입 시점), 보호자 계정은 애초에 조회할 지갑이 없다.
     * 그래도 역할을 먼저 확인해 404 대신 403 을 준다. "지갑이 없다"는 응답은 보호자에게
     * 원인을 알려주지 못하고, 지갑 생성이 누락된 장애 상황과도 구분되지 않는다.
     */
    @Override
    @Transactional(readOnly = true)
    public WalletBalanceResponse findMyBalance(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        if (user.getRole() != Role.WARD) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "지갑은 피보호자만 이용할 수 있습니다.");
        }

        Wallet wallet = walletMapper.findWalletByUserId(userId);
        if (wallet == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "지갑을 찾을 수 없습니다.");
        }

        return new WalletBalanceResponse(wallet);
    }
}
