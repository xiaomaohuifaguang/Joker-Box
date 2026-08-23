package com.cat.simple.ai.tools.system;


import com.cat.common.entity.auth.Org;
import com.cat.common.entity.auth.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "UserInfo", description = "用户信息")
public class UserInfoVO {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "管理员")
    private boolean admin;

    @Schema(description = "性别")
    private String sex = "未知";

    @Schema(description = "邮箱")
    private String mail;

    @Schema(description = "手机号")
    private Long phone;




}
