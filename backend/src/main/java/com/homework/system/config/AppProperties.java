package com.homework.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String uploadDir;
    private String archiveDir;
    private String packageDir;
    private Integer packageLinkTtlMinutes;

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String getArchiveDir() {
        return archiveDir;
    }

    public void setArchiveDir(String archiveDir) {
        this.archiveDir = archiveDir;
    }

    public String getPackageDir() {
        return packageDir;
    }

    public void setPackageDir(String packageDir) {
        this.packageDir = packageDir;
    }

    public Integer getPackageLinkTtlMinutes() {
        return packageLinkTtlMinutes;
    }

    public void setPackageLinkTtlMinutes(Integer packageLinkTtlMinutes) {
        this.packageLinkTtlMinutes = packageLinkTtlMinutes;
    }
}
