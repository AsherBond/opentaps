package org.opentaps.foundation.infrastructure;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import java.util.HashSet;
import java.util.Set;
import org.ofbiz.entity.GenericDelegator;
import java.util.List;
import org.ofbiz.entity.GenericEntityException;

/*
* Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
* 
* This program is free software; you can redistribute it and/or modify
* it under the terms of the Honest Public License.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* Honest Public License for more details.
* 
* You should have received a copy of the Honest Public License
* along with this program; if not, write to Funambol,
* 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
*/

/**
 * A general concept of User to be used across all the frameworks.  One goal of this class is to translate User from one
 * framework to another.  For example, you should be able to create a user from
 * the ofbiz UserLogin GenericValue, then pass it to another framework.  This class does not extend UserLogin because that
 * is a data model entity of a user login, whereas this is a general concept of user
 */
public class User {

    private GenericValue ofbizUserLogin;
    private GenericDelegator delegator;
    private Set<String> permissions;

    /**
     * Default constructor.
     */
    public User() {
        //
    }

    public User(GenericValue userLogin) throws InfrastructureException {
        this();
        setOfbizUserLogin(userLogin);
    }

    public User(GenericValue userLogin, GenericDelegator delegator) throws InfrastructureException {
        this();
        setOfbizUserLogin(userLogin);
        setDelegator(delegator);
    }

    /**
     * Gets the ofbiz <code>UserLogin</code>.
     * @return the ofbiz <code>UserLogin</code>
     */
    public GenericValue getOfbizUserLogin() {
        return ofbizUserLogin;
    }

    /**
     * Sets the delegator.
     * Some methods need a delegator to function properly.
     * @param delegator a <code>GenericDelegator</code> value
     */
    public void setDelegator(GenericDelegator delegator) {
        this.delegator = delegator;
    }

    /**
     * Sets the ofbiz userlogin, as long as it is a "UserLogin" GenericValue object.
     * @param ofbizUserLogin the ofbiz <code>UserLogin</code>
     * @throws InfrastructureException if an error occurs
     */
    public void setOfbizUserLogin(GenericValue ofbizUserLogin) throws InfrastructureException {
        if (ofbizUserLogin == null) {
            throw new InfrastructureException("Cannot create User from null ofbiz UserLogin");
        }
        ModelEntity model = ofbizUserLogin.getModelEntity();
        if ("UserLogin".equals(model.getEntityName())) {
            this.ofbizUserLogin = ofbizUserLogin;
        } else {
            throw new InfrastructureException("Cannot create User from [" + ofbizUserLogin + "]");
        }
    }

    /**
     * Return unique identifier of the User object
     * @return
     * @throws InfrastructureException
     */
    public String getUserId() throws InfrastructureException {
        if (getOfbizUserLogin() != null) {
            return getOfbizUserLogin().getString("userLoginId");
        }
        // handle other types of user identifiers as they become available
          else {
            throw new InfrastructureException("User has no known identifier");
        }
    }

    /**
     * Gets the set of base permissions that apply to this user.
     * @return the set of base permissions that apply to this use
     * @throws InfrastructureException if an error occurs
     */
    public Set<String> getPermissions() throws InfrastructureException {
        if (permissions == null) {

            if (delegator == null) {
                throw new InfrastructureException("No delegator has been set, cannot check permissions.");
            }

            try {
                permissions = new HashSet<String>();
                // if no user is set, then return an empty permission set
                if (ofbizUserLogin != null) {
                    List<GenericValue> groupsGV = ofbizUserLogin.getRelated("UserLoginSecurityGroup");
                    for (GenericValue groupGV : groupsGV) {
                        List<GenericValue> permissionsGV = groupGV.getRelated("SecurityGroupPermission");
                        for (GenericValue permissionGV : permissionsGV) {
                            permissions.add(permissionGV.getString("permissionId"));
                        }
                    }
                }
            } catch (GenericEntityException e) {
                permissions = null;
                throw new InfrastructureException(e);
            }

        }
        return permissions;
    }

    /**
     * Checks if this user has the given permission for the given module or application.
     * @param module the module
     * @param permission the permission
     * @return a <code>Boolean</code> value
     * @throws InfrastructureException if an error occurs
     */
    public Boolean hasPermission(String module, String permission) throws InfrastructureException {
        return getPermissions().contains(module + "_" + permission);
    }

    /**
     * Checks if this user has admin permissions for the given module or application.
     * @param module the module
     * @return a <code>Boolean</code> value
     * @throws InfrastructureException if an error occurs
     */
    public Boolean hasAdminPermissionsForModule(String module) throws InfrastructureException {
        return getPermissions().contains(module + "_ADMIN");
    }

}
