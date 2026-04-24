package org.example.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "firebase")
public class FirebaseProperties {
    private String adminSdkPath;
    private String allowedDomain = "dankook.ac.kr";
    private List<String> adminUids = new ArrayList<>();
    private List<String> adminEmails = new ArrayList<>();

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

    public List<String> getAdminUids() {
        return adminUids;
    }

    public void setAdminUids(List<String> adminUids) {
        this.adminUids = adminUids;
    }

    public List<String> getAdminEmails() {
        return adminEmails;
    }

    public void setAdminEmails(List<String> adminEmails) {
        this.adminEmails = adminEmails;
    }
}
