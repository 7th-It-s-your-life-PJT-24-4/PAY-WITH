package com.paywith.account.mapper;

import com.paywith.account.domain.Account;
import com.paywith.account.dto.AccountResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface AccountMapper {
    void insertAccount(Account account);

    List<AccountResponse> findAccountsByUserId(Long userId);

    AccountResponse findAccountById(Long accountId);

    boolean existsByUserIdAndAccount(
            @Param("userId") Long userId,
            @Param("bankCode") String bankCode,
            @Param("accountNo") String accountNo
    );

    boolean existsByUserIdAndAccountId(@Param("userId") Long userId, @Param("accountId") Long accountId);
    }
