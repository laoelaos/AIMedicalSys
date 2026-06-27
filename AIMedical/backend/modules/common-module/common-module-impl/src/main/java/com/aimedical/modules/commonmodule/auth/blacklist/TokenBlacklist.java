package com.aimedical.modules.commonmodule.auth.blacklist;

public interface TokenBlacklist {

    void add(String jti, long expirationTime);

    boolean isBlacklisted(String jti);
}
