package in.atail.moneymanager.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 统一错误响应格式
 *
 * 用于返回给前端的错误信息结构
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {

    /**
     * HTTP 状态码
     * 例如: 400, 401, 403, 404, 500
     */
    private int status;

    /**
     * 错误消息
     * 用户可读的错误描述
     */
    private String message;

    /**
     * 错误发生时间
     * 格式: yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 请求路径
     * 记录发生错误的 API 路径
     */
    private String path;

    /**
     * 错误详情 (仅开发环境使用)
     * 详细的错误堆栈或调试信息
     */
    private String details;

    // 简化构造方法
    public ErrorResponse(int status, String message, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }
}