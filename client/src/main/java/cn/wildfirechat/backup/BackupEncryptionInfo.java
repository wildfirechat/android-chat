package cn.wildfirechat.backup;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 备份加密信息
 */
public class BackupEncryptionInfo {
    private boolean enabled;
    private String algorithm;
    private String keyDerivation;
    private String passwordHint;

    public BackupEncryptionInfo() {
        this.enabled = false;
        this.algorithm = BackupConstants.ENCRYPTION_ALGORITHM;
        this.keyDerivation = BackupConstants.KEY_DERIVATION;
        this.passwordHint = "";
    }

    public BackupEncryptionInfo(boolean enabled, String passwordHint) {
        this.enabled = enabled;
        this.algorithm = BackupConstants.ENCRYPTION_ALGORITHM;
        this.keyDerivation = BackupConstants.KEY_DERIVATION;
        this.passwordHint = passwordHint != null ? passwordHint : "";
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("enabled", enabled);
        json.put("algorithm", algorithm);
        json.put("keyDerivation", keyDerivation);
        json.put("passwordHint", passwordHint);
        return json;
    }

    public static BackupEncryptionInfo fromJSON(JSONObject json) throws JSONException {
        BackupEncryptionInfo info = new BackupEncryptionInfo();
        info.enabled = json.optBoolean("enabled", false);
        info.algorithm = json.optString("algorithm", BackupConstants.ENCRYPTION_ALGORITHM);
        info.keyDerivation = json.optString("keyDerivation", BackupConstants.KEY_DERIVATION);
        info.passwordHint = json.optString("passwordHint", "");
        return info;
    }

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getKeyDerivation() {
        return keyDerivation;
    }

    public void setKeyDerivation(String keyDerivation) {
        this.keyDerivation = keyDerivation;
    }

    public String getPasswordHint() {
        return passwordHint;
    }

    public void setPasswordHint(String passwordHint) {
        this.passwordHint = passwordHint;
    }
}
