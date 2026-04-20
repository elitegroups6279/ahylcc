package com.hfnew.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页结果包装类
 */
@Data
public class PageResult<T> {

    /**
     * 当前页码
     */
    private long page;

    /**
     * 每页大小
     */
    private long pageSize;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 数据列表
     */
    private List<T> list;

    public PageResult() {
    }

    public PageResult(long page, long pageSize, long total, List<T> list) {
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.list = list;
    }

    /**
     * 从 MyBatis-Plus IPage 构建 PageResult
     */
    public static <T> PageResult<T> from(IPage<T> iPage) {
        return new PageResult<>(
                iPage.getCurrent(),
                iPage.getSize(),
                iPage.getTotal(),
                iPage.getRecords()
        );
    }
}
