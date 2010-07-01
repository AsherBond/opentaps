/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
/* Copyright (c) 2005-2006 Open Source Strategies, Inc. */

/*
 *  $Id:$
 *
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
package com.opensourcestrategies.crmsfa.security;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.opensourcestrategies.crmsfa.activities.UtilActivity;
import com.opensourcestrategies.crmsfa.cases.UtilCase;
import com.opensourcestrategies.crmsfa.opportunities.UtilOpportunity;
import com.opensourcestrategies.crmsfa.party.PartyHelper;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;

/**
 * Special security methods for the CRM/SFA Application.
 */
public final class CrmsfaSecurity {

    private CrmsfaSecurity() { }

    private static final String MODULE = CrmsfaSecurity.class.getName();

    /**
     * This method supplements the standard OFBIZ security model with a security check specified in PartyRelationship.
     * It first does the standard OFBIZ security checks, then sees if an unexpired PartyRelationship exists where partyIdFrom=partyIdFor,
     * partyIdTo=UserLogin.partyId, and whose securityGroupId contains the security permission of module+"_MANAGER" or module+"_OPERATION".
     * If not, it will check one more time on whether, for any partyIdFrom for which a security permission does exist, there exists
     * a current (unexpired) PartyRelationship where partyIdFrom=partyIdFor, partyIdTo={partyId for which the required permission exists.}
     * If any of these are true, then the permission is true.  Otherwise, or if any entity operation errors occurred, false is returned.
     *
     * @param   security - Security object
     * @param   securityModule - The module to check (e.g., "CRMSFA_ACCOUNT", "PARTYMGR")
     * @param   securityOperation - What operation is being checked (e.g., "_VIEW", "_CREATE", "_UPDATE")
     * @param   userLogin - The userLogin to check permission for
     * @param   partyIdFor - What Account or Party the userLogin has permission to perform the operation on
     */
    public static boolean hasPartyRelationSecurity(Security security, String securityModule, String securityOperation, GenericValue userLogin, String partyIdFor) {

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
            GenericDelegator delegator = userLogin.getDelegator();

            // validate that partyIdFor is in our system in a proper role
            String roleTypeIdFor = PartyHelper.getFirstValidRoleTypeId(partyIdFor, PartyHelper.CLIENT_PARTY_ROLES, delegator);
            if (roleTypeIdFor == null) {
                Debug.logError("Failed to check permission for partyId [" + partyIdFor
                        + "] because that party does not have a valid role. I.e., it is not an Account, Contact, Lead, etc.", MODULE);
                return false;
            }

            // Now get a list of all the parties for whom the userLogin's partyId has the required securityModule+"_MANAGER" or securityModule+securityOperation permission
            // due to a grant by PartyRelationship.securityGroupId
            EntityCondition filterByDateCondition = EntityUtil.getFilterByDateExpr();
            EntityCondition operationConditon = EntityCondition.makeCondition(EntityOperator.OR,
                                    EntityCondition.makeCondition("permissionId", EntityOperator.EQUALS, securityModule + "_MANAGER"),
                                    EntityCondition.makeCondition("permissionId", EntityOperator.EQUALS, securityModule + securityOperation));
            EntityCondition searchConditions = EntityCondition.makeCondition(EntityOperator.AND,
                                    EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, userLogin.getString("partyId")),
                                    operationConditon,
                                    filterByDateCondition);
            List<GenericValue> permittedRelationships = delegator.findByCondition("PartyRelationshipAndPermission", searchConditions, null, null);

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
                                        EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyIdFor),
                                        EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, permittedRelationship.getString("partyIdFrom")),
                                        filterByDateCondition);
                List<GenericValue> indirectPermittedRelationships = delegator.findByCondition("PartyRelationship", indirectConditions, null, null);
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

        Debug.logWarning("Checked UserLogin [" + userLogin.getString("userLoginId") + "] for permission to perform [" + securityModule + "] + [" + securityOperation + "] on partyId = [" + partyIdFor + "], but permission was denied", MODULE);
        return false;
    }

    /**
     * Checks if a userLogin has permission to perform an operation on an opportunity.
     * The userLogin must pass CRMSFA_OPP_${securityOperation} for all associated accounts and contacts.
     */
    public static boolean hasOpportunityPermission(Security security, String securityOperation, GenericValue userLogin, String salesOpportunityId) {

        GenericDelegator delegator = userLogin.getDelegator();
        try {
            // check for existance first
            GenericValue opportunity = delegator.findByPrimaryKeyCache("SalesOpportunity", UtilMisc.toMap("salesOpportunityId", salesOpportunityId));
            if (opportunity == null) {
                return false;
            }

            // check for closed opportunities for actions that are not _VIEW
            if (!"_VIEW".equals(securityOperation) && "SOSTG_CLOSED".equals(opportunity.getString("opportunityStageId"))) {
                return false;
            }

            // check that userLogin can perform this operation on all associated accounts (orthogonal to leads)
            List<String> accountIds = UtilOpportunity.getOpportunityAccountPartyIds(delegator, salesOpportunityId);
            for (String accountId : accountIds) {
                if (!hasPartyRelationSecurity(security, "CRMSFA_OPP", securityOperation, userLogin, accountId)) {
                    return false;
                }
            }

            // check that userLogin can perform this operation on all associated leads (orthogonal to accounts)
            List<String> leadIds = UtilOpportunity.getOpportunityLeadPartyIds(delegator, salesOpportunityId);
            for (String leadId : leadIds) {
                if (!hasPartyRelationSecurity(security, "CRMSFA_OPP", securityOperation, userLogin, leadId)) {
                    return false;
                }
            }

            // check that userLogin can perform this operation on all associated contacts
            List<String> contactIds = UtilOpportunity.getOpportunityContactPartyIds(delegator, salesOpportunityId);
            for (String contactId : contactIds) {
                if (!hasPartyRelationSecurity(security, "CRMSFA_OPP", securityOperation, userLogin, contactId)) {
                    return false;
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Checked UserLogin [" + userLogin + "] for permission to perform [CRMSFA_OPP] + [" + securityOperation + "] on salesOpportunityId = [" + salesOpportunityId + "], but permission was denied due to exception: " + e.getMessage(), MODULE);
            return false;
        }

        // everything was passed
        return true;
    }

    /**
     * Checks if a userLogin has permission to perform an operation on a case. Cases are associated with accounts and contacts.
     * They also have someone in the role of request taker, but this person cannot do anything. Module CRMSFA_CASE is implied.
     */
    public static boolean hasCasePermission(Security security, String securityOperation, GenericValue userLogin, String custRequestId) {
        GenericDelegator delegator = userLogin.getDelegator();
        try {
            // check for existance first
            GenericValue custRequest = delegator.findByPrimaryKeyCache("CustRequest", UtilMisc.toMap("custRequestId", custRequestId));
            if (custRequest == null) {
                return false;
            }

            // check for closed cases for actions that are not _VIEW
            String statusId = custRequest.getString("statusId");
            if (!"_VIEW".equals(securityOperation) && UtilCase.caseIsInactive(custRequest)) {
                return false;
            }

            // use the cases helper method to get the PartyRelationshipAndCaseRoles for accounts and contacts of this case
            List<GenericValue> roles = UtilCase.getCaseAccountsAndContacts(delegator, custRequestId);
            for (Iterator<GenericValue> iter = roles.iterator(); iter.hasNext();) {
                GenericValue role = iter.next(); // we're interested in the partyIdFrom, which is also the partyId of PartyRelationshipAndCaseRole
                if (hasPartyRelationSecurity(security, "CRMSFA_CASE", securityOperation, userLogin, role.getString("partyId"))) {
                    return true;
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Checked UserLogin [" + userLogin + "] for permission to perform [CRMSFA_CASE] + [" + securityOperation + "] on custRequestId = [" + custRequestId + "], but permission was denied due to exception: " + e.getMessage(), MODULE);
        }
        return false;
    }

    /**
     * Checks if a userLogin has permission to perform an operation on a activity. Activities are workEfforts that have associations to accounts, contacts, leads,
     * opportunities and cases using various map entities. The user will need to pass all security checks for each association. This is to prevent the user from
     * doing things when he has access to only one association but not all.
     *
     * First, the user must pass a general CRMSFA_ACT_${securityOperation} check.
     * Then, if the internalPartyId is supplied, the user must pass the appropriate CRMSFA_ACCOUNT/CONTACT/LEAD_${securityOperation} check.
     * Then, if the salesOpportunityId is supplied, the user must pass CRMSFA_OPP_${securityOperation}
     * Then, if the custRequestId is supplied, the user must pass CRMSFA_CASE_${securityOperation}
     */
    public static boolean hasActivityPermission(Security security, String securityOperation, GenericValue userLogin,
            String workEffortId, String internalPartyId, String salesOpportunityId, String custRequestId) {

        // first check general CRMSFA_ACT_${securityOperation} permission
        if (!security.hasEntityPermission("CRMSFA_ACT", securityOperation, userLogin)) {
            Debug.logWarning("Checked UserLogin [" + userLogin.getString("userLoginId") + "] for permission to perform [CRMSFA_ACT] + [" + securityOperation + "] in general but permission was denied.", MODULE);
            return false;
        }

        GenericDelegator delegator = userLogin.getDelegator();
        try {
            // check for existance first
            GenericValue workEffort = delegator.findByPrimaryKeyCache("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            if (workEffort == null) {
                Debug.logWarning("Activity [" + workEffortId + "] cannot be found", MODULE);
                return false;
            }

            // check for closed activities for actions that are not _VIEW
            if (UtilActivity.activityIsInactive(workEffort) && !"_VIEW".equals(securityOperation)) {
                Debug.logWarning("User [" + userLogin.getString("userLoginId") + "] cannot attempt operation [" + securityOperation + "] on activity [" + workEffortId + "] whose status is [" + workEffort.getString("currentStatusId"), MODULE);
                return false;
            }

            // if there is an internalPartyId, check to see if user has permission for a party
            if ((internalPartyId != null) && !internalPartyId.equals("")) {

                // determine the security module of internal party, such as CRMSFA_ACCOUNT for accounts
                String securityModule = getSecurityModuleOfInternalParty(internalPartyId, delegator);
                if (securityModule == null) {
                    Debug.logWarning("Checked UserLogin [" + userLogin + "] for permission to perform [CRMSFA_ACT] + [" + securityOperation + "] on workEffortId = [" + workEffortId + "] but permission was denied because internalPartyId=[" + internalPartyId + "] has an unknown roleTypeId", MODULE);
                    return false;
                }

                // the security operation to check against the internal party is either _UPDATE or _VIEW depending on what is being done to the activity
                String internalPartySecurityOp = "_VIEW".equals(securityOperation) ? "_VIEW" : "_UPDATE";

                // see if user can do this operation on this party
                if (!hasPartyRelationSecurity(security, securityModule, internalPartySecurityOp, userLogin, internalPartyId)) {
                    Debug.logWarning("User [" + userLogin.getString("userLoginId") + "] is not related to party [" + internalPartyId + "] for activity [" + workEffortId + "]", MODULE);
                    return false;
                }
            }

            // if there is an opportunity, check to see if user has OPP permission
            if ((salesOpportunityId != null) && !salesOpportunityId.equals("")) {
                if (!hasOpportunityPermission(security, securityOperation, userLogin, salesOpportunityId)) {
                    Debug.logWarning("User [" + userLogin.getString("userLoginId") + "] does not have permission for opportunity [" + salesOpportunityId + "] for activity [" + workEffortId + "]", MODULE);
                    return false;
                }
            }

            // if there is a case, check to see if user has CASE permission
            if ((custRequestId != null) && !custRequestId.equals("")) {
                if (!hasCasePermission(security, securityOperation, userLogin, custRequestId)) {
                    Debug.logWarning("User [" + userLogin.getString("userLoginId") + "] does not have permission for case [" + custRequestId + "] for activity [" + workEffortId + "]", MODULE);
                    return false;
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Checked UserLogin [" + userLogin + "] for permission to perform [CRMSFA_ACT] + [" + securityOperation + "] on workEffortId = [" + workEffortId + "], internalPartyId=[" + internalPartyId + "], salesOpportunityId=[" + salesOpportunityId + "], custRequestId = [" + custRequestId + "], but permission was denied due to an exception: " + e.getMessage(), MODULE);
            return false;
        }

        // the user has passed everything
        return true;
    }

    /**
     * As above, but checks permission for every single existing association for a work effort. As a short cut, this will only check for parties which are directly
     * associated with the work effort through WorkEffortPartyAssociations. If the application changes to allow the existance of work efforts without any
     * party associations, then this method must be changed to relfect that. TODO: comprehensive (check case and opp security).
     */
    public static boolean hasActivityPermission(Security security, String securityOperation, GenericValue userLogin, String workEffortId) {
        // first check general CRMSFA_ACT_${securityOperation} permission
        if (!security.hasEntityPermission("CRMSFA_ACT", securityOperation, userLogin)) {
            Debug.logWarning("Checked UserLogin [" + userLogin + "] for permission to perform [CRMSFA_ACT] + [" + securityOperation + "] in general but permission was denied.", MODULE);
            return false;
        }

        GenericDelegator delegator = userLogin.getDelegator();
        try {
            // check for existance first
            GenericValue workEffort = delegator.findByPrimaryKeyCache("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            if (workEffort == null) {
                Debug.logWarning("Tried to perform operation [" + securityOperation + "] on an non-existent activity [" + workEffortId + "]", MODULE);
                return false;
            }

            // check for closed activities for actions that are not _VIEW
            if (!"_VIEW".equals(securityOperation) && UtilActivity.activityIsInactive(workEffort)) {
                Debug.logWarning("Tried to perform operation [" + securityOperation + "] on an inactive activity [" + workEffortId + "]", MODULE);
                return false;
            }

            List<GenericValue> parties = UtilActivity.getActivityParties(delegator, workEffortId, PartyHelper.CLIENT_PARTY_ROLES);
            for (Iterator<GenericValue> iter = parties.iterator(); iter.hasNext();) {
                String internalPartyId = iter.next().getString("partyId");
                String securityModule = getSecurityModuleOfInternalParty(internalPartyId, delegator);
                if (!hasPartyRelationSecurity(security, securityModule, securityOperation, userLogin, internalPartyId)) {
                    return false;
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Checked UserLogin [" + userLogin + "] for permission to perform [CRMSFA_ACT] + [" + securityOperation + "] on all associations with workEffortId=[" + workEffortId + "] but permission was denied due to an exception: " + e.getMessage(), MODULE);
            return false;
        }

        // the user has passed everything
        return true;

    }

    /**
     * @deprecated
     * Checks for activity security scope (visibility) permission for a security scope operation.
     * @author Alexandre Gomes
     * @param security
     * @param userLogin
     * @param workEffortId
     * @param securityScopeOperation - security scope operation : SECURITY_SCOPE_VIEW, SECURITY_SCOPE_UPDATE, SECURITY_SCOPE_CREATE
     * @return boolean
     */
    public static boolean hasActivityPermission(Security security, String securityOperation, GenericValue userLogin,
            String workEffortId, String securityScopeOperation) {

        // check for security scope (activity visibility) permission
        if (!hasSecurityScopePermission(security, userLogin, workEffortId, false)) {
            return false;
        }
        // passed security scope (visibility) tests, security scope permission granted
        return true;
    }

    /**
     * Get the security module relevant to the role of the given internal partyId.
     * @return The module as a string, such as "CRMSFA_ACCOUNT" for ACCOUNT partyIds or null if the role type is not found
     */
    public static String getSecurityModuleOfInternalParty(String partyId, GenericDelegator delegator) throws GenericEntityException {
        String roleTypeId = PartyHelper.getFirstValidInternalPartyRoleTypeId(partyId, delegator);
        return getSecurityModuleForRole(roleTypeId);
    }

    /**
     * Get the security module relevant to the given roleTypeId.
     */
    public static String getSecurityModuleForRole(String roleTypeId) {
        if ("ACCOUNT".equals(roleTypeId)) return "CRMSFA_ACCOUNT";
        if ("CONTACT".equals(roleTypeId)) return "CRMSFA_CONTACT";
        if ("PROSPECT".equals(roleTypeId)) return "CRMSFA_LEAD";
        if ("PARTNER".equals(roleTypeId)) return "CRMSFA_PARTNER";
        Debug.logInfo("No security module (CRMSFA_${role}) found for party role [" + roleTypeId + "].  Some operations might not be allowed.", MODULE);
        return null;
    }

    /**
     * Checks for activity security scope (visibility) permission for a security scope operation.
     * @author Alexandre Gomes
     * @param security
     * @param userLogin
     * @param workEffortId
     * @param isUpdateScope security scope operation
     * @return boolean
     */
    public static boolean hasSecurityScopePermission(Security security, GenericValue userLogin, String workEffortId, boolean isUpdateScope) {

        // check for activity admin (super user) permission
        if (security.hasEntityPermission("CRMSFA", "_ACT_ADMIN", userLogin)) {
            return true;
        }

        if (!isUpdateScope) {
            try {
                boolean isAssignee = hasActivityRelation(userLogin, workEffortId, false);
                return isAssignee;
            } catch (GenericEntityException e) {
                Debug.logError(e, "Checked UserLogin [" + userLogin + "] for permission to perform on workEffortId = [" + workEffortId + "], but permission was denied due to an exception: " + e.getMessage(), MODULE);
                return false;
            }
        }

        if (isUpdateScope) {
            try {
                boolean isOwner = hasActivityRelation(userLogin, workEffortId, true);
                return isOwner;
            } catch (GenericEntityException e) {
                Debug.logError(e, "Checked UserLogin [" + userLogin + "] for permission to perform on workEffortId = [" + workEffortId + "], but permission was denied due to an exception: " + e.getMessage(), MODULE);
                return false;
            }
        }

        // all tests passed, grant security scope permission
        return true;
    }

    public static boolean hasActivityUpdatePartiesPermission(Security security, GenericValue userLogin, String workEffortId, boolean checkForOwner) throws GenericEntityException {
        // check for activity admin (super user) permission
        if (security.hasEntityPermission("CRMSFA", "_ACT_ADMIN", userLogin)) {
            return true;
        }

        // if user does not have CRMSFA_ACT_ADMIN permission check if he is assignee for this activity
        try {
            if (!hasActivityRelation(userLogin, workEffortId, false)) {
                return false;
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Checked UserLogin [" + userLogin + "] for permission to update party on workEffortId = [" + workEffortId + "], but permission was denied due to an exception: " + e.getMessage(), MODULE);
            return false;
        }

        // passed all update parties permission tests, grant hasActivityUpdateParties permission
        return true;
    }

    private static boolean hasActivityRelation(GenericValue userLogin, String workEffortId, boolean checkForOwner) throws GenericEntityException {
        GenericDelegator delegator = userLogin.getDelegator();
        String partyId = (String) userLogin.get("partyId");

        // check if user is owner (checkForOwner == true) or just for assignee (checkForOwner == false)
        ArrayList<EntityCondition> conditionList = new ArrayList<EntityCondition>();
        ArrayList<EntityCondition> roleExprs = new ArrayList<EntityCondition>();
        roleExprs.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "CAL_OWNER"));
        if (!checkForOwner) {
            roleExprs.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "CAL_ATTENDEE"));
            roleExprs.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "CAL_DELEGATE"));
            roleExprs.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "CAL_ORGANIZER"));
        }
        EntityCondition roleCond = EntityCondition.makeCondition(roleExprs, EntityOperator.OR);
        conditionList.add(roleCond);

        // partyId and workEffortId primary keys condition
        ArrayList<EntityCondition> pkExprs = new ArrayList<EntityCondition>();
        pkExprs.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
        pkExprs.add(EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, workEffortId));
        EntityCondition pkCond = EntityCondition.makeCondition(pkExprs, EntityOperator.AND);
        conditionList.add(pkCond);
        conditionList.add(EntityUtil.getFilterByDateExpr());

        EntityCondition mainCond = EntityCondition.makeCondition(conditionList, EntityOperator.AND);

        List<GenericValue> userAssignments = delegator.findByCondition("WorkEffortPartyAssignment", mainCond, null, null);
        if (userAssignments.size() == 0) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the user has permission to change activity owner
     * (must have CRMSFA_ACT_ADMIN or be owner of this activity).
     */
    public static boolean hasChangeActivityOwnerPermission(GenericDelegator delegator, Security security, GenericValue userLogin, String workEffortId) throws GenericEntityException {

        GenericValue currentActivityOwner = UtilActivity.getActivityOwner(workEffortId, delegator);

        boolean isOwner = false;
        if (UtilValidate.isNotEmpty(currentActivityOwner)) {
            String currentActivityOwnerId = currentActivityOwner.getString("partyId");
            isOwner = currentActivityOwnerId.equals(userLogin.getString("partyId"));
        }

        if ((security.hasEntityPermission("CRMSFA_ACT", "_ADMIN", userLogin) || isOwner) && hasActivityUpdatePartiesPermission(security, userLogin, workEffortId, false)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if a userLogin has permission to perform an operation on an order.
     */
    public static boolean hasOrderPermission(Security security, String securityOperation, GenericValue userLogin, String orderId) {
        GenericDelegator delegator = userLogin.getDelegator();
        try {
            // check for existance first
            GenericValue order = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (order == null) {
                return false;
            }

            return true;
        } catch (GenericEntityException e) {
            Debug.logError(e, "Checked UserLogin [" + userLogin + "] for permission to perform [CRMSFA_CASE] + [" + securityOperation + "] on orderId = [" + orderId + "], but permission was denied due to exception: " + e.getMessage(), MODULE);
        }
        return false;
    }

}
