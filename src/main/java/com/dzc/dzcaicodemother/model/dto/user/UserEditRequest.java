package com.dzc.dzcaicodemother.model.dto.user;

import java.io.Serializable;

import com.mybatisflex.annotation.Column;

import lombok.Data;

/**
 * 用户编辑资料请求
 *
 */
@Data
public class UserEditRequest implements Serializable {

    /**
     * 用户昵称
     */
    @Column("userName")
    private String userName;

    /**
     * 用户头像
     */
    @Column("userAvatar")
    private String userAvatar;

    /**
     * 用户简介
     */
    @Column("userProfile")
    private String userProfile;

    /**
     * 手机号
     */
    @Column("phone")
    private String phone;

    /**
     * 邮箱
     */
    @Column("email")
    private String email;
}
