/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.organization.model.Robot;
import cn.wildfirechat.remote.ChatManager;

/**
 * 组织架构机器人数据源。
 * <p>
 * 机器人来自组织通讯录服务，但界面（联系人入口、选人页）只依赖本类，不关心组织树：
 * <ul>
 *   <li>启动时先从 SharedPreferences 恢复上次的列表，界面立刻可用；</li>
 *   <li>本次启动登录后第一次拿到根组织时，按根公司逐个请求机器人接口，
 *       按根公司顺序合并、按 {@code robotId} 去重，与服务端返回顺序保持一致；</li>
 *   <li>结果写回缓存并通过 {@link #robotsLiveData()} 通知界面；</li>
 *   <li>全部请求失败时保留旧缓存，避免网络抖动把机器人入口清空。</li>
 * </ul>
 * 缓存按登录用户分 key，切换账号不会串数据；退出登录时由
 * {@link cn.wildfire.chat.kit.organization.OrganizationServiceProvider} 的实现调用 {@link #clear()} 清理。
 */
public class OrganizationRobotManager {
    private static final String TAG = "OrgRobotManager";
    private static final String SP_FILE_NAME = "wfc_org_robots";
    private static final String PREF_PREFIX = "robots_";
    private static final String PREF_DEFAULT_USER = "default";

    private static final OrganizationRobotManager INSTANCE = new OrganizationRobotManager();

    private final MutableLiveData<List<Robot>> robotsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final List<Robot> robots = new ArrayList<>();

    private String loadedUserId;
    private boolean requestedThisSession;
    private boolean loading;
    /** 用户切换/退出登录时自增，用来丢弃旧用户的在途请求结果 */
    private int generation;

    public static OrganizationRobotManager getInstance() {
        return INSTANCE;
    }

    private OrganizationRobotManager() {
        restore();
    }

    /**
     * 机器人列表。首次观察时如果恢复了缓存会立刻拿到上次的数据。
     */
    public LiveData<List<Robot>> robotsLiveData() {
        ensureUser();
        return robotsLiveData;
    }

    public List<Robot> getRobots() {
        ensureUser();
        synchronized (this) {
            return new ArrayList<>(robots);
        }
    }

    public boolean isEmpty() {
        ensureUser();
        synchronized (this) {
            return robots.isEmpty();
        }
    }

    public Set<String> getRobotIdSet() {
        Set<String> ids = new HashSet<>();
        for (Robot robot : getRobots()) {
            if (!TextUtils.isEmpty(robot.robotId)) {
                ids.add(robot.robotId);
            }
        }
        return ids;
    }

    /**
     * 根组织请求成功后调用。多公司时按根公司顺序逐个请求并合并去重。
     * <p>
     * 只在本次启动第一次成功拿到根组织时真正发请求；如果一批请求全部失败，
     * 允许后续再次触发的根组织请求重试。
     */
    public void loadCompanyRobots(List<Organization> companies) {
        if (companies == null || companies.isEmpty()) {
            return;
        }
        synchronized (this) {
            if (requestedThisSession || loading) {
                return;
            }
        }
        OrganizationServiceProvider provider = WfcUIKit.getWfcUIKit().getOrganizationServiceProvider();
        if (provider == null || !provider.isServiceAvailable()) {
            return;
        }

        List<Integer> companyIds = new ArrayList<>();
        for (Organization company : companies) {
            if (company != null && company.id > 0 && !companyIds.contains(company.id)) {
                companyIds.add(company.id);
            }
        }
        if (companyIds.isEmpty()) {
            return;
        }

        synchronized (this) {
            requestedThisSession = true;
            loading = true;
        }

        final Map<Integer, List<Robot>> robotMap = new HashMap<>();
        final int[] remaining = {companyIds.size()};
        final int[] successCount = {0};
        final int requestGeneration;
        synchronized (this) {
            requestGeneration = generation;
        }

        for (Integer companyId : companyIds) {
            provider.getCompanyRobots(companyId, new SimpleCallback<List<Robot>>() {
                @Override
                public void onUiSuccess(List<Robot> result) {
                    synchronized (OrganizationRobotManager.this) {
                        // 用户切换/退出登录后丢弃上一轮在途请求
                        if (requestGeneration != generation) {
                            return;
                        }
                        robotMap.put(companyId, result == null ? new ArrayList<>() : result);
                        successCount[0]++;
                        if (--remaining[0] == 0) {
                            finishLoad(companyIds, robotMap, successCount[0] > 0, requestGeneration);
                        }
                    }
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    Log.e(TAG, "getCompanyRobots error " + companyId + " " + code + " " + msg);
                    synchronized (OrganizationRobotManager.this) {
                        if (requestGeneration != generation) {
                            return;
                        }
                        if (--remaining[0] == 0) {
                            finishLoad(companyIds, robotMap, successCount[0] > 0, requestGeneration);
                        }
                    }
                }
            });
        }
    }

