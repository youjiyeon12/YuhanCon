package com.yuhancon.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 보안 기록(로그인 성공·실패·잠금, 권한 없는 접근, 관리자 작업). logback-spring.xml이 logs/security.log로 따로 모은다.
// 비밀번호·세션 값은 남기지 않는다.
public final class SecurityLog {

    public static final Logger LOG = LoggerFactory.getLogger("SECURITY");

    private SecurityLog() {}

    // 사용자가 입력한 값은 줄바꿈을 지우고 길이를 잘라, 가짜 기록 줄을 끼워 넣지 못하게 한다
    public static String clean(String raw) {
        if (raw == null) return "-";
        String s = raw.replaceAll("[\r\n\t]", "_");
        return s.length() > 40 ? s.substring(0, 40) + "…" : s;
    }
}
