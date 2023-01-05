/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app;

import java.util.List;

import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.organization.Employee;
import cn.wildfire.chat.kit.organization.EmployeeEx;
import cn.wildfire.chat.kit.organization.Organization;
import cn.wildfire.chat.kit.organization.OrganizationEx;
import cn.wildfire.chat.kit.organization.OrganizationRelationship;
import cn.wildfire.chat.kit.organization.OrganizationServiceProvider;

public class OrgService implements OrganizationServiceProvider {

    private boolean isServiceAvailable;

    private static final OrgService Instance = new OrgService();

    //组织通讯录服务地址，如果没有部署，可以设置为null
    public static String ORG_SERVER_ADDRESS/*请仔细阅读上面的注释*/ = "https://org.wildfirechat.cn";

    private OrgService() {

    }

    public static OrgService Instance() {
        return Instance;
    }

    public void login() {

    }

    public void clearOrgServiceAuthInfos() {

    }

    public boolean isServiceAvailable() {
        return isServiceAvailable;
    }

    @Override
    public void getRelationship(String employeeId, SimpleCallback<OrganizationRelationship> callback) {

    }

    @Override
    public void getRootOrganization(SimpleCallback<OrganizationRelationship> callback) {

    }

    @Override
    public void getOrganizationEx(int orgId, SimpleCallback<OrganizationEx> callback) {

    }

    @Override
    public void getOrganizations(List<Integer> orgIds, SimpleCallback<List<Organization>> callback) {

    }

    @Override
    public void batchGetOrgEmployees(List<Integer> orgIds, SimpleCallback<List<String>> callback) {

    }

    @Override
    public void getOrgEmployees(int orgId, SimpleCallback<List<String>> callback) {

    }

    @Override
    public void getEmployee(String employeeId, SimpleCallback<Employee> callback) {

    }

    @Override
    public void getEmployeeEx(String employeeId, SimpleCallback<EmployeeEx> callback) {

    }

    @Override
    public void searchEmployee(int orgId, String keyword, SimpleCallback<List<Employee>> callback) {

    }
}
