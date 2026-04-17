package com.dzc.dzcaicodemother.model.dto.notification;

import java.io.Serializable;

import lombok.Data;

@Data
public class NotificationQueryRequest implements Serializable {

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    private Boolean unreadOnly;
}
