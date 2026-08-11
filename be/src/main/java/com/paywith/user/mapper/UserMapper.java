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

    /**
     * FCM 이 영구 실패로 응답한 토큰을 지운다. 사용자 ID 가 아니라 토큰 값으로 지우는 이유는,
     * 발송 결과에는 어느 사용자의 것인지가 아니라 토큰만 담겨 오기 때문이다. 기기를 옮겨
     * 같은 토큰이 여러 행에 남아 있다면 함께 지워지는데, 어차피 죽은 토큰이라 문제되지 않는다.
     */
    int clearFcmTokens(@Param("tokens") List<String> tokens);

    int delete(@Param("id") Long id);
}
