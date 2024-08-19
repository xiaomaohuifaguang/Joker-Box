package com.cat.common.entity.ai.chat.res;

import com.cat.common.entity.ai.chat.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/***
 * <TODO description class purpose>
 * @title Choice
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/8/3 15:29
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Choice implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private long index;

    private Message message;

    private Message delta;

    private Boolean logprobs;

    private String finish_reason;



}
