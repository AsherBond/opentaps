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
/*
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.opentaps.common.security;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.lang.reflect.*;
import javax.servlet.http.HttpServletRequest;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;

/**
 * Generic security methods for all opentaps applications.
 * Each applicaiton should extend this and use their own security class.
 *
 * Instead of using static methods, this class is an object so that applications
 * can extend it and take advantage of inheritance benefits.  One benefit is
 * the ability to define required methods using interfaces or abstract methods.
 * Another is encapsulation.  A third is simplification of method calls.
 *
 * TODO: An alternative approach would be to extend Security or OFBizSecurity.
 * There's even a way to load this instead of OFBizSecurity in a properties file.
 * The main benefit of this approach is we have access to these methods wherever
 * ofbiz passes around the Security object.
 */
public class OpentapsSecurity {

    private static final String MODULE = OpentapsSecurity.class.getName();

    // cache of classes that extend OpentapsSecurity
    private static Map applicationSecurityCache = FastMap.newInstance();

    private Security security;
    private GenericValue userLogin;

    // prevent use of no argument constructor
    protected OpentapsSecurity() { }

    /**
     * Create a new OpentapsSecurity for the given Security and userLogin.
     */
    public OpentapsSecurity(Security security, GenericValue userLogin) {
        this.security = security;
        this.userLogin = userLogin;
    }

    /** Registers a subclass of OpentapsSecurity with the application name. */
    public static void registerApplicationSecurity(String applicationName, Class c) {
        applicationSecurityCache.put(applicationName, c);
    }

    /**
     * This method supplements the standard OFBIZ security model with a security check specified in PartyRelationship.
     * It first does the standard OFBIZ security checks, then sees if an unexpired PartyRelationship exists where partyIdFrom=partyIdFor,
     * partyIdTo=UserLogin.partyId, and whose securityGroupId contains the security permission of module+"_MANAGER" or module+"_OPERATION".
     * If not, it will check one more time on whether, for any partyIdFrom for which a security permission does exist, there exists
     * a current (unexpired) PartyRelationship where partyIdFrom=partyIdFor, partyIdTo={partyId for which the required permission exists.}
     * If any of these are true, then the permission is true.  Otherwise, or if any entity operation errors occurred, false is returned.
     *
     * @param   securityModule - The module to check (e.g., "CRMSFA_ACCOUNT", "PARTYMGR")
     * @param   securityOperation - What operation is being checked (e.g., "_VIEW", "_CREATE", "_UPDATE")
     * @param   partyIdFor - What Account or Party the userLogin has permission to perform the operation on
     */
    public boolean hasPartyRelationSecurity(String securityModule, String securityOperation, String partyIdFor) {

        if ((userLogin == null) || (userLogin.getDelegator() == null)) {
            Debug.logError("userLogin is null or has no associated delegator", MODULE);
            return false;
        }

        // check ${securityModule}_MANAGER permission
        if (security.hasEntityPermission(securityModule, "_MANAGER", userLogin)) {
            return true;
        }
        // check ${securityModule}_${securityOperation} permission
        if (security.hasEntityPermission(securityModule, securityOperation, userLogin)) {
            return true;
        }
        // TODO: #3 and #4 in http://jira.undersunconsulting.com/browse/OFBIZ-638

        try {
            // now we'll need to do some searching so we should get a delegator from user login
            Delegator delegator = userLogin.getDelegator();

            // validate that partyIdFor is in our system in a proper role
            /* XXX refactoring:  this won't work generally, so perhaps use an extra input parameter to pass in the list of roles to check for?
            String roleTypeIdFor = PartyHelper.getFirstValidRoleTypeId(partyIdFor, PartyHelper.CLIENT_PARTY_ROLES, delegator);
            if (roleTypeIdFor == null) {
                Debug.logError("Failed to check permission for partyId [" + partyIdFor
                        + "] because that party does not have a valid role. I.e., it is not an Account, Contact, Lead, etc.", MODULE);
                return false;
            }
            */

            // Now get a list of all the parties for whom the userLogin's partyId has the required securityModule+"_MANAGER" or securityModule+securityOperation permission
            // due to a grant by PartyRelationship.securityGroupId
            EntityCondition filterByDateCondition = EntityUtil.getFilterByDateExpr();
            EntityCondition operationConditon = EntityCondition.makeCondition(EntityOperator.OR,
                               EntityCondition.makeCondition("permissionId", securityModule + "_MANAGER"),
                               EntityCondition.makeCondition("permissionId", securityModule + securityOperation));
            EntityCondition searchConditions = EntityCondition.makeCondition(EntityOperator.AND,
                               EntityCondition.makeCondition("partyIdTo", userLogin.getString("partyId")),
                               operationConditon,
                               filterByDateCondition);
            List<GenericValue> permittedRelationships = delegator.findByConditionCache("PartyRelationshipAndPermission", searchConditions, null, null);

            // do any of these explicitly state a permission for partyIdFor?  If so, then we're done
            List<GenericValue> directPermittedRelationships = EntityUtil.filterByAnd(permittedRelationships, UtilMisc.toMap("partyIdFrom", partyIdFor));
            if ((directPermittedRelationships != null) && (directPermittedRelationships.size() > 0)) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose(userLogin + " has direct permitted relationship for " + partyIdFor, MODULE);
                }
                return true;
            }

