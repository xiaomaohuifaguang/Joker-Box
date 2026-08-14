package com.cat.simple.config.cache;

import lombok.Getter;

@Getter
public enum CacheKeyEnum {

    TOKEN("token:", 7 * 24 * 60 * 60, "令牌"),
    ROLE_MENUS("role:menus", 7 * 24 * 60 * 60, "角色菜单绑定缓存(hash, field=角色ID)"),
    MAIL_CODE("mail:code:", 5 * 60, "邮箱验证码"),
    SSO("sso:", 24 * 60 * 60, "单点登录key"),
    ORG_TREE("org:tree", 24 * 60 * 60, "机构树"),
    PROCESS_CODE_REQ("process:code:seq:", 48 * 60 * 60, "流程每日记录"),
    AI_MODEL_DEFAULT("ai:model:default:", 7 * 24 * 60 * 60, "默认模型");




    CacheKeyEnum(String prefix, long expire, String desc) {
        this.prefix = prefix;
        this.expire = expire;
        this.desc = desc;
    }

    private final String prefix;

    private final long expire;

    private final String desc;


}
