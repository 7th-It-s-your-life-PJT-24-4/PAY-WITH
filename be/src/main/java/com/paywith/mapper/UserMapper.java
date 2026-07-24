package com.paywith.mapper;

import com.paywith.domain.User;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    List<User> findAll();

    User findById(@Param("id") Long id);

    User findByPhone(@Param("phone") String phone);

    int insert(User user);

    int update(User user);

    int delete(@Param("id") Long id);
}
