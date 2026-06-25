package com.aimedical.modules.commonmodule.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 个人资料更新请求DTO
 *
 * <p>用于当前登录用户编辑自己的个人资料（昵称、手机号、邮箱）。
 * 不允许通过此接口修改用户名、密码、用户类型等敏感字段。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public class ProfileUpdateRequest {

    /**
     * 昵称（真实姓名）
     */
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    /**
     * 手机号
     */
    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
