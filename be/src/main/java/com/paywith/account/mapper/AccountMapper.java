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
    boolean existsByUserIdAndAccount(
            @Param("userId") Long userId,
            @Param("bankCode") String bankCode,
            @Param("accountNo") String accountNo
    );}
