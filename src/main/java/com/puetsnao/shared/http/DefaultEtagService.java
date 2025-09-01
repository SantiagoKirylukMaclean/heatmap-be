package com.puetsnao.shared.http;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

@Service
public class DefaultEtagService implements EtagService {
    @Override
    public String buildWeak(String namespace, String... parts) {
        StringBuilder sb = new StringBuilder();
        sb.append(namespace);
        for (String p : parts) {
            sb.append(':').append(p == null ? "" : p);
        }
        CRC32 crc = new CRC32();
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        crc.update(bytes, 0, bytes.length);
        String hex = Long.toHexString(crc.getValue());
        return "W/\"" + hex + "\"";
    }

    @Override
    public boolean matches(String ifNoneMatchHeader, String etag) {
        if (ifNoneMatchHeader == null || ifNoneMatchHeader.isBlank()) return false;
        String header = ifNoneMatchHeader.trim();
        if ("*".equals(header)) return true;
        String[] tokens = header.split(",");
        for (String t : tokens) {
            String v = t.trim();
            if (v.equals(etag)) return true;
        }
        return false;
    }
}
