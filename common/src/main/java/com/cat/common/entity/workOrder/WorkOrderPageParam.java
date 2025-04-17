package com.cat.common.entity.workOrder;

import com.cat.common.entity.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "WorkOrderPageParam", description = "工单分页查询参数")
@EqualsAndHashCode(callSuper = false)
public class WorkOrderPageParam extends PageParam {


    @Schema(description = "查询类型 1 全部 2 待办 0 草稿 11 本人申请")
    private String type;

    @Schema(description = "用户id")
    private String userId;


    public void init(){
        type = Objects.isNull(type) ? "" : type;
        switch (type){
            case "1", "2", "0", "11":break;
            default:{
                setType("");
            }
        }

    }



}