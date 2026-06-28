/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.net.Callback;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.organization.OrganizationServiceProvider;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.EmployeeEx;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.organization.model.OrganizationEx;
import cn.wildfire.chat.kit.organization.model.OrganizationRelationship;
import cn.wildfire.chat.kit.organization.model.PageResponse;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GeneralCallback2;

public class OrganizationService implements OrganizationServiceProvider {

    private boolean isServiceAvailable;

    private static final OrganizationService Instance = new OrganizationService();

    //组织通讯录服务地址，如果没有部署，可以设置为null
    public static String ORG_SERVER_ADDRESS/*请仔细阅读上面的注释*/ = Config.ORG_SERVER_ADDRESS;
    public static String ORG_SERVER_BACKUP_ADDRESS = Config.ORG_SERVER_BACKUP_ADDRESS;

    private String getOrgServerAddress() {
        return Config.selectServer(ORG_SERVER_ADDRESS, ORG_SERVER_BACKUP_ADDRESS);
    }

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
                String url = getOrgServerAddress() + "/api/user_login";
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

    private void fillDefaultPortrait(Employee employee) {
        if (employee != null && (TextUtils.isEmpty(employee.portraitUrl) || !employee.portraitUrl.startsWith("http"))) {
            employee.portraitUrl = AppService.Instance().appServerAddress() + "/avatar?name=" + Uri.encode(employee.name);
        }
    }

    private void fillDefaultPortrait(List<?> list) {
        if (list == null) {
            return;
        }
        for (Object obj : list) {
            if (obj instanceof Employee) {
                fillDefaultPortrait((Employee) obj);
            }
        }
    }

    private void fillDefaultPortrait(OrganizationEx organizationEx) {
        if (organizationEx == null) {
            return;
        }
        fillDefaultPortrait(organizationEx.employees);
    }

    @Override
    public void getRelationship(String employeeId, SimpleCallback<List<OrganizationRelationship>> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("employeeId", employeeId);
        String url = getOrgServerAddress() + "/api/relationship/employee";
        OKHttpHelper.post(url, params, callback);

    }

    @Override
    public void getRootOrganization(SimpleCallback<List<Organization>> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        String url = getOrgServerAddress() + "/api/organization/root";
        OKHttpHelper.post(url, params, new SimpleCallback<List<Organization>>() {
            @Override
            public void onUiSuccess(List<Organization> organizations) {
                fillDefaultPortrait(organizations);
                callback.onUiSuccess(organizations);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                callback.onUiFailure(code, msg);
            }
        });
    }

    @Override
    public void getOrganizationEx(int orgId, SimpleCallback<OrganizationEx> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("id", orgId);
        String url = getOrgServerAddress() + "/api/organization/query_ex";
        OKHttpHelper.post(url, params, new SimpleCallback<OrganizationEx>() {
            @Override
            public void onUiSuccess(OrganizationEx organizationEx) {
                fillDefaultPortrait(organizationEx);
                callback.onUiSuccess(organizationEx);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                callback.onUiFailure(code, msg);
            }
        });
    }

    @Override
    public void getOrganizations(List<Integer> orgIds, SimpleCallback<List<Organization>> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("ids", orgIds);
        String url = getOrgServerAddress() + "/api/organization/query_list";
        OKHttpHelper.post(url, params, new SimpleCallback<List<Organization>>() {
            @Override
            public void onUiSuccess(List<Organization> organizations) {
                fillDefaultPortrait(organizations);
                callback.onUiSuccess(organizations);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                callback.onUiFailure(code, msg);
            }
        });
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
        String url = getOrgServerAddress() + "/api/organization/batch_employees";
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
        String url = getOrgServerAddress() + "/api/organization/employees";
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
        String url = getOrgServerAddress() + "/api/employee/query";
        OKHttpHelper.post(url, params, new SimpleCallback<Employee>() {
            @Override
            public void onUiSuccess(Employee employee) {
                fillDefaultPortrait(employee);
                callback.onUiSuccess(employee);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                callback.onUiFailure(code, msg);
            }
        });
    }

    @Override
    public void getEmployees(List<String> employeeIds, SimpleCallback<List<Employee>> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("employeeIds", employeeIds);
        String url = getOrgServerAddress() + "/api/employee/query_list";
        OKHttpHelper.post(url, params, new SimpleCallback<List<Employee>>() {
            @Override
            public void onUiSuccess(List<Employee> employees) {
                fillDefaultPortrait(employees);
                callback.onUiSuccess(employees);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                callback.onUiFailure(code, msg);
            }
        });
    }

    @Override
    public void getEmployeeEx(String employeeId, SimpleCallback<EmployeeEx> callback) {
        if (!isServiceAvailable) {
            callback.onUiFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("employeeId", employeeId);
        String url = getOrgServerAddress() + "/api/employee/query_ex";
        OKHttpHelper.post(url, params, new SimpleCallback<EmployeeEx>() {
            @Override
            public void onUiSuccess(EmployeeEx employeeEx) {
                if (employeeEx != null && employeeEx.employee != null) {
                    fillDefaultPortrait(employeeEx.employee);
                }
                callback.onUiSuccess(employeeEx);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                callback.onUiFailure(code, msg);
            }
        });
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
        params.put("count", 50);
        params.put("page", 0);
        String url = getOrgServerAddress() + "/api/employee/search";
        OKHttpHelper.post(url, params, new SimpleCallback<PageResponse<Employee>>() {
            @Override
            public void onUiSuccess(PageResponse<Employee> employees) {
                fillDefaultPortrait(employees.contents);
                callback.onUiSuccess(employees.contents);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                callback.onUiFailure(code, msg);
            }
        });
    }

    @Override
    public void searchEmployee(String keyword, Callback<List<Employee>> callback) {
        if (!isServiceAvailable) {
            callback.onFailure(-1, "未登录，或服务不可用");
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        String url = getOrgServerAddress() + "/api/organization/root";
        OKHttpHelper.post(url, params, new Callback<List<Organization>>() {
            @Override
            public void onSuccess(List<Organization> organizations) {
                List<Employee> ressults = new ArrayList<>();
                if (organizations != null) {
                    CountDownLatch latch = new CountDownLatch(organizations.size());
                    for (Organization org : organizations) {
                        Map<String, Object> params = new HashMap<>(1);
                        params.put("organizationId", org.id);
                        params.put("keyword", keyword);
                        params.put("count", 50);
                        params.put("page", 0);
                        String url = getOrgServerAddress() + "/api/employee/search";
                        OKHttpHelper.post(url, params, new Callback<PageResponse<Employee>>() {
                            @Override
                            public void onSuccess(PageResponse<Employee> employees) {
                                if (employees.contents != null) {
                                    fillDefaultPortrait(employees.contents);
                                    ressults.addAll(employees.contents);
                                }
                                latch.countDown();
                            }

                            @Override
                            public void onFailure(int code, String msg) {
                                Log.e(TAG, "search employee error " + code + " " + msg);
                                latch.countDown();
                            }
                        });
                    }
                    try {
                        latch.await();
                    } catch (InterruptedException ignored) {
                    }
                    callback.onSuccess(ressults);
                }
            }

            @Override
            public void onFailure(int code, String msg) {
                callback.onFailure(code, msg);
            }
        });
    }

    private static final String TAG = "OrgService";

}
