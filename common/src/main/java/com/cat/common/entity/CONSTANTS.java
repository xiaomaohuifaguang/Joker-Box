package com.cat.common.entity;

/***
 * 常量
 * @title Constants
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/24 23:21
 **/
public class CONSTANTS {

    public static final String DEFAULT_PASSWORD = "12345678";

    public static final Integer ROLE_ADMIN_CODE = 1;
    public static final Integer ROLE_EVERYONE_CODE = 2;

    public static final String REDIS_PARENT_TOKEN = "token:";

    public static final String REDIS_PARENT_MAIL_CODE = "mail:code:";

    /**
     * Security 匿名用户 角色
     */
    public static final String ANONYMOUS_ROLE = "ROLE_ANONYMOUS";

    /**
     * 令牌类型
     */
    public static final String TOKEN_TYPE = "Bearer";

    /**
     * 用户类型 普通用户
     */
    public static final String USER_TYPE_USER = "0";

    /**
     * 用户类型 服务
     */
    public static final String USER_TYPE_SERVER = "1";


    /**
     * 文件类型
     */
    public static final String FILE_TYPE_1 = "file";
    public static final String FILE_TYPE_2 = "folder";
    public static final String FILE_TYPE_3 = "avatar";

    /**
     * 根目录
     */
    public static final String FILE_ALL_PARENT = "0";



}
