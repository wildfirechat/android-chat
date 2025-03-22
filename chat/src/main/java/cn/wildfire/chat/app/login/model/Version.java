package cn.wildfire.chat.app.login.model;

public class Version {

    /** 主键 */
    private Long id;

    /** 版本号(整数类型，用于比较) */
    private Integer versionCode;

    /** 版本名称(如1.0.0) */
    private String versionName;

    /** OSS文件地址 */
    private String apkUrl;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 文件MD5校验值 */
    private String md5;

    /** 是否强制更新(0否 1是) */
    private Integer forceUpdate;

    /** 更新描述 */
    private String updateDesc;

    /** 状态(0停用 1启用) */
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(Integer versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getApkUrl() {
        return apkUrl;
    }

    public void setApkUrl(String apkUrl) {
        this.apkUrl = apkUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Integer getForceUpdate() {
        return forceUpdate;
    }

    public void setForceUpdate(Integer forceUpdate) {
        this.forceUpdate = forceUpdate;
    }

    public String getUpdateDesc() {
        return updateDesc;
    }

    public void setUpdateDesc(String updateDesc) {
        this.updateDesc = updateDesc;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
