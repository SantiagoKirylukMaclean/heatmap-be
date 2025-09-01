package com.puetsnao.shared.http;

public interface EtagService {
    String buildWeak(String namespace, String... parts);
    boolean matches(String ifNoneMatchHeader, String etag);
}
