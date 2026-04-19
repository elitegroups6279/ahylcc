package com.hfnew.dto.rbac;

import java.util.List;

public class PermissionsResponse {
    private List<String> permissions;

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}

