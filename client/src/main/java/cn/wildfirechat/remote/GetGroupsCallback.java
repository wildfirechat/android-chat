package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.GroupInfo;

public interface GetGroupsCallback {

    void onSuccess(List<GroupInfo> groupInfos);

    void onFail(int errorCode);
}
