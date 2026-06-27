package com.aimedical.common.result;

import com.aimedical.common.exception.ErrorCode;
import lombok.Data;

@Data
public class Result<T> {

    private String code;
    private String message;
    private T data;

    public Result() {
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = "SUCCESS";
        result.message = "成功";
        result.data = data;
        return result;
    }

    public static <T> Result<T> success(T data, String message) {
        Result<T> result = new Result<>();
        result.code = "SUCCESS";
        result.message = message;
        result.data = data;
        return result;
    }

    public static <T> Result<T> fail(String code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        return result;
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }
}
