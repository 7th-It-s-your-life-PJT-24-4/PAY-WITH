package com.paywith.user.mapper;

import com.paywith.user.domain.User;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface UserMapper {

    List<User> findAll();

    User findById(@Param("id") Long id);

    User findByPhone(@Param("phone") String phone);

    int insert(User user);

    int update(User user);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /** 로그아웃·토큰 해제 시 null 을 넣어 지운다 */
    int updateFcmToken(@Param("id") Long id, @Param("fcmToken") String fcmToken);

    int delete(@Param("id") Long id);
}
