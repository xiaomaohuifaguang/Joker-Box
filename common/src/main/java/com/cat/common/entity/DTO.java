package com.cat.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

/***
 * <TODO description class purpose>
 * @title DTO
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/10 0:37
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "DTO", description = "业务层传递控制层")
public class DTO<T> {
    public boolean flag = true;
    public String msg = "请求成功";
    public T data;

    public static DTO<?> success(){
        return new DTO<>();
    }

    public static <T> DTO<T> success(T t){
        return new DTO<T>().setData(t);
    }

    public static <T> DTO<T> back(T t){
        return new DTO<T>().setData(t);
    }

    public static DTO<?> error(String msg){
        return new DTO<>().setFlag(false).setMsg(StringUtils.hasText(msg) ? msg : "请求失败");
    }
    public static <T> DTO<T> error(String msg, T t){
        return new DTO<T>().setFlag(false).setMsg(StringUtils.hasText(msg) ? msg : "请求失败");
    }

}
