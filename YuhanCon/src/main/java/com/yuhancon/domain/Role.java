package com.yuhancon.domain;

public enum Role {
    USER("일반 사용자"),
    PERFORMER("공연 등록자"),
    ADMIN("관리자");

    private final String label;

    Role(String label) { this.label = label; }

    public String getLabel() { return label; }
}
