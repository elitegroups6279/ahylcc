package com.hfnew.config;

/**
 * ThreadLocal holder for current user's organization ID.
 * Used by OrgTenantHandler to automatically filter queries by org.
 */
public class OrgContextHolder {

    /**
     * Default org_id used when super admin (orgId=null) creates records.
     * Should match the primary organization in t_organization.
     */
    public static final Long DEFAULT_ORG_ID = 2L;

    private static final ThreadLocal<Long> CURRENT_ORG_ID = new ThreadLocal<>();

    public static void setOrgId(Long orgId) {
        CURRENT_ORG_ID.set(orgId);
    }

    public static Long getOrgId() {
        return CURRENT_ORG_ID.get();
    }

    /**
     * Returns the effective org_id: the user's own org if set,
     * otherwise the default org (for super admin auto-fill on INSERT).
     */
    public static Long getEffectiveOrgId() {
        Long orgId = CURRENT_ORG_ID.get();
        return orgId != null ? orgId : DEFAULT_ORG_ID;
    }

    public static void clear() {
        CURRENT_ORG_ID.remove();
    }
}
