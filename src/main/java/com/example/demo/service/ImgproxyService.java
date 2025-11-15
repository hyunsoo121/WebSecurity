package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

@Service
public class ImgproxyService {

    private final String baseUrl;
    private final byte[] key;
    private final byte[] salt;

    public ImgproxyService(
            @Value("${imgproxy.base-url}") String baseUrl,
            @Value("${imgproxy.key}") String keyHex,
            @Value("${imgproxy.salt}") String saltHex) {
        this.baseUrl = baseUrl;
        this.key = hexStringToByteArray(keyHex);
        this.salt = hexStringToByteArray(saltHex);
    }

    public String generateSignedUrl(String sourceUrl, String options) {
        try {
            String encodedUrl = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(sourceUrl.getBytes(StandardCharsets.UTF_8));

            String path = options + "/" + encodedUrl;

            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key, "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] data = addSaltAndPath(path.getBytes(StandardCharsets.UTF_8));
            byte[] hash = sha256_HMAC.doFinal(data);

            String signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(hash);

            return String.format("%s/%s%s", baseUrl, signature, path);

        } catch (Exception e) {
            throw new RuntimeException("Imgproxy URL 생성 실패", e);
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private byte[] addSaltAndPath(byte[] path) {
        byte[] data = new byte[salt.length + path.length];
        System.arraycopy(salt, 0, data, 0, salt.length);
        System.arraycopy(path, 0, data, salt.length, path.length);
        return data;
    }
}