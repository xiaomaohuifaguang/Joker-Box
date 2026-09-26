package com.cat.simple.rapiddev.service.impl;

import com.cat.common.entity.rapidDevelopment.FieldInfo;
import com.cat.common.entity.rapidDevelopment.SampleCode;
import com.cat.common.entity.rapidDevelopment.TableInfo;
import com.cat.common.utils.ServletUtils;
import com.cat.simple.rapiddev.mapper.RapidDevelopmentMapper;
import com.cat.simple.rapiddev.service.RapidDevelopmentService;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class RapidDevelopmentServiceImpl implements RapidDevelopmentService {

    @Resource
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @Resource
    private RapidDevelopmentMapper rapidDevelopmentMapper;

    private static final String resource_path = "generate/";



    @Override
    public SampleCode generate(String tableName) throws IOException, TemplateException {

        TableInfo tableInfo = makeTableInfo(tableName);

        return makeSampleCode(tableInfo);
    }

    @Override
    public void download(String tableName) throws IOException, TemplateException {
        TableInfo tableInfo = makeTableInfo(tableName);
        SampleCode sampleCode = makeSampleCode(tableInfo);
        Map<String, String> files = makeFiles(sampleCode, tableInfo);
        HttpServletResponse response = ServletUtils.getHttpServletResponse();
        response.setContentType("application/zip");
        String fileName = URLEncoder.encode(tableName + "-code.zip", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        try (ZipOutputStream zip = new ZipOutputStream(response.getOutputStream())) {
            for (Map.Entry<String, String> e : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }

    private TableInfo makeTableInfo(String tableName){
        TableInfo tableInfo = rapidDevelopmentMapper.getBaseTableInfo(tableName);
        if(Objects.isNull(tableInfo)){
            throw new IllegalStateException("表不存在");
        }

        List<FieldInfo> fieldInfos = rapidDevelopmentMapper.queryAllFields(tableName);
        tableInfo.setFieldInfos(fieldInfos);
        tableInfo.init();
        return tableInfo;
    }

    private SampleCode makeSampleCode(TableInfo tableInfo) throws TemplateException, IOException {
        SampleCode sampleCode = new SampleCode();
        // 实体类java 生成
        String entity = makeEntity(tableInfo);
        sampleCode.setEntity(entity);

        // mapper接口 生成
        String mapper = makeMapper(tableInfo);
        sampleCode.setMapper(mapper);

        // mapper xml 生成
        String mapperXml = makeMapperXml(tableInfo);
        sampleCode.setMapperXml(mapperXml);

        // 业务层接口 生成
        String service = makeService(tableInfo);
        sampleCode.setService(service);

        // 业务层实现 生成
        String impl = makeServiceImpl(tableInfo);
        sampleCode.setImpl(impl);

        // 控制层 生成
        String controller = makeController(tableInfo);
        sampleCode.setController(controller);

        // 前端 ts 类型
        String tsTypes = makeTsTypes(tableInfo);
        sampleCode.setTypes(tsTypes);

        // 前端 分页hook
        String usePage = makeUsePage(tableInfo);
        sampleCode.setUsePage(usePage);

        // 前端 ts api
        String tsApi = makeTsApi(tableInfo);
        sampleCode.setApi(tsApi);

        // 前端react 页面
        String reactPage = makeReactPage(tableInfo);
        sampleCode.setPage(reactPage);

        // 新增/修改弹窗
        String formDialog = makeFormDialog(tableInfo);
        sampleCode.setFormDialog(formDialog);

        // 类型索引
        String typeIndex = makeTypeIndex(tableInfo);
        sampleCode.setTypeIndex(typeIndex);

        // 推荐菜单配置说明
        String readme = makeReadme(tableInfo);
        sampleCode.setReadme(readme);

        return sampleCode;
    }

    private Map<String, String> makeFiles(SampleCode sampleCode, TableInfo tableInfo){
        List<String> contents = List.of(
                sampleCode.getEntity(),
                sampleCode.getController(),
                sampleCode.getService(),
                sampleCode.getImpl(),
                sampleCode.getMapper(),
                sampleCode.getMapperXml(),
                sampleCode.getTypes(),
                sampleCode.getUsePage(),
                sampleCode.getApi(),
                sampleCode.getFormDialog(),
                sampleCode.getPage());
        List<String> paths = makeFilePaths(tableInfo);
        Map<String, String> files = new LinkedHashMap<>();
        for (int i = 0; i < paths.size(); i++) {
            files.put(paths.get(i), contents.get(i));
        }
        return files;
    }

    /**
     * 生成文件的目标路径列表（与 makeFiles 中内容顺序一一对应）
     */
    private List<String> makeFilePaths(TableInfo tableInfo){
        List<String> paths = new ArrayList<>();
        paths.add("后端/common/src/main/java/"+tableInfo.getEntityPackage().replace(".","/") +"/"+tableInfo.getTableNameUp()+".java");
        paths.add("后端/simple/src/main/java/"+tableInfo.getControllerPackage().replace(".","/") +"/"+tableInfo.getTableNameUp()+"Controller.java");
        paths.add("后端/simple/src/main/java/"+tableInfo.getServicePackage().replace(".","/") +"/"+tableInfo.getTableNameUp()+"Service.java");
        paths.add("后端/simple/src/main/java/"+tableInfo.getImplPackage().replace(".","/") +"/"+tableInfo.getTableNameUp()+"ServiceImpl.java");
        paths.add("后端/simple/src/main/java/"+tableInfo.getMapperPackage().replace(".","/") +"/"+tableInfo.getTableNameUp()+"Mapper.java");
        paths.add("后端/simple/src/main/resources/"+tableInfo.getMapperXmlPackage().replace(".","/") +"/"+tableInfo.getTableNameUp()+"Mapper.xml");
        paths.add("前端/types/"+tableInfo.getTableNameDown()+".ts");
        paths.add("前端/hooks/"+"use"+tableInfo.getTableNameUp()+"Page.ts");
        paths.add("前端/lib/api/"+tableInfo.getTableNameDown()+".ts");
        paths.add("前端/app/console/"+ tableInfo.getTableNameDown() + "/_components/" + tableInfo.getTableNameUp() + "FormDialog.tsx");
        paths.add("前端/app/console/"+ tableInfo.getTableNameDown() + "/page.tsx");
        return paths;
    }

    private String makeEntity(TableInfo tableInfo) throws IOException, TemplateException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate(resource_path + "entity.java.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeMapper(TableInfo tableInfo) throws IOException, TemplateException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate(resource_path + "mapper.java.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeMapperXml(TableInfo tableInfo) throws IOException, TemplateException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate(resource_path + "mapper.xml.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeService(TableInfo tableInfo) throws IOException, TemplateException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate(resource_path + "service.java.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeServiceImpl(TableInfo tableInfo) throws TemplateException, IOException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate(resource_path + "service.impl.java.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeController(TableInfo tableInfo) throws TemplateException, IOException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate(resource_path + "controller.java.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeTsTypes(TableInfo tableInfo) throws IOException, TemplateException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate(resource_path + "type.ts.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeTsApi(TableInfo tableInfo) throws IOException, TemplateException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate(resource_path + "api.ts.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeReactPage(TableInfo tableInfo) throws IOException, TemplateException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate(resource_path + "page.tsx.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeUsePage(TableInfo tableInfo) throws IOException, TemplateException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate(resource_path + "usePage.ts.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeFormDialog(TableInfo tableInfo) throws IOException, TemplateException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate(resource_path + "dialog.tsx.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeTypeIndex(TableInfo tableInfo) throws IOException, TemplateException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate(resource_path + "index.ts.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeReadme(TableInfo tableInfo)  {
        return """
                # 推荐配置

                - 菜单名称: %s
                - 路径: /console/%s

                # 生成文件结构

                ```text
                %s
                ```
                """.formatted(tableInfo.getTableComment(), tableInfo.getTableNameDown(), makeStructureTree(tableInfo));
    }

    /**
     * 将生成文件的扁平路径渲染成目录结构树
     */
    private String makeStructureTree(TableInfo tableInfo) {
        TreeNode root = new TreeNode("");
        for (String path : makeFilePaths(tableInfo)) {
            TreeNode node = root;
            for (String segment : path.split("/")) {
                node = node.children.computeIfAbsent(segment, TreeNode::new);
            }
        }
        compressTree(root);
        StringBuilder sb = new StringBuilder();
        renderTree(root, sb, "");
        return sb.toString().stripTrailing();
    }

    /**
     * 合并单链子节点（a -> b -> c 合并为 a/b/c），避免深层包路径每层独占一行
     */
    private void compressTree(TreeNode node) {
        for (TreeNode child : node.children.values()) {
            compressTree(child);
        }
        Map<String, TreeNode> merged = new LinkedHashMap<>();
        for (TreeNode child : node.children.values()) {
            TreeNode current = child;
            while (current.children.size() == 1) {
                TreeNode next = current.children.values().iterator().next();
                current = new TreeNode(current.name + "/" + next.name, next.children);
            }
            merged.put(current.name, current);
        }
        node.children = merged;
    }

    private void renderTree(TreeNode node, StringBuilder sb, String prefix) {
        int i = 0;
        for (TreeNode child : node.children.values()) {
            boolean last = ++i == node.children.size();
            sb.append(prefix).append(last ? "└── " : "├── ").append(child.name).append('\n');
            renderTree(child, sb, prefix + (last ? "    " : "│   "));
        }
    }

    /**
     * 结构树节点，children 用 LinkedHashMap 保持插入顺序（先后端后前端）
     */
    private static class TreeNode {
        private final String name;
        private Map<String, TreeNode> children;

        TreeNode(String name) {
            this(name, new LinkedHashMap<>());
        }

        TreeNode(String name, Map<String, TreeNode> children) {
            this.name = name;
            this.children = children;
        }
    }












}
