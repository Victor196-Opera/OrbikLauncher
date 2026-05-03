package com.orbik.launcher;

import android.content.pm.ResolveInfo;

public class AppInfo {
    public String appName;
    public String packageName;
    public ResolveInfo resolveInfo;

    public AppInfo(String appName, String packageName, ResolveInfo resolveInfo) {
        this.appName = appName;
        this.packageName = packageName;
        this.resolveInfo = resolveInfo;
    }
}