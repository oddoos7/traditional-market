package com.traditionalmarket.backend.payment.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class PaymentIdempotencyKeys {

    private PaymentIdempotencyKeys() {
    }

    public static String confirmation(String orderNumber, String paymentKey) {
        return create("confirm", orderNumber + ":" + paymentKey);
    }

    public static String fullCancellation(String paymentKey) {
        return create("cancel", paymentKey + ":full");
    }

    private static String create(String operation, String identity) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8));
            return operation + "-" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
