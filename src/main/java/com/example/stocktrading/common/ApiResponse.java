package com.example.stocktrading.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private int status;
    private T data;
    private String message;
    private String timestamp;

    public static ApiResponse<MsgData> successOnlyMsg(String message) {
        return new ApiResponse<>(200, new MsgData(message), "Success",
                ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, null, "Success",
                ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, data, "Success",
                ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(200, data, message,
                ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, null, message,
                ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    public record MsgData(String message) {
    }
}
