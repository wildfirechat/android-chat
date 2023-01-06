/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization;

import java.util.List;

import cn.wildfire.chat.kit.net.SimpleCallback;

public interface OrganizationServiceProvider {
    void getRelationship(String employeeId, SimpleCallback<OrganizationRelationship> callback);

    void getRootOrganization(SimpleCallback<List<Organization>> callback);

    void getOrganizationEx(int orgId, SimpleCallback<OrganizationEx> callback);

    void getOrganizations(List<Integer> orgIds, SimpleCallback<List<Organization>> callback);

    void getOrgEmployees(List<Integer> orgIds, SimpleCallback<List<String>> callback);

    void getOrgEmployees(int orgId, SimpleCallback<List<String>> callback);

    void getEmployee(String employeeId, SimpleCallback<Employee> callback);

    void getEmployeeEx(String employeeId, SimpleCallback<EmployeeEx> callback);

    void searchEmployee(int orgId, String keyword, SimpleCallback<List<Employee>> callback);

}
