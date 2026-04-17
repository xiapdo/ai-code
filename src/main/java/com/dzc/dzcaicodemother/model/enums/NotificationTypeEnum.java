package com.dzc.dzcaicodemother.model.enums;

public enum NotificationTypeEnum {

    LIKE("LIKE", "点赞"),
    COLLECT("COLLECT", "收藏"),
    COMMENT("COMMENT", "评论"),
    REPLY("REPLY", "回复");

    private final String value;
    private final String description;

    NotificationTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static NotificationTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (NotificationTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
