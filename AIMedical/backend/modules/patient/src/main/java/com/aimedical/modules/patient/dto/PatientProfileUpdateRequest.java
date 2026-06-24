package com.aimedical.modules.patient.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PatientProfileUpdateRequest {

    @Size(min = 1, max = 20, message = "姓名长度需在1-20个字符之间")
    private String name;

    @Pattern(regexp = "^(男|女|未知)$", message = "性别必须为男、女或未知")
    private String gender;

    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能大于150")
    private Integer age;

    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
