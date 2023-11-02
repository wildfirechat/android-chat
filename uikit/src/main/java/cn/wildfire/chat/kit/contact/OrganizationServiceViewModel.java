/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.organization.OrganizationServiceProvider;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.EmployeeEx;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.organization.model.OrganizationEx;
import cn.wildfire.chat.kit.organization.model.OrganizationRelationship;
import cn.wildfirechat.client.ConnectionStatus;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnConnectionStatusChangeListener;

public class OrganizationServiceViewModel extends ViewModel implements OnConnectionStatusChangeListener {
    private static final String TAG = OrganizationServiceViewModel.class.getSimpleName();
    private MutableLiveData<List<Organization>> rootOrganizationLiveData;
    private MutableLiveData<List<Organization>> myOrganizationLiveData;
    private MutableLiveData<List<Employee>> organizationEmployeeLiveData;
    private final OrganizationServiceProvider organizationServiceProvider;

    public OrganizationServiceViewModel() {
        ChatManager.Instance().addConnectionChangeListener(this);

        organizationServiceProvider = WfcUIKit.getWfcUIKit().getOrganizationServiceProvider();

        if (organizationServiceProvider != null
            && !organizationServiceProvider.isServiceAvailable()
            && ChatManager.Instance().getConnectionStatus() == ConnectionStatus.ConnectionStatusConnected) {
            login();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ChatManager.Instance().removeConnectionChangeListener(this);
    }

    @Override
    public void onConnectionStatusChange(int status) {
        if (status == ConnectionStatus.ConnectionStatusConnected) {
            login();
        }
    }

    public MutableLiveData<List<Organization>> rootOrganizationLiveData() {
        if (rootOrganizationLiveData == null) {
            rootOrganizationLiveData = new MutableLiveData<>();
        }
        if (organizationServiceProvider.isServiceAvailable()) {
            loadRootOrganizations();
        }
        return rootOrganizationLiveData;
    }

    public MutableLiveData<List<Organization>> myOrganizationLiveData() {
        if (myOrganizationLiveData == null) {
            myOrganizationLiveData = new MutableLiveData<>();
        }
        if (organizationServiceProvider.isServiceAvailable()) {
            loadMyOrganizations();
        }

        return myOrganizationLiveData;
    }

    public MutableLiveData<List<String>> getOrganizationEmployees(int orgId) {
        MutableLiveData<List<String>> liveData = new MutableLiveData<>();
        organizationServiceProvider.getOrgEmployees(orgId, new SimpleCallback<List<String>>() {
            @Override
            public void onUiSuccess(List<String> strings) {
                liveData.postValue(strings);
            }

            @Override
            public void onUiFailure(int code, String msg) {

            }
        });
        return liveData;
    }

    public MutableLiveData<List<Organization>> getOrganizations(List<Integer> ids) {
        MutableLiveData<List<Organization>> liveData = new MutableLiveData<>();
        organizationServiceProvider.getOrganizations(ids, new SimpleCallback<List<Organization>>() {
            @Override
            public void onUiSuccess(List<Organization> organizations) {
                liveData.postValue(organizations);
            }

            @Override
            public void onUiFailure(int code, String msg) {

            }
        });
        return liveData;
    }

    public MutableLiveData<OrganizationEx> getOrganizationEx(int organizationId) {
        MutableLiveData<OrganizationEx> liveData = new MutableLiveData<>();
        OrganizationServiceProvider organizationServiceProvider = WfcUIKit.getWfcUIKit().getOrganizationServiceProvider();
        organizationServiceProvider.getOrganizationEx(organizationId, new SimpleCallback<OrganizationEx>() {
            @Override
            public void onUiSuccess(OrganizationEx organizationEx) {
                liveData.setValue(organizationEx);
            }

            @Override
            public void onUiFailure(int code, String msg) {

            }
        });
        return liveData;
    }

    public MutableLiveData<List<Employee>> getOrganizationEmployees(List<Integer> organizationIds, boolean includeSubOrganization) {
        MutableLiveData<List<Employee>> liveData = new MutableLiveData<>();

        List<Employee> employees = new ArrayList<>();
        ChatManager.Instance().getWorkHandler().post(new Runnable() {
            @Override
            public void run() {
                CountDownLatch latch = new CountDownLatch(organizationIds.size());
                for (Integer orgId : organizationIds) {
                    organizationServiceProvider.getOrganizationEx(orgId, new SimpleCallback<OrganizationEx>() {
                        @Override
                        public void onUiSuccess(OrganizationEx organizationEx) {
                            if (organizationEx != null && organizationEx.employees != null) {
                                employees.addAll(organizationEx.employees);
                            }
                            latch.countDown();
                        }

                        @Override
                        public void onUiFailure(int code, String msg) {
                            latch.countDown();
                        }
                    });
                }
                try {
                    latch.await();
                    liveData.postValue(employees);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return liveData;
    }

    public MutableLiveData<List<Organization>> getOrganizationPath(int organizationId) {
        MutableLiveData<List<Organization>> liveData = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(new Runnable() {
            @Override
            public void run() {
                List<Organization> pathList = new ArrayList<>();
                getOrganizationPathSync(organizationId, pathList);
                Log.d("PickOrgMemberFragment", "pathList " + organizationId + " " + pathList.size());
                liveData.postValue(pathList);
            }
        });

        return liveData;
    }

    public MutableLiveData<EmployeeEx> getEmployeeEx(String userId) {
        MutableLiveData<EmployeeEx> liveData = new MutableLiveData<>();
        organizationServiceProvider.getEmployeeEx(userId, new SimpleCallback<EmployeeEx>() {
            @Override
            public void onUiSuccess(EmployeeEx employeeEx) {
                liveData.postValue(employeeEx);

            }

            @Override
            public void onUiFailure(int code, String msg) {

            }
        });
        return liveData;
    }

    private void getOrganizationPathSync(int orgId, List<Organization> outPathList) {
        Organization org = getOrganizationSync(orgId);
        if (org != null) {
            outPathList.add(0, org);
            if (org.parentId != 0) {
                getOrganizationPathSync(org.parentId, outPathList);
            }
        }
    }

    private Organization getOrganizationSync(int orgId) {
        CountDownLatch latch = new CountDownLatch(1);
        final Organization[] orgArr = new Organization[1];
        organizationServiceProvider.getOrganizations(Collections.singletonList(orgId), new SimpleCallback<List<Organization>>() {
            @Override
            public void onUiSuccess(List<Organization> organizations) {
                if (!organizations.isEmpty()) {
                    orgArr[0] = organizations.get(0);
                }
                latch.countDown();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                latch.countDown();
                Log.e(TAG, "getOrganizations error " + code + " " + msg);
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return orgArr[0];
    }

    private void login() {
        if (organizationServiceProvider != null && !organizationServiceProvider.isServiceAvailable()) {
            organizationServiceProvider.login(new GeneralCallback() {
                @Override
                public void onSuccess() {
                    if (rootOrganizationLiveData != null) {
                        loadRootOrganizations();
                        loadMyOrganizations();
                    }
                }

                @Override
                public void onFail(int errorCode) {
                    Log.e(TAG, "login failed " + errorCode);

                }
            });
        }
    }


    private void loadRootOrganizations() {
        organizationServiceProvider.getRootOrganization(new SimpleCallback<List<Organization>>() {
            @Override
            public void onUiSuccess(List<Organization> organizations) {
                rootOrganizationLiveData.setValue(organizations);
            }

            @Override
            public void onUiFailure(int code, String msg) {

            }
        });
    }

    private void loadMyOrganizations() {
        organizationServiceProvider.getRelationship(ChatManager.Instance().getUserId(), new SimpleCallback<List<OrganizationRelationship>>() {
            @Override
            public void onUiSuccess(List<OrganizationRelationship> organizationRelationships) {
                List<Integer> orgIds = new ArrayList<>();
                if (organizationRelationships != null && !organizationRelationships.isEmpty()) {
                    for (OrganizationRelationship r : organizationRelationships) {
                        if (r.bottom) {
                            orgIds.add(r.organizationId);
                        }
                    }
                    if (!orgIds.isEmpty()) {
                        organizationServiceProvider.getOrganizations(orgIds, new SimpleCallback<List<Organization>>() {
                            @Override
                            public void onUiSuccess(List<Organization> organizations) {
                                myOrganizationLiveData.setValue(organizations);
                            }

                            @Override
                            public void onUiFailure(int code, String msg) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {

            }
        });
    }
}
