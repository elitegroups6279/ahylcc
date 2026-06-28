package com.hfnew.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hfnew.dto.warehouse.StockVO;
import com.hfnew.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    @Select("SELECT * FROM t_stock WHERE deleted = 0 AND material_id = #{materialId} FOR UPDATE")
    Stock selectByMaterialIdForUpdate(@Param("materialId") Long materialId);

    @Select("SELECT * FROM t_stock WHERE deleted = 0 AND material_id = #{materialId} AND supply_category = #{supplyCategory} FOR UPDATE")
    Stock selectByMaterialAndCategoryForUpdate(@Param("materialId") Long materialId, @Param("supplyCategory") String supplyCategory);

    @Update("UPDATE t_stock SET quantity = quantity - #{qty}, update_time = CURRENT_TIMESTAMP WHERE deleted = 0 AND material_id = #{materialId} AND quantity >= #{qty}")
    int decreaseIfEnough(@Param("materialId") Long materialId, @Param("qty") Integer qty);

    @Select("""
            <script>
            SELECT s.id AS stock_id, s.material_id, s.supply_category, s.quantity, s.total_value,
                   m.name AS material_name, m.category, m.specification, m.unit, m.warning_threshold
            FROM t_stock s
            JOIN t_material m ON m.id = s.material_id
            WHERE s.deleted = 0 AND m.deleted = 0
            <if test="warningOnly">AND s.quantity &lt;= m.warning_threshold</if>
            <if test="supplyCategory != null and supplyCategory != ''">AND s.supply_category = #{supplyCategory}</if>
            ORDER BY s.id DESC
            </script>
            """)
    IPage<StockVO> selectStockPage(IPage<StockVO> page, @Param("warningOnly") boolean warningOnly, @Param("supplyCategory") String supplyCategory);
}
