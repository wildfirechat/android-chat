/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.organization.OrganizationServiceProvider;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.EmployeeEx;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.organization.model.OrganizationEx;
import cn.wildfire.chat.kit.organization.model.OrganizationRelationship;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GeneralCallback2;

public class OrganizationService implements OrganizationServiceProvider {

    private boolean isServiceAvailable;

    private static final OrganizationService Instance = new OrganizationService();

    //组织通讯录服务地址，如果没有部署，可以设置为null
    public static String ORG_SERVER_ADDRESS/*请仔细阅读上面的注释*/ = Config.ORG_SERVER_ADDRESS;

    private OrganizationService() {

    }

    public static OrganizationService Instance() {
        return Instance;
    }

    @Override
    public void login(GeneralCallback callback) {
//        int ApplicationType_Robot = 0;
//        int ApplicationType_Channel = 1;
//        int ApplicationType_Admin = 2;
        if (isServiceAvailable) {
            return;
        }
        ChatManager.Instance().getAuthCode("admin", 2, Config.IM_SERVER_HOST, new GeneralCallback2() {
            @Override
            public void onSuccess(String result) {

                Map<String, Object> params = new HashMap<>(1);
                params.put("authCode", result);
                String url = ORG_SERVER_ADDRESS + "/api/user_login";
                OKHttpHelper.post(url, params, new SimpleCallback<Void>() {
                    @Override
                    public void onUiSuccess(Void r) {
                        Instance.isServiceAvailable = true;
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    }

                    @Override
                    public void onUiFailure(int code, String msg) {
                        if (callback != null) {
                            callback.onFail(code);
                        }
                    }
                });
            }

            @Override
            public void onFail(int errorCode) {
                if (callback != null) {
                    callback.onFail(errorCode);
                }
            }
        });

    }

    public void reset() {
        this.isServiceAvailable = false;
    }

    public void clearOrgServiceAuthInfos() {

    }

    @Override
    public boolean isServiceAvailable() {
        return isServiceAvailable;
    }

    @Override
    public void getRelationship(String employeeId, SimpleCallback<List<OrganizationRelationship>> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("employeeId", employeeId);
        String url = ORG_SERVER_ADDRESS + "/api/relationship/employee";
        OKHttpHelper.post(url, params, callback);

    }

    @Override
    public void getRootOrganization(SimpleCallback<List<Organization>> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        String url = ORG_SERVER_ADDRESS + "/api/organization/root";
        OKHttpHelper.post(url, params, callback);
    }

    @Override
    public void getOrganizationEx(int orgId, SimpleCallback<OrganizationEx> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("id", orgId);
        String url = ORG_SERVER_ADDRESS + "/api/organization/query_ex";
        OKHttpHelper.post(url, params, callback);
    }

    @Override
    public void getOrganizations(List<Integer> orgIds, SimpleCallback<List<Organization>> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("ids", orgIds);
        String url = ORG_SERVER_ADDRESS + "/api/organization/query_list";
        OKHttpHelper.post(url, params, callback);
    }

    @Override
    public void getOrganizationEmployees(List<Integer> orgIds, SimpleCallback<List<Employee>> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        getOrgEmployees(orgIds, new SimpleCallback<List<String>>() {
            @Override
            public void onUiSuccess(List<String> employeeIds) {
                if (employeeIds != null && !employeeIds.isEmpty()) {
                    getEmployees(employeeIds, callback);
                } else {
                    callback.onUiSuccess(new ArrayList<>());
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                callback.onUiFailure(code, msg);
            }
        });
    }


    @Override
    public void getOrgEmployees(List<Integer> orgIds, SimpleCallback<List<String>> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("ids", orgIds);
        String url = ORG_SERVER_ADDRESS + "/api/organization/batch_employees";
        OKHttpHelper.post(url, params, callback);
    }

    @Override
    public void getOrgEmployees(int orgId, SimpleCallback<List<String>> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("id", orgId);
        String url = ORG_SERVER_ADDRESS + "/api/organization/employees";
        OKHttpHelper.post(url, params, callback);
    }

    @Override
    public void getEmployee(String employeeId, SimpleCallback<Employee> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("employeeId", employeeId);
        String url = ORG_SERVER_ADDRESS + "/api/employee/query";
        OKHttpHelper.post(url, params, callback);
    }

    @Override
    public void getEmployees(List<String> employeeIds, SimpleCallback<List<Employee>> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("employeeIds", employeeIds);
        String url = ORG_SERVER_ADDRESS + "/api/employee/query_list";
        OKHttpHelper.post(url, params, callback);
    }

    @Override
    public void getEmployeeEx(String employeeId, SimpleCallback<EmployeeEx> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("employeeId", employeeId);
        String url = ORG_SERVER_ADDRESS + "/api/employee/query_ex";
        OKHttpHelper.post(url, params, callback);
    }

    @Override
    public void searchEmployee(int orgId, String keyword, SimpleCallback<List<Employee>> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("organizationId", orgId);
        params.put("keyword", keyword);
        String url = ORG_SERVER_ADDRESS + "/api/employee/search";
        OKHttpHelper.post(url, params, callback);
    }

    private static final String TAG = "OrgService";

}
