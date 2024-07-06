package com.cat.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/***
 * 统一返回信息
 * @title HttpResult
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/23 23:59
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "HttpResult", description = "响应")
public class HttpResult<T> {

    @Schema(description = "状态码")
    private long code = HttpResultStatus.SUCCESS.code();
    @Schema(description = "响应数据")
    private T data;
    @Schema(description = "响应消息")
    private String msg = HttpResultStatus.SUCCESS.msg();
    @Schema(description = "时间戳")
    private long timestamp = System.currentTimeMillis();

    /**
     * 响应方法
     * @return 统一返回信息
     * @param <T> 接收泛型
     */
    public static <T> HttpResult<T> back(T data){
        return back(HttpResultStatus.SUCCESS, data);
    }

    public static <T> HttpResult<T> back(HttpResultStatus httpResultStatus, T data){
        return back(httpResultStatus.code(), data, httpResultStatus.msg());
    }

    public static <T> HttpResult<T> back(long code,  T data, String msg){
        return new HttpResult<T>().setCode(code).setData(data).setMsg(msg);
    }

    public static <T> HttpResult<T> back(HttpResultStatus httpResultStatus){
        return new HttpResult<T>().setCode(httpResultStatus.code()).setMsg(httpResultStatus.msg());
    }


}