    private void finishLoad(List<Integer> companyIds, Map<Integer, List<Robot>> robotMap,
                            boolean anySuccess, int requestGeneration) {
        if (requestGeneration != generation) {
            return;
        }
        loading = false;
        if (!anySuccess) {
            // 全部失败：保留本地缓存，且允许后续根组织请求重试
            requestedThisSession = false;
            return;
        }

        List<Robot> merged = new ArrayList<>();
        Set<String> robotIds = new HashSet<>();
        for (Integer companyId : companyIds) {
            List<Robot> companyRobots = robotMap.get(companyId);
            if (companyRobots == null) {
                continue;
            }
            for (Robot robot : companyRobots) {
                if (robot != null && !TextUtils.isEmpty(robot.robotId) && robotIds.add(robot.robotId)) {
                    merged.add(robot);
                }
            }
        }

        synchronized (this) {
            robots.clear();
            robots.addAll(merged);
        }
        save(merged);
        publish(new ArrayList<>(merged));
    }

    /**
     * 退出登录时清理当前用户的缓存，下次登录重新请求。
     */
    public void clear() {
        String userId = loadedUserId;
        if (TextUtils.isEmpty(userId)) {
            userId = currentUserId();
        }
        SharedPreferences sp = preferences();
        if (sp != null) {
            sp.edit().remove(PREF_PREFIX + (TextUtils.isEmpty(userId) ? PREF_DEFAULT_USER : userId)).apply();
        }
        synchronized (this) {
            generation++;
            robots.clear();
            requestedThisSession = false;
            loading = false;
            loadedUserId = null;
        }
        publish(new ArrayList<>());
    }

    // ==================== 缓存 ====================

    private void ensureUser() {
        String userId = currentUserId();
        synchronized (this) {
            if (TextUtils.equals(userId, loadedUserId)) {
                return;
            }
        }
        restore();
    }

    private void restore() {
        String userId = currentUserId();
        List<Robot> restored = new ArrayList<>();
        SharedPreferences sp = preferences();
        if (sp != null) {
            String json = sp.getString(PREF_PREFIX + (TextUtils.isEmpty(userId) ? PREF_DEFAULT_USER : userId), null);
            if (!TextUtils.isEmpty(json)) {
                try {
                    JSONArray array = new JSONArray(json);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.optJSONObject(i);
                        if (obj == null) {
                            continue;
                        }
                        Robot robot = new Robot();
                        robot.companyId = obj.optInt("companyId");
                        robot.robotId = obj.optString("robotId");
                        robot.robotName = obj.optString("robotName");
                        robot.portraitUrl = obj.optString("portraitUrl");
                        robot.sort = obj.optInt("sort");
                        robot.createdAt = obj.optLong("createdAt");
                        if (!TextUtils.isEmpty(robot.robotId)) {
                            restored.add(robot);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "restore robots error", e);
                }
            }
        }
        synchronized (this) {
            // 用户变了：之前用户的在途请求结果不能再落到当前用户上
            generation++;
            loadedUserId = userId;
            robots.clear();
            robots.addAll(restored);
        }
        publish(new ArrayList<>(restored));
    }

    private void save(List<Robot> list) {
        SharedPreferences sp = preferences();
        if (sp == null) {
            return;
        }
        String userId = currentUserId();
        try {
            JSONArray array = new JSONArray();
            for (Robot robot : list) {
                JSONObject obj = new JSONObject();
                obj.put("companyId", robot.companyId);
                obj.put("robotId", robot.robotId == null ? "" : robot.robotId);
                obj.put("robotName", robot.robotName == null ? "" : robot.robotName);
                obj.put("portraitUrl", robot.portraitUrl == null ? "" : robot.portraitUrl);
                obj.put("sort", robot.sort);
                obj.put("createdAt", robot.createdAt);
                array.put(obj);
            }
            sp.edit().putString(PREF_PREFIX + (TextUtils.isEmpty(userId) ? PREF_DEFAULT_USER : userId), array.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "save robots error", e);
        }
    }

    private SharedPreferences preferences() {
        try {
            Context context = WfcUIKit.getWfcUIKit().getApplication();
            return context == null ? null : context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        } catch (Exception e) {
            Log.e(TAG, "get SharedPreferences error", e);
            return null;
        }
    }

    private String currentUserId() {
        try {
            return ChatManager.Instance().getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private void publish(List<Robot> list) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            robotsLiveData.setValue(list);
        } else {
            robotsLiveData.postValue(list);
        }
    }
}
