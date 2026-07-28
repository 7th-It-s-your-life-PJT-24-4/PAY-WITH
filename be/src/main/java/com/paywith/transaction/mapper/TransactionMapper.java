package com.paywith.transaction.mapper;

import com.paywith.transaction.domain.Transaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionMapper {
    void insertTransaction(Transaction transaction);

}
