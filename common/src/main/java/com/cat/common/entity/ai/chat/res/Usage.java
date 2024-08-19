package com.cat.common.entity.ai.chat.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/***
 * <TODO description class purpose>
 * @title Usage
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/8/3 15:31
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Usage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private long prompt_tokens;

    private long completion_tokens;

    private long total_tokens;



}
