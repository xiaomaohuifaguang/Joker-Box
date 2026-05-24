package com.cat.simple.codeTable.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.common.entity.codeTable.CodeItem;
import com.cat.common.entity.codeTable.CodeItemQueryParam;
import com.cat.common.entity.codeTable.CodeOption;
import com.cat.common.entity.codeTable.CodeTable;
import com.cat.common.entity.codeTable.CodeTablePageParam;

import java.util.List;

public interface CodeTableService {

    Page<CodeTable> page(CodeTablePageParam param);

    CodeTable addTable(CodeTable codeTable);

    CodeTable updateTable(CodeTable codeTable);

    void deleteTable(String id);

    CodeTable detailTable(String id);

    List<CodeOption> options(String code);

    List<CodeItem> listItems(CodeItemQueryParam param);

    CodeItem addItem(CodeItem codeItem);

    CodeItem updateItem(CodeItem codeItem);

    void deleteItem(String id);

    List<CodeOption> treeItems(CodeItemQueryParam param);
}
