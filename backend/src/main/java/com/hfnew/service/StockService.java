package com.hfnew.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.warehouse.StockVO;
import com.hfnew.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockMapper stockMapper;

    public PageResult<StockVO> list(int page, int pageSize, boolean warningOnly, String supplyCategory) {
        int p = Math.max(page, 1);
        int ps = Math.max(pageSize, 1);
        Page<StockVO> pageReq = new Page<>(p, ps);
        IPage<StockVO> result = stockMapper.selectStockPage(pageReq, warningOnly, supplyCategory);

        for (StockVO vo : result.getRecords()) {
            int qty = vo.getQuantity() != null ? vo.getQuantity() : 0;
            int threshold = vo.getWarningThreshold() != null ? vo.getWarningThreshold() : 0;
            vo.setWarning(qty <= threshold ? 1 : 0);
        }

        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }
}
