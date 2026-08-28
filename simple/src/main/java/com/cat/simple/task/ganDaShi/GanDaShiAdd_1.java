package com.cat.simple.task.ganDaShi;

import com.alibaba.fastjson2.JSONObject;
import com.cat.common.entity.ganDaShi.GanDaShiPost;
import com.cat.common.utils.IOUtils;
import com.cat.common.utils.JSONUtils;
import com.cat.simple.gandashi.service.GanDaShiPostService;
import jakarta.annotation.Resource;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class GanDaShiAdd_1 {

    @Resource
    private GanDaShiPostService ganDaShiPostService;

//    @Scheduled(initialDelay = 30, fixedDelay = 365 * 24 * 60 * 60, timeUnit = TimeUnit.SECONDS)
//    @SchedulerLock(name = "GanDaShiAdd1.test", lockAtMostFor = "30m")
    public void test() throws IOException {

        naLanXingDe();
        shuiMoTangShi();
    }


    // 纳兰性德
    private void naLanXingDe() throws IOException {
        String jsonStr = IOUtils.readTextByPath("C:\\Users\\six6\\todo\\projects\\chinese-poetry\\纳兰性德\\纳兰性德诗集.json");
        List<Map<String, Object>> maps = JSONUtils.parseMapList(jsonStr);
        for (Map<String, Object> map : maps) {
            String title = map.get("title").toString();
            String author = map.get("author").toString();

            String paraJsonStr = JSONUtils.toJSONString(map.get("para"));
            List<String> para = JSONUtils.parseList(paraJsonStr, String.class);
            String paraStr = String.join("\n", para);

            String paraHtml = para.stream()
                    .map(p -> "<p>" + p + "</p>")
                    .collect(Collectors.joining("\n"));

            String content =
                    """
                    <p>%s</p><p>作者：%s</p>%s
                    """.formatted(title, author, paraHtml);

            String text =
                    """
                        %s
                    作者：%s
                    
                    %s
                    """.formatted(title, author, paraStr);

            GanDaShiPost ganDaShiPost = new GanDaShiPost();
            ganDaShiPost.setId(null);
            ganDaShiPost.setCreateBy("1");
            ganDaShiPost.setTitle(title);
            ganDaShiPost.setContent(content);
            ganDaShiPost.setText(text);
            ganDaShiPost.setCreateTime(LocalDateTime.now());
            ganDaShiPost.setDeleted("0");
            ganDaShiPost.setViewCount(0);
            ganDaShiPostService.addWithUserId(ganDaShiPost);
        }
    }


    // 水墨唐诗
    private void shuiMoTangShi() throws IOException {
        String jsonStr = IOUtils.readTextByPath("C:\\Users\\six6\\todo\\projects\\chinese-poetry\\水墨唐诗\\shuimotangshi.json");
        List<Map<String, Object>> maps = JSONUtils.parseMapList(jsonStr);
        for (Map<String, Object> map : maps) {
            String title = map.get("title").toString();
            String author = map.get("author").toString();

            String paraJsonStr = JSONUtils.toJSONString(map.get("paragraphs"));
            List<String> para = JSONUtils.parseList(paraJsonStr, String.class);
            String paraStr = String.join("\n", para);

            String paraHtml = para.stream()
                    .map(p -> "<p>" + p + "</p>")
                    .collect(Collectors.joining("\n"));

            String prologue = map.get("prologue").toString();

            String content =
            """
            <p>%s</p><p>作者：%s</p>%s<p>%s<p/>
            """.formatted(title, author, paraHtml, prologue);

            String text =
            """
                %s
            作者：%s
           
            %s
            
            注释：%s
            """.formatted(title, author, paraStr, prologue);

            GanDaShiPost ganDaShiPost = new GanDaShiPost();
            ganDaShiPost.setId(null);
            ganDaShiPost.setCreateBy("1");
            ganDaShiPost.setTitle(title);
            ganDaShiPost.setContent(content);
            ganDaShiPost.setText(text);
            ganDaShiPost.setCreateTime(LocalDateTime.now());
            ganDaShiPost.setDeleted("0");
            ganDaShiPost.setViewCount(0);
            ganDaShiPostService.addWithUserId(ganDaShiPost);
        }
    }

}
