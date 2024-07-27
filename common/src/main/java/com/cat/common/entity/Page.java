package com.cat.common.entity;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/***
 * 分页
 * @title Page
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/25 16:11
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "Page", description = "分页")
public class Page<T> implements IPage<T> {

    @Schema(description = "记录")
    private List<T> records;
    @Schema(description = "总数")
    private long total;
    @Schema(description = "页大小")
    private long size;
    @Schema(description = "页码")
    private long current;
//    @Schema(description = "总页数")
//    private long totalPage;

    public Page(PageParam pageParam) {
        this.setSize(pageParam.getSize() > 5 ? pageParam.getSize() : 10L);
        this.setCurrent(pageParam.getCurrent() > 0 ? pageParam.getCurrent() : 1L);
    }

    @Override
    public List<OrderItem> orders() {
        return null;
    }

    @Override
    public List<T> getRecords() {
        return this.records;
    }

    @Override
    public IPage<T> setRecords(List<T> records) {
        this.records = records;
        return this;
    }

    @Override
    public long getTotal() {
        return this.total;
    }

    @Override
    public IPage<T> setTotal(long total) {
        this.total = total;
        return this;
    }

    @Override
    public long getSize() {
        return this.size;
    }

    @Override
    public IPage<T> setSize(long size) {
        this.size = size;
        return this;
    }

    @Override
    public long getCurrent() {
        return this.current;
    }

    @Override
    public IPage<T> setCurrent(long current) {
        this.current = current;
        return this;
    }

//    public long getTotalPage() {
//        return this.getPages();
//    }
}