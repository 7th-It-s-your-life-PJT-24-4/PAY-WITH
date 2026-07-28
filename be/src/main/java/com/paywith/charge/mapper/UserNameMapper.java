package com.paywith.charge.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserNameMapper {
    String findUserName(Long userId);
}
