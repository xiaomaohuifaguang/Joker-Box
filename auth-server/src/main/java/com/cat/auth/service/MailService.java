package com.cat.auth.service;

import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;

import java.io.IOException;

/***
 * 邮件业务层
 * @title MailService
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/8 0:19
 **/
public interface MailService {

    /**
     * 发送验证码
     * @param email 邮箱
     * @param code 验证码
     * @return 验证码
     */
    String sendCode(String email) throws IOException, TemplateException, MessagingException;

}
