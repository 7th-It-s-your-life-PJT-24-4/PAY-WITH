package com.paywith.user.mapper;

import com.paywith.user.domain.User;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    List<User> findAll();

    User findById(@Param("id") Long id);

    User findByEmail(@Param("email") String email);

    int insert(User user);

    int update(User user);

    int delete(@Param("id") Long id);
}
