/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.organization.OrganizationServiceProvider;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.organization.model.OrganizationRelationship;
import cn.wildfirechat.client.ConnectionStatus;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnConnectionStatusChangeListener;

public class OrganizationServiceViewModel extends ViewModel implements OnConnectionStatusChangeListener {
    private static final String TAG = OrganizationServiceViewModel.class.getSimpleName();
    private MutableLiveData<Object> organizationServiceAvailableLiveData;
    private MutableLiveData<List<Organization>> rootOrganizationLiveData;
    private MutableLiveData<List<Organization>> myOrganizationLiveData;
    private final OrganizationServiceProvider organizationServiceProvider;

    public OrganizationServiceViewModel() {
        ChatManager.Instance().addConnectionChangeListener(this);
        organizationServiceAvailableLiveData = new MutableLiveData<>();

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
