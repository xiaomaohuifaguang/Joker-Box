package com.cat.common.entity.process;

import com.cat.common.entity.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "ProcessInstancePageParam", description = "流程实例分页查询参数")
@EqualsAndHashCode(callSuper = false)
public class ProcessInstancePageParam extends PageParam {


    @Schema(description = "查询类型 1 全部 2 待办 3 草稿")
    private String type;



    @Schema(description = "用户id")
    private String userId;


    public void init(){
        type = Objects.isNull(type) ? "" : type;
        switch (type){
            case "0","1", "2", "3":break;
            default:{
                setType("");
            }
        }
    }



}
