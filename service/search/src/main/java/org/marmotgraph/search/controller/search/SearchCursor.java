package org.marmotgraph.search.controller.search;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
@AllArgsConstructor
public class SearchCursor {
    private final ObjectMapper objectMapper;
    private final byte[] secret = generateSecret();

    private static byte[] generateSecret() {
        byte[] key = new byte[32]; // 256 bits
        new SecureRandom().nextBytes(key);
        return key;
    }

    public String encode(List<Object> sortValues) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(sortValues);
            byte[] sig = hmac(payload);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
            String encodedSig = Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
            return encodedPayload + "." + encodedSig;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode cursor", e);
        }
    }

    public List<Object> decode(String token) {
        try {
            String[] parts = token.split("\\.", 2);
            if (parts.length != 2) throw new IllegalArgumentException("Malformed cursor");
            byte[] payload = Base64.getUrlDecoder().decode(parts[0]);
            byte[] sig     = Base64.getUrlDecoder().decode(parts[1]);
            byte[] expected = hmac(payload);
            if (!MessageDigest.isEqual(sig, expected)) {
                throw new IllegalArgumentException("Invalid cursor signature");
            }
            return objectMapper.readValue(payload, objectMapper.getTypeFactory().constructCollectionType(List.class, Object.class));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode cursor", e);
        }
    }

    private byte[] hmac(byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(data);
    }

}
