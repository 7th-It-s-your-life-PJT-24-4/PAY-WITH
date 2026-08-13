package com.paywith.notification.service;

import com.paywith.fds.dto.FdsDecision;
import com.paywith.notification.domain.NotificationType;
import com.paywith.notification.domain.TransferNotificationInfo;
import com.paywith.notification.mapper.NotificationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 송금 관련 알림의 문구와 수신자 결정을 한곳에 모은다. 호출부(FDS 판정, 승인/거절, 승인 후
 * 송금 실행)가 세 군데로 흩어져 있어, 각자 조회하고 각자 문구를 만들면 같은 코드가 세 번
 * 생기고 표현도 서로 어긋난다.
 *
 * <p>모든 메서드가 예외를 삼킨다. 호출부가 전부 {@code @Transactional} 안이라 여기서 예외가
 * 새면 진행 중인 트랜잭션이 rollback-only 로 표시돼, 알림 때문에 송금이 뒤집힌다.
 */
@Component
public class TransferNotifier {

    private static final Logger log = LoggerFactory.getLogger(TransferNotifier.class);

    private static final String REF_TYPE_TRANSACTION = "TRANSACTION";
    /**
     * 승인 요청 알림만 참조 대상이 다르다. 보호자가 알림을 누르면 승인 화면으로 가야 하는데 그
     * 화면은 approvalId 로 열리고, 거래 ID 로는 열 수 없다.
     */
    private static final String REF_TYPE_APPROVAL = "APPROVAL";
    private static final String UNKNOWN_RECIPIENT = "수취인";

    private static final String TITLE_BLOCKED = "송금 차단 알림";
    private static final String BODY_BLOCKED = "위험 거래가 감지되어 %s님께 %,d원 송금을 차단했습니다.";
    private static final String TITLE_APPROVAL_REQUEST = "송금 승인 요청";
    private static final String BODY_APPROVAL_REQUEST =
        "%s님께 %,d원을 송금하려고 합니다. 확인해 주세요.";
    private static final String TITLE_CAUTION = "송금 주의 알림";
    // 이 알림은 송금이 실행되기 전(FDS 판정 시점)에 나간다. 뒤에서 잔액 부족 등으로 실패할 수
    // 있으므로 "완료됐다"고 단정하지 않는다 — 결제 쪽 문구와 달라 보이는 이유다.
    private static final String BODY_CAUTION = "주의가 필요한 송금이 감지되었습니다. %s님께 %,d원 송금이 요청되었습니다.";

    private static final String TITLE_REJECTED = "송금이 거절되었습니다";
    private static final String BODY_REJECTED = "보호자가 %s님께 %,d원 송금을 거절했습니다.";
    private static final String TITLE_COMPLETED = "송금이 완료되었습니다";
    private static final String BODY_COMPLETED = "%s님께 %,d원을 보냈습니다.";
    private static final String TITLE_FAILED = "송금이 실패했습니다";
    // 입금 호출을 통과한 뒤의 실패에 쓴다. "실패"라고 쓰지 않는 이유는 그 시점엔 이미 잔액이
    // 차감됐고 수취인에게 돈이 갔는지도 모르기 때문이다. 실패로 읽으면 다시 보내 이중 송금이 된다.
    private static final String TITLE_UNRESOLVED = "송금 결과를 확인해 주세요";
    private static final String BODY_UNRESOLVED =
        "%s님께 %,d원 송금이 정상 처리되지 않았습니다. 다시 보내지 마시고 고객센터로 문의해 주세요.";

    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;

    public TransferNotifier(NotificationMapper notificationMapper,
        NotificationService notificationService) {
        this.notificationMapper = notificationMapper;
        this.notificationService = notificationService;
    }

    /**
     * FDS 판정 결과를 보호자에게 알린다. SAFE 면 아무것도 하지 않는다.
     *
     * <p>보류(HELD)는 여기서 다루지 않는다. 승인 화면으로 보내려면 approvalId 가 필요해
     * {@link #notifyApprovalRequested} 로 분리했다.
     */
    public void notifyRiskDetected(Long transactionId, FdsDecision decision) {
        if (!decision.requiresGuardNotification()) {
            return;
        }
        TransferNotificationInfo info = findInfo(transactionId);
        if (info == null) {
            return;
        }

        String recipient = recipientNameOf(info);
        if (decision.isBlocked()) {
            notifyGuardians(info, NotificationType.ANOMALY, TITLE_BLOCKED,
                String.format(BODY_BLOCKED, recipient, info.getAmount()),
                REF_TYPE_TRANSACTION, transactionId);
        } else {
            notifyGuardians(info, NotificationType.ANOMALY, TITLE_CAUTION,
                String.format(BODY_CAUTION, recipient, info.getAmount()),
                REF_TYPE_TRANSACTION, transactionId);
        }
    }

    /**
     * 보류된 송금의 승인을 보호자에게 요청한다.
     *
     * <p>참조를 거래가 아니라 승인요청으로 싣는다. 알림을 누르면 승인 화면으로 가야 하는데 그
     * 화면은 approvalId 로 열리므로, 거래 ID 만 보내면 앱이 목적지를 찾지 못한다.
     */
    public void notifyApprovalRequested(Long transactionId, Long approvalId) {
        TransferNotificationInfo info = findInfo(transactionId);
        if (info == null) {
            return;
        }
        notifyGuardians(info, NotificationType.APPROVAL_REQUEST, TITLE_APPROVAL_REQUEST,
            String.format(BODY_APPROVAL_REQUEST, recipientNameOf(info), info.getAmount()),
            REF_TYPE_APPROVAL, approvalId);
    }

    /** 보호자가 거절했음을 피보호자에게 알린다. */
    public void notifyRejected(Long transactionId) {
        TransferNotificationInfo info = findInfo(transactionId);
        if (info == null) {
            return;
        }
        notifyWard(info, TITLE_REJECTED,
            String.format(BODY_REJECTED, recipientNameOf(info), info.getAmount()), transactionId);
    }

    /**
     * 승인 뒤 송금까지 끝났음을 피보호자에게 알린다.
     *
     * <p>"승인됨"과 "완료됨"을 따로 보내지 않는다. 피보호자에게 필요한 건 승인 여부가 아니라
     * 결과이고, 승인 직후에 알리면 뒤이은 송금이 실패했을 때 돈이 나간 것처럼 읽힌다.
     */
    public void notifyTransferCompleted(Long transactionId) {
        TransferNotificationInfo info = findInfo(transactionId);
        if (info == null) {
            return;
        }
        notifyWard(info, TITLE_COMPLETED,
            String.format(BODY_COMPLETED, recipientNameOf(info), info.getAmount()), transactionId);
    }

    /**
     * 돈이 나가기 전에 끝난 실패에만 쓴다(잔액 부족 등). 잔액이 그대로라 다시 시도해도 안전하다.
     *
     * @param reason 이미 사용자에게 보여줄 수 있는 문구여야 한다(내부 메시지 금지).
     */
    public void notifyTransferFailed(Long transactionId, String reason) {
        TransferNotificationInfo info = findInfo(transactionId);
        if (info == null) {
            return;
        }
        notifyWard(info, TITLE_FAILED, reason, transactionId);
    }

    /**
     * 입금 호출을 통과한 뒤 끝나지 못한 송금에 쓴다. 이 시점엔 잔액이 이미 차감됐고, 수취인에게
     * 도달했는지는 알 수 없다(호출 자체가 실패했거나, 입금은 됐는데 완료 기록만 실패했거나).
     *
     * <p>사유를 받지 않고 문구를 고정한다. 이 경로의 예외 메시지에는 transactionId 가 붙어 있어
     * 푸시에 그대로 쓰기에 맞지 않고, 무엇보다 사용자가 알아야 할 것은 원인이 아니라 "다시 보내면
     * 안 된다"는 사실이다. 보호자에게 가는 API 응답에는 원래 사유가 그대로 실린다.
     */
    public void notifyTransferUnresolved(Long transactionId) {
        TransferNotificationInfo info = findInfo(transactionId);
        if (info == null) {
            return;
        }
        notifyWard(info, TITLE_UNRESOLVED,
            String.format(BODY_UNRESOLVED, recipientNameOf(info), info.getAmount()), transactionId);
    }

    private void notifyGuardians(TransferNotificationInfo info, NotificationType type,
        String title, String body, String refType, Long refId) {
        notificationService.notifyGuardians(info.getWardId(), type, title, body, refType, refId);
    }

    private void notifyWard(TransferNotificationInfo info, String title, String body,
        Long transactionId) {
        notificationService.notifyUser(info.getWardId(), NotificationType.APPROVAL_RESULT,
            title, body, REF_TYPE_TRANSACTION, transactionId);
    }

    private TransferNotificationInfo findInfo(Long transactionId) {
        try {
            TransferNotificationInfo info =
                notificationMapper.findTransferNotificationInfo(transactionId);
            if (info == null) {
                log.warn("알림 대상 거래를 찾지 못해 알림을 건너뜀. transactionId={}", transactionId);
            }
            return info;
        } catch (RuntimeException e) {
            log.error("알림 대상 조회 실패 — 호출 흐름은 유지. transactionId={}", transactionId, e);
            return null;
        }
    }

    private String recipientNameOf(TransferNotificationInfo info) {
        return info.getRecipientName() == null ? UNKNOWN_RECIPIENT : info.getRecipientName();
    }
}
