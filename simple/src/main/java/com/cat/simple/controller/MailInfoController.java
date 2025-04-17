package com.cat.simple.controller;

import com.cat.common.entity.*;
import com.cat.common.entity.mail.MailInfo;
import com.cat.simple.service.MailInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mailInfo")
@Tag(name = "mailInfo")
public class MailInfoController {

@Resource
private MailInfoService mailInfoService;

//    @Operation(summary = "添加")
//    @RequestMapping(value = "/add",method = RequestMethod.POST)
//    public HttpResult<?> add(@RequestBody MailInfo mailInfo) {
//        return HttpResult.back(mailInfoService.add(mailInfo) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
//    }
//
//    @Operation(summary = "删除")
//    @RequestMapping(value = "/remove",method = RequestMethod.POST)
//    public HttpResult<?> remove(@RequestBody MailInfo mailInfo) {
//        return HttpResult.back(mailInfoService.delete(mailInfo) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
//    }
//
//    @Operation(summary = "修改")
//    @RequestMapping(value = "/update",method = RequestMethod.POST)
//    public HttpResult<?> update(@RequestBody MailInfo mailInfo) {
//        return HttpResult.back(mailInfoService.update(mailInfo) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
//    }

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<MailInfo> info(@RequestBody MailInfo mailInfo) {
        return HttpResult.back(mailInfoService.info(mailInfo));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<MailInfo>> queryPage(@RequestBody PageParam pageParam) {
        return HttpResult.back(mailInfoService.queryPage(pageParam));
    }



}