            // if not, then there is one more thing to check: for all the permitted relationships, were there any which are in turn related
            // to the partyIdFor through another current (non-expired) PartyRelationship?  Note that here we had to break with convention because
            // of the way PartyRelationship for CONTACT is written (ie, CONTACT_REL_INV is opposite of ASSIGNED_TO, etc.  See comments in CRMSFADemoData.xml
            for (Iterator<GenericValue> pRi = permittedRelationships.iterator(); pRi.hasNext();) {
                GenericValue permittedRelationship = pRi.next();
                EntityCondition indirectConditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("partyIdFrom", partyIdFor),
                        EntityCondition.makeCondition("partyIdTo", permittedRelationship.getString("partyIdFrom")),
                        filterByDateCondition);
                List<GenericValue> indirectPermittedRelationships = delegator.findByConditionCache("PartyRelationship", indirectConditions, null, null);
                if ((indirectPermittedRelationships != null) && (indirectPermittedRelationships.size() > 0)) {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose(userLogin + " has indirect permitted relationship for " + partyIdFor, MODULE);
                    }
                    return true;
                }
            }

        } catch (GenericEntityException ex) {
            Debug.logError("Unable to determine security from party relationship due to error " + ex.getMessage(), MODULE);
            return false;
        }

        Debug.logWarning("Checked UserLogin [" + userLogin + "] for permission to perform [" + securityModule + "] + [" + securityOperation + "] on partyId = [" + partyIdFor + "], but permission was denied", MODULE);
        return false;
    }

    /**
     * Method to check that the userLogin has permission to see a given section in an applicaiton.
     * This is partly a factory method that retrieves the application's specific OpentapsSecurity
     * object and invokes the abstract checkSectionSecurity method.  The fallback is to use the
     * standard hasEntityPermission check, in which case a warning is also logged.
     */
    public static boolean checkSectionSecurity(String application, String section, String module, HttpServletRequest request) {
        Security security = (Security) request.getAttribute("security");
        GenericValue userLogin = (GenericValue) request.getAttribute("userLogin");
        Class<?> c = (Class<?>) applicationSecurityCache.get(application);
        if (c == null) {
            c = OpentapsSecurity.class;
        }
        try {
            Constructor<?> constructor = c.getConstructor(new Class<?>[] {Security.class, GenericValue.class});
            OpentapsSecurity osecurity = (OpentapsSecurity) constructor.newInstance(new Object[] {security, userLogin});
            return osecurity.checkSectionSecurity(section, module, request);
        } catch (NoSuchMethodException e) {
            Debug.logError(e, "Could not find required constructor for [" + c.getName() + "]", MODULE);
            return false;
        } catch (IllegalAccessException e) {
            Debug.logError(e, "Insufficient privileges to create [" + c.getName() + "]: " + e.getMessage(), MODULE);
            return false;
        } catch (InstantiationException e) {
            Debug.logError(e, "Failed to create object [" + c.getName() + "]: " + e.getMessage(), MODULE);
            return false;
        } catch (InvocationTargetException e) {
            Debug.logError(e, "Failed to invoke constructor or [" + c.getName() + "]: " + e.getMessage(), MODULE);
            return false;
        }
    }

    /** Subclasses should override this to do more specific security checking. */
    public boolean checkSectionSecurity(String section, String module, HttpServletRequest request) {
        GenericValue userLogin = (GenericValue) request.getAttribute("userLogin");
        return security.hasEntityPermission(module, "_VIEW", userLogin);
    }
}
