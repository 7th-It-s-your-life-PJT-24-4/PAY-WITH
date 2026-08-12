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
     * 같은 토큰을 쓰던 다른 사용자의 등록을 해제한다. 토큰은 앱 인스턴스 단위라 소유자가 한 명
     * 이어야 하는데, 앞선 사용자가 로그아웃하지 않고 떠나면 그 행에 토큰이 남는다. 정리하지
     * 않으면 그 사용자의 거래 알림이 지금 기기를 쓰는 사람에게 간다.
     */
    int clearFcmTokenFromOtherUsers(@Param("id") Long id, @Param("fcmToken") String fcmToken);

    /**
     * 요청한 토큰이 현재 저장값과 같을 때만 지운다. 사용자 ID 만으로 지우면, 다른 기기로
     * 로그인해 토큰이 교체된 뒤 옛 기기의 로그아웃이 도착했을 때 지금 쓰는 기기의 토큰까지
     * 지워진다. 0 건이면 이미 교체된 것이므로 실패가 아니다.
     */
    int clearFcmTokenIfMatches(@Param("id") Long id, @Param("fcmToken") String fcmToken);

    /**
     * FCM 이 영구 실패로 응답한 토큰을 지운다. 사용자 ID 가 아니라 토큰 값으로 지우는 이유는,
     * 발송 결과에는 어느 사용자의 것인지가 아니라 토큰만 담겨 오기 때문이다. 기기를 옮겨
     * 같은 토큰이 여러 행에 남아 있다면 함께 지워지는데, 어차피 죽은 토큰이라 문제되지 않는다.
     */
    int clearFcmTokens(@Param("tokens") List<String> tokens);

    int delete(@Param("id") Long id);
}
