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
/* Copyright (c) Open Source Strategies, Inc. */

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
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.GenericDispatcher;
import org.opentaps.base.constants.OpentapsConfigurationTypeConstants;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.SecurityPermissionConstants;
import org.opentaps.base.entities.UserLogin;
import org.opentaps.base.entities.WorkEffortPartyAssignment;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

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
            Delegator delegator = userLogin.getDelegator();

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

        Delegator delegator = userLogin.getDelegator();
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
        Delegator delegator = userLogin.getDelegator();
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
     * @param security a <code>Security</code> value
     * @param securityOperation a <code>String</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param workEffortId a <code>String</code> value
     * @param internalPartyId an optional party ID to check permission for
     * @param salesOpportunityId an optional sales opportunity ID to check permission for
     * @param custRequestId an opportunity case ID to check permission for
     * @return a <code>boolean</code> value
     */
    public static boolean hasActivityPermission(Security security, String securityOperation, GenericValue userLogin,
                                                String workEffortId, String internalPartyId, String salesOpportunityId, String custRequestId) {


        // first check general CRMSFA_ACT_${securityOperation} permission
        if (!security.hasEntityPermission("CRMSFA_ACT", securityOperation, userLogin)) {
            Debug.logWarning("Checked UserLogin [" + userLogin.getString("userLoginId") + "] for permission to perform [CRMSFA_ACT] + [" + securityOperation + "] in general but permission was denied.", MODULE);
            return false;
        }

        Delegator delegator = userLogin.getDelegator();
        Infrastructure infrastructure = new Infrastructure(GenericDispatcher.getLocalDispatcher(null, delegator));

        try {
            // check for existance first
            GenericValue workEffort = delegator.findByPrimaryKeyCache("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            if (workEffort == null) {
                Debug.logWarning("Activity [" + workEffortId + "] cannot be found", MODULE);
                return false;
            }

            DomainsLoader dl = new DomainsLoader(infrastructure, new User(userLogin));
            PartyRepositoryInterface repository = dl.getDomainsDirectory().getPartyDomain().getPartyRepository();

            // if ACTIVITY_OWNER_CHANGE_ONLY configuration value is set to Y, deny any non view operation not made by the activity owner
            Boolean ownerChangeOnly = infrastructure.getConfigurationValueAsBoolean(OpentapsConfigurationTypeConstants.ACTIVITY_OWNER_CHANGE_ONLY);
            if (ownerChangeOnly && !"_VIEW".equals(securityOperation)) {
                List<WorkEffortPartyAssignment> owners = repository.findList(WorkEffortPartyAssignment.class, repository.map(WorkEffortPartyAssignment.Fields.workEffortId, workEffortId,
                                                                                                                             WorkEffortPartyAssignment.Fields.roleTypeId, RoleTypeConstants.CAL_OWNER,
                                                                                                                             WorkEffortPartyAssignment.Fields.partyId, userLogin.getString(UserLogin.Fields.partyId.name())));
                if (UtilValidate.isEmpty(owners)) {
                    // user is not the owner of the activity, allow only if user is the owner of the main party (Lead / Account / .. this activity is related to).
                    List<WorkEffortPartyAssignment> partiesAssignments = repository.findList(WorkEffortPartyAssignment.class,
                                                                                   EntityCondition.makeCondition(
                                                                                         EntityCondition.makeCondition(WorkEffortPartyAssignment.Fields.workEffortId.name(), workEffortId),
                                                                                         EntityCondition.makeCondition(WorkEffortPartyAssignment.Fields.roleTypeId.name(), EntityOperator.IN, PartyHelper.CLIENT_PARTY_ROLES)));
                    boolean bypassOwnerOnly = false;
                    for (WorkEffortPartyAssignment assignment : partiesAssignments) {
                        // note: do use "CRMSFA_ACT", "_OVRD_OWN_ONLY" as we want an exact match and not be allowed with CRMSFA_ACT_MANAGER
                        if (hasPartyRelationSecurity(security, SecurityPermissionConstants.CRMSFA_ACT_OVRD_OWN_ONLY, "", userLogin, assignment.getPartyId())) {
                            bypassOwnerOnly = true;
                            break;
                        }
                    }

                    if (!bypassOwnerOnly) {
                        Debug.logWarning("User [" + userLogin.getString("userLoginId") + "] is not the owner of the activity [" + workEffortId + "] or of any of the main parties, permission to perform [" + securityOperation + "] denied because the ACTIVITY_OWNER_CHANGE_ONLY setting is set to Y.", MODULE);
                        return false;
                    }
                }
            }

            // if there is an internalPartyId, check to see if user has permission for a party
            if (UtilValidate.isNotEmpty(internalPartyId)) {

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
            if (UtilValidate.isNotEmpty(salesOpportunityId)) {
                if (!hasOpportunityPermission(security, securityOperation, userLogin, salesOpportunityId)) {
                    Debug.logWarning("User [" + userLogin.getString("userLoginId") + "] does not have permission for opportunity [" + salesOpportunityId + "] for activity [" + workEffortId + "]", MODULE);
                    return false;
                }
            }

            // if there is a case, check to see if user has CASE permission
            if (UtilValidate.isNotEmpty(custRequestId)) {
                if (!hasCasePermission(security, securityOperation, userLogin, custRequestId)) {
                    Debug.logWarning("User [" + userLogin.getString("userLoginId") + "] does not have permission for case [" + custRequestId + "] for activity [" + workEffortId + "]", MODULE);
                    return false;
                }
            }

            // if the user is assigned to the activity, allow
            List<WorkEffortPartyAssignment> partyAssignments = repository.findList(WorkEffortPartyAssignment.class, repository.map(WorkEffortPartyAssignment.Fields.workEffortId, workEffortId,
                                                                                                                                   WorkEffortPartyAssignment.Fields.partyId, userLogin.getString(UserLogin.Fields.partyId.name())));

            if (UtilValidate.isNotEmpty(partyAssignments)) {
                return true;
            }

            // if the activity relates to some leads, check that all leads are assigned to the user (normally only one lead would be assigned or none)
            List<WorkEffortPartyAssignment> leadAssignments = repository.findList(WorkEffortPartyAssignment.class, repository.map(WorkEffortPartyAssignment.Fields.workEffortId, workEffortId,
                                                                                                                                  WorkEffortPartyAssignment.Fields.roleTypeId, RoleTypeConstants.PROSPECT));
            for (WorkEffortPartyAssignment assignment : leadAssignments) {
                if (!repository.isUserAssignedToLead(assignment.getPartyId())) {
                    Debug.logWarning("User [" + userLogin.getString("userLoginId") + "] is not assigned to lead [" + assignment.getPartyId() + "] for activity [" + workEffortId + "]", MODULE);
                    return false;
                }
            }

            // for other parties, check the user has either _UPDATE or _VIEW depending on what is being done to the activity
            List<WorkEffortPartyAssignment> otherAssignments = repository.findList(WorkEffortPartyAssignment.class,
                                                                                   EntityCondition.makeCondition(
                                                                                         EntityCondition.makeCondition(WorkEffortPartyAssignment.Fields.workEffortId.name(), workEffortId),
                                                                                         EntityCondition.makeCondition(WorkEffortPartyAssignment.Fields.roleTypeId.name(), EntityOperator.NOT_EQUAL, RoleTypeConstants.PROSPECT)));
            for (WorkEffortPartyAssignment assignment : otherAssignments) {

                // determine the security module of internal party, such as CRMSFA_ACCOUNT for accounts
                String securityModule = getSecurityModuleOfInternalParty(assignment.getPartyId(), delegator);
                // ignore unknown roles
                if (securityModule == null) {
                    continue;
                }

                // the security operation to check against the internal party is either _UPDATE or _VIEW depending on what is being done to the activity
                String internalPartySecurityOp = "_VIEW".equals(securityOperation) ? "_VIEW" : "_UPDATE";

                // see if user can do this operation on this party
                if (!hasPartyRelationSecurity(security, securityModule, internalPartySecurityOp, userLogin, assignment.getPartyId())) {
                    Debug.logWarning("User [" + userLogin.getString("userLoginId") + "] does not have [" + securityModule + internalPartySecurityOp + "] permission to related party [" + assignment.getPartyId() + "] for activity [" + workEffortId + "]", MODULE);
                    return false;
                }
            }
        } catch (GeneralException e) {
            Debug.logError(e, "Checked UserLogin [" + userLogin + "] for permission to perform [CRMSFA_ACT] + [" + securityOperation + "] on workEffortId = [" + workEffortId + "], internalPartyId=[" + internalPartyId + "], salesOpportunityId=[" + salesOpportunityId + "], custRequestId = [" + custRequestId + "], but permission was denied due to an exception: " + e.getMessage(), MODULE);
            return false;
        }

        // the user has passed everything
        return true;
    }

    /**
     * Checks if a userLogin has permission to perform an operation on a activity.
     * @param security a <code>Security</code> value
     * @param securityOperation a <code>String</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param workEffortId a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean hasActivityPermission(Security security, String securityOperation, GenericValue userLogin, String workEffortId) {
        return hasActivityPermission(security, securityOperation, userLogin, workEffortId, null, null, null);
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
    public static String getSecurityModuleOfInternalParty(String partyId, Delegator delegator) throws GenericEntityException {
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
        Delegator delegator = userLogin.getDelegator();
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
    public static boolean hasChangeActivityOwnerPermission(Delegator delegator, Security security, GenericValue userLogin, String workEffortId) throws GenericEntityException {

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
        Delegator delegator = userLogin.getDelegator();
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

    /**
     * Checks if a UserLogin has permission to perform an operation on a Note.
     *
     * @param security a <code>Security</code> value
     * @param modulePermission a <code>String</code> value
     * @param securityOperation a <code>String</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param note a <code>GenericValue</code> value
     * @param partyId a <code>String</code> value
     * @param custRequestId a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean hasNotePermission(Security security, String modulePermission, String securityOperation, GenericValue userLogin, GenericValue note, String partyId, String custRequestId) {

        try {
            Delegator delegator = userLogin.getDelegator();
            Infrastructure infrastructure = new Infrastructure(GenericDispatcher.getLocalDispatcher(null, delegator));

            if (note == null) {
                Debug.logError("Given Note was null", MODULE);
                return false;
            }

            // the following applies to UPDATE and DELETE actions only, for the rest just allow
            if ("_VIEW".equals(securityOperation)) {
                return true;
            }

            String noteId = note.getString("noteId");
            modulePermission = getModulePermission(modulePermission);

            // can be a party Note or a case Note
            if (UtilValidate.isNotEmpty(partyId)) {
                // make sure userLogin has UPDATE permission for the party
                if (!CrmsfaSecurity.hasPartyRelationSecurity(security, modulePermission, "_UPDATE", userLogin, partyId)) {
                    Debug.logWarning("Checked UserLogin [" + userLogin + "] for permission to perform [" + modulePermission + "_UPDATE] on partyId = [" + partyId + "], but permission was denied.", MODULE);
                    return false;
                }
            } else if (UtilValidate.isNotEmpty(custRequestId)) {
                // make sure userLogin has UPDATE permission for the case
                if (!CrmsfaSecurity.hasCasePermission(security, "_UPDATE", userLogin, custRequestId)) {
                    Debug.logWarning("Checked UserLogin [" + userLogin + "] for permission to perform [" + modulePermission + "_UPDATE] on custRequestId = [" + custRequestId + "], but permission was denied.", MODULE);
                    return false;
                }
            } else {
                // this error should never happen except due to programming error
                Debug.logError("Missing partyId or custRequestId in hasNotePermission.", MODULE);
                return false;
            }

            // if true, throw a permission denied error if the current user is not also the owner of the note and user does not have the CRMSFA_NOTE_OVRD_OWN_ONLY permission on the related party
            boolean noteOwnerChangeOnly = infrastructure.getConfigurationValueAsBoolean(OpentapsConfigurationTypeConstants.NOTE_OWNER_CHANGE_ONLY);
            if (noteOwnerChangeOnly && UtilValidate.isNotEmpty(partyId)) {
                if (CrmsfaSecurity.hasPartyRelationSecurity(security, SecurityPermissionConstants.CRMSFA_NOTE_OVRD_OWN_ONLY, "", userLogin, partyId)) {
                    noteOwnerChangeOnly = false;
                }
            }

            if (noteOwnerChangeOnly && (userLogin == null || !userLogin.getString("partyId").equals(note.getString("noteParty")))) {
                Debug.logWarning("UserLogin [" + userLogin + "] is not the owner of note = [" + noteId + "], permission [" + modulePermission + securityOperation + "] denied.", MODULE);
                return false;
            }
        } catch (GeneralException e) {
            Debug.logError(e, "Checked UserLogin [" + userLogin + "] for permission [" + modulePermission + securityOperation + "] on note = [" + note + "], but permission was denied due to exception : " + e.getMessage(), MODULE);
            return false;
        }

        return true;
    }

    /**
     * Checks if a UserLogin has permission to perform an operation on a Note.
     *
     * @param security a <code>Security</code> value
     * @param modulePermission a <code>String</code> value
     * @param securityOperation a <code>String</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param noteId a <code>String</code> value
     * @param partyId a <code>String</code> value
     * @param custRequestId a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean hasNotePermission(Security security, String modulePermission, String securityOperation, GenericValue userLogin, String noteId, String partyId, String custRequestId) {
        try {
            Delegator delegator = userLogin.getDelegator();
            GenericValue note = delegator.findByPrimaryKeyCache("NoteData", UtilMisc.toMap("noteId", noteId));

            if (note == null) {
                Debug.logWarning("Note [" + noteId + "] cannot be found", MODULE);
                return false;
            }

            return hasNotePermission(security, modulePermission, securityOperation, userLogin, note, partyId, custRequestId);

        }  catch (GeneralException e) {
            Debug.logError(e, "Checked UserLogin [" + userLogin + "] for permission [" + modulePermission + securityOperation + "] on note = [" + noteId + "], but permission was denied due to exception : " + e.getMessage(), MODULE);
            return false;
        }
    }


    /**
     * Converts the main module into its corresponding permission module.
     *
     * @param modulePermission a <code>String</code> value
     * @return a <code>String</code> value
     */
    public static String getModulePermission(String modulePermission) {
        if ("CRMSFA_LEADS".equals(modulePermission)) {
            return "CRMSFA_LEAD";
        } else if ("CRMSFA_ACCOUNT".equals(modulePermission)) {
            return "CRMSFA_ACCOUNT";
        } else if ("CRMSFA_CONTACT".equals(modulePermission)) {
            return "CRMSFA_CONTACT";
        }

        return "";
    }

}
