/*
 * Copyright (c) Open Source Strategies, Inc.
 *
 * Opentaps is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opentaps is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Opentaps.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.opentaps.foundation.infrastructure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;


/**
 * A general concept of User to be used across all the frameworks.  One goal of this class is to translate User from one
 * framework to another.  For example, you should be able to create a user from
 * the ofbiz UserLogin GenericValue, then pass it to another framework.  This class does not extend UserLogin because that
 * is a data model entity of a user login, whereas this is a general concept of user
 */
public class User {

    private GenericValue ofbizUserLogin;
    private Delegator delegator;
    private Set<String> permissions;

    /**
     * Default constructor.
     */
    public User() { }

    /**
     * Creates a new <code>User</code> instance.
     * @param userLogin a <code>GenericValue</code> value
     */
    public User(GenericValue userLogin) {
        this();
        setOfbizUserLogin(userLogin);
        setDelegator(userLogin.getDelegator());
    }

    /**
     * Creates a new <code>User</code> instance.
     * @param userLogin a <code>GenericValue</code> value
     * @param delegator a <code>Delegator</code> value
     */
    public User(GenericValue userLogin, Delegator delegator) {
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
     * @param delegator a <code>Delegator</code> value
     */
    public void setDelegator(Delegator delegator) {
        this.delegator = delegator;
    }

    /**
     * Sets the ofbiz userlogin, as long as it is a "UserLogin" GenericValue object.
     * @param ofbizUserLogin the ofbiz <code>UserLogin</code>
     * @throws IllegalArgumentException if the given value is null or not a UserLogin
     */
    public void setOfbizUserLogin(GenericValue ofbizUserLogin) throws IllegalArgumentException {
        if (ofbizUserLogin == null) {
            throw new IllegalArgumentException("Cannot create User from null ofbiz UserLogin");
        }
        ModelEntity model = ofbizUserLogin.getModelEntity();
        if ("UserLogin".equals(model.getEntityName())) {
            this.ofbizUserLogin = ofbizUserLogin;
        } else {
            throw new IllegalArgumentException("Cannot create User from [" + ofbizUserLogin + "] which is not a UserLogin value.");
        }
    }

    /**
     * Returns the unique identifier of this <code>User</code> object.
     * @return an unique identifier
     * @throws InfrastructureException if an error occurs
     */
    public String getUserId() throws InfrastructureException {
        if (getOfbizUserLogin() != null) {
            return getOfbizUserLogin().getString("userLoginId");
        } else {
            // handle other types of user identifiers as they become available
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
