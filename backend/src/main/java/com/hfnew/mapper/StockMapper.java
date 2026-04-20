package com.hfnew.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hfnew.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    @Select("SELECT * FROM t_stock WHERE deleted = 0 AND material_id = #{materialId} FOR UPDATE")
    Stock selectByMaterialIdForUpdate(@Param("materialId") Long materialId);

    @Update("UPDATE t_stock SET quantity = quantity - #{qty}, update_time = CURRENT_TIMESTAMP WHERE deleted = 0 AND material_id = #{materialId} AND quantity >= #{qty}")
    int decreaseIfEnough(@Param("materialId") Long materialId, @Param("qty") Integer qty);
}
