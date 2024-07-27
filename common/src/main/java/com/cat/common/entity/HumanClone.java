package com.cat.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/***
 * <TODO description class purpose>
 * @title HumanClone
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/10 23:39
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "HumanClone", description = "克隆人信息")
public class HumanClone {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "性别")
    private String sex;

    @Schema(description = "身份证号")
    private String idCard;

    @Schema(description = "手机号")
    private long phone;

    @Schema(description = "邮箱")
    private String mail;


}
