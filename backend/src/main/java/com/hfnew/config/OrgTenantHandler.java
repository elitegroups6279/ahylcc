package com.hfnew.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;

import java.util.Arrays;
import java.util.List;

/**
 * MyBatis-Plus tenant handler that enforces org_id data isolation.
 * - Super admins (orgId=null) see all data (ignoreTable returns true).
 * - Regular users only see data belonging to their own org_id.
 * - Only applies to business tables that have org_id column.
 */
public class OrgTenantHandler implements TenantLineHandler {

    /**
     * Tables that have org_id and need automatic filtering.
     * All other tables are ignored (no org_id column).
     */
    private static final List<String> TENANT_TABLES = Arrays.asList(
            "t_elderly", "t_staff", "t_bed",
            "t_payment_record", "t_fee_account", "t_fee_bill"
    );

    @Override
    public Expression getTenantId() {
        Long orgId = OrgContextHolder.getOrgId();
        if (orgId != null) {
            return new LongValue(orgId);
        }
        // When orgId is null (super admin), ignoreTable returns true,
        // so this NullValue won't actually be used in SQL generation.
        return new NullValue();
    }

    @Override
    public String getTenantIdColumn() {
        return "org_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // Super admin (orgId is null) sees all data — skip tenant filtering
        if (OrgContextHolder.getOrgId() == null) {
            return true;
        }
        // Only filter tables that have org_id column
        return !TENANT_TABLES.contains(tableName.toLowerCase());
    }
}
