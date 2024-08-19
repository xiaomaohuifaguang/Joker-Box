package com.cat.common.entity.ai.chat.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/***
 * <TODO description class purpose>
 * @title ChatResponse
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/8/3 15:28
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ChatResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;


    private String id;

    private String object;

    private String created;

    private String model;

    private String system_fingerprint;

    private List<Choice> choices;

    private Usage usage;

}
