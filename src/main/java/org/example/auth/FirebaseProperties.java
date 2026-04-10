package org.example.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase")
public class FirebaseProperties {
    private String adminSdkPath;
    private String allowedDomain = "dankook.ac.kr";

    public String getAdminSdkPath() {
        return adminSdkPath;
    }

    public void setAdminSdkPath(String adminSdkPath) {
        this.adminSdkPath = adminSdkPath;
    }

    public String getAllowedDomain() {
        return allowedDomain;
    }

    public void setAllowedDomain(String allowedDomain) {
        this.allowedDomain = allowedDomain;
    }
}
