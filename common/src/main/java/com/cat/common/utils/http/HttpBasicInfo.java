package com.cat.common.utils.http;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/***
 * <TODO description class purpose>
 * @title HttpBasicInfo
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/8/3 1:17
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class HttpBasicInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String url;






}
