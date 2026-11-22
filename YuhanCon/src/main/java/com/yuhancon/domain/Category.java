package com.yuhancon.domain;

public enum Category {
    CONCERT("콘서트"),
    SCHOOL("학교공연"),
    BUSKING("버스킹"),
    ETC("기타");

    private final String label;

    Category(String label) { this.label = label; }

    public String getLabel() { return label; }
}
