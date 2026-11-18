package com.yunshu.zhixue.common.response;

import com.yunshu.zhixue.common.exception.ErrorCode;
import lombok.Getter;

import java.util.List;

/**
 * 统一响应结构（D4-1，方案 A）。
 *
 * <p>成功：HTTP 2xx + {@code {code:0, message:"ok", data}}。<br>
 * 失败：真实 HTTP 状态码 + {@code {code:业务码, message, data:null, fieldErrors?, fileErrors?}}。</p>
 */
@Getter
public class ApiResponse<T> {

    /** 业务码：0 表示成功，其余见 {@link ErrorCode}。 */
    private final int code;

    /** 面向用户的提示信息，不包含堆栈、SQL、内部主机等实现细节。 */
    private final String message;

    private final T data;

    /** 参数校验失败（10000）时的字段级错误。 */
    private final List<FieldErrorItem> fieldErrors;

    /** 批量导入校验失败（30005）时的行级错误。 */
    private final List<RowErrorItem> fileErrors;

    private ApiResponse(int code, String message, T data,
                        List<FieldErrorItem> fieldErrors,
                        List<RowErrorItem> fileErrors) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.fieldErrors = fieldErrors;
        this.fileErrors = fileErrors;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data, null, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "ok", null, null, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null, null, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null, null, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, null, null);
    }

    public static <T> ApiResponse<T> validationError(List<FieldErrorItem> fieldErrors) {
        return new ApiResponse<>(ErrorCode.VALIDATION_FAILED.getCode(),
                ErrorCode.VALIDATION_FAILED.getMessage(), null, fieldErrors, null);
    }

    public static <T> ApiResponse<T> importError(List<FieldErrorItem> fieldErrors,
                                                 List<RowErrorItem> fileErrors) {
        return new ApiResponse<>(ErrorCode.IMPORT_VALIDATION_FAILED.getCode(),
                ErrorCode.IMPORT_VALIDATION_FAILED.getMessage(), null, fieldErrors, fileErrors);
    }
}
