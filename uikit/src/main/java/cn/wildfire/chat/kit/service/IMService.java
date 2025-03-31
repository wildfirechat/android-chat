/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.service;

import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.domain.TGroupJoinRequests;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.net.base.StatusResult;
import cn.wildfirechat.client.GroupMemberSource;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GeneralCallback4;
import cn.wildfirechat.remote.GeneralCallback5;

public class IMService {
    private static final IMService Instance = new IMService();

    /**
     * App Server默认使用的是8888端口，替换为自己部署的服务时需要注意端口别填错了
     * <br>
     * 这是个 http 地址，http 前缀不能省略，否则会提示配置错误，然后直接退出
     * <br>
     * 正式商用时，建议用https，确保token安全
     * <br>
     * <br>
     */
    public static String RY_SERVER_ADDRESS/*请仔细阅读上面的注释*/ = "http://1.94.177.77:8080";

    private IMService() {

    }

    public static IMService Instance() {
        return Instance;
    }


    public void submitGroupApply(String groupId, String userId, String reason, GeneralCallback callback) {
        String url = RY_SERVER_ADDRESS + "/api/groupJoin";
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);
        params.put("applicantId", userId);
        params.put("remark", reason);

        OKHttpHelper.post(url, params, new SimpleCallback<StatusResult>() {
            @Override
            public void onUiSuccess(StatusResult response) {
                Log.d("weiAndKe", "onUiSuccess: " + response.isSuccess()); // 确认 code 值
                callback.onSuccess();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Log.e("weiAndKe", "onUiFailure - HTTP状态码: " + code + ", 错误信息: " + msg);
                callback.onFail(code);
            }
        });
    }

    public void getGroupApplyInfo(String userId, GeneralCallback4<TGroupJoinRequests> callback) {
        String url = RY_SERVER_ADDRESS + "/api/groupJoin/getMembersByAdminId";
        Map<String, Object> params = new HashMap<>();
        params.put("operatorId", userId);

        OKHttpHelper.post(url, params, new SimpleCallback<List<TGroupJoinRequests>>() {

            @Override
            public void onUiSuccess(List<TGroupJoinRequests> listResultWrapper) {
              /*  for (TGroupJoinRequests req : listResultWrapper) {
                    GroupInfo groupInfo = ChatManager.Instance().getGroupInfo(req.getGroupId(), false);
                    req.set
                    Log.e("weiAndKe", "listResultWrapper" + req.toString());
                }*/

                callback.onSuccess(listResultWrapper);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Log.e("weiAndKe", "onUiFailure11 - HTTP状态码: " + code + ", 错误信息: " + msg);
                callback.onFail(code);
            }
        });
    }

    public void getGroupApplyCount(String userId, GeneralCallback5<Integer> generalCallback5) {
        String url = RY_SERVER_ADDRESS + "/api/groupJoin/getMembersCountByAdminId";
        Map<String, Object> params = new HashMap<>();
        params.put("operatorId", userId);
        OKHttpHelper.post(url, params, new SimpleCallback<Integer>() {
            @Override
            public void onUiSuccess(Integer result) {
                generalCallback5.onSuccess(result);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Log.e("weiAndKe", "onUiFailure22 - HTTP状态码: " + code + ", 错误信息: " + msg);
                generalCallback5.onFail(code);
            }
        });
    }

    public void approveGroupJoinRequest(TGroupJoinRequests groupJoinRequests, GeneralCallback callback) {
        String url;
        if (groupJoinRequests == null) {
            return;
        }
        groupJoinRequests.setApplyTime(null);
        long status = groupJoinRequests.getStatus();
        if (status == 1L) {
            //同意
            url = RY_SERVER_ADDRESS + "/api/groupJoin/justApprove";
            String memberExtra = GroupMemberSource.buildGroupMemberSourceExtra(GroupMemberSource.Type_QRCode, "审批");
            ChatManager.Instance().addGroupMembers(groupJoinRequests.getGroupId(), Collections.singletonList(groupJoinRequests.getApplicantId()), memberExtra, Collections.singletonList(0), null, new GeneralCallback() {
                @Override
                public void onSuccess() {
                    callback.onSuccess();
                }

                @Override
                public void onFail(int errorCode) {
                    callback.onFail(errorCode);
                }
            });
            OKHttpHelper.post(url, groupJoinRequests, new SimpleCallback<String>() {
                @Override
                public void onUiSuccess(String v) {
                }

                @Override
                public void onUiFailure(int code, String msg) {
                }
            });
        } else if (status == 2L) {
            //拒绝
            url = RY_SERVER_ADDRESS + "/api/groupJoin/rejected";
            OKHttpHelper.post(url, groupJoinRequests, new SimpleCallback<String>() {
                @Override
                public void onUiSuccess(String v) {
                    callback.onSuccess();
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    callback.onFail(code);
                }
            });
        }
    }
}
