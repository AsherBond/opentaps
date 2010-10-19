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

package com.opensourcestrategies.crmsfa.teams;

import java.util.*;

import com.opensourcestrategies.crmsfa.party.PartyHelper;
import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

/**
 * Team services. The service documentation is in services_teams.xml.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 407 $
 */

public final class TeamServices {

    private TeamServices() { }

    private static final String MODULE = TeamServices.class.getName();
    public static final String notificationResource = "notification";

    /**
     * Creates a team and assigns the userLogin as the team leader. Requires CRMSFA_TEAM_CREATE permission.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map createTeam(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String groupName = (String) context.get("groupName");
        String comments = (String) context.get("comments");

        // ensure team create permission
        if (!security.hasPermission("CRMSFA_TEAM_CREATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }
        try {
            Map servRes = dispatcher.runSync("createPartyGroup", UtilMisc.toMap("groupName", groupName, "comments", comments, "userLogin", userLogin));
            if (ServiceUtil.isError(servRes)) {
                return servRes;
            }
            String partyId = (String) servRes.get("partyId");

            servRes = dispatcher.runSync("createPartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", "ACCOUNT_TEAM", "userLogin", userLogin));
            if (ServiceUtil.isError(servRes)) {
                return servRes;
            }

            // we'll use the system user to add the userLogin to the team
            GenericValue system = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            servRes = dispatcher.runSync("crmsfa.addTeamMember", UtilMisc.toMap("teamMemberPartyId", userLogin.get("partyId"),
                        "accountTeamPartyId", partyId, "securityGroupId", "SALES_MANAGER", "userLogin", system));
            if (ServiceUtil.isError(servRes)) {
                return servRes;
            }

            Map result = ServiceUtil.returnSuccess();
            result.put("partyId", partyId);
            return result;
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateTeamFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateTeamFail", locale, MODULE);
        }
    }

    /**
     * Updates a team. Requires CRMSFA_TEAM_UPDATE permission on the team partyId.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map updateTeam(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = (String) context.get("partyId");
        String groupName = (String) context.get("groupName");
        String comments = (String) context.get("comments");

        // ensure team update permission
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_TEAM", "_UPDATE", userLogin, partyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }
        try {
            Map servRes = dispatcher.runSync("updatePartyGroup", UtilMisc.toMap("partyId", partyId, "groupName", groupName, "comments", comments, "userLogin", userLogin));
            if (ServiceUtil.isError(servRes)) {
                return servRes;
            }
            return ServiceUtil.returnSuccess();
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateTeamFail", locale, MODULE);
        }
    }

    /**
     * Deactivates a team. Requires CRMSFA_TEAM_DEACTIVATE permission on the team.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map deactivateTeam(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String teamPartyId = (String) context.get("partyId");

        // ensure team deactivate permission on this team
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_TEAM", "_DEACTIVATE", userLogin, teamPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }
        try {
            // expire all active relationships for team
            List relationships = delegator.findByAnd("PartyRelationship",
                    UtilMisc.toList(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, teamPartyId), EntityUtil.getFilterByDateExpr()));
            PartyHelper.expirePartyRelationships(relationships, UtilDateTime.nowTimestamp(), dispatcher, userLogin);

            // set the team party to PARTY_DISABLED (note: if your version of ofbiz fails to do this, then replace this with direct entity ops)
            Map servRes = dispatcher.runSync("setPartyStatus", UtilMisc.toMap("partyId", teamPartyId, "statusId", "PARTY_DISABLED", "userLogin", userLogin));
            if (ServiceUtil.isError(servRes)) {
                return servRes;
            }

            return ServiceUtil.returnSuccess();
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAssignFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAssignFail", locale, MODULE);
        }
    }

    /**
     * Assigns a team to an account. The userLogin must have CRMSFA_TEAM_ASSIGN permission on this account.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map assignTeamToAccount(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String accountPartyId = (String) context.get("accountPartyId");
        String teamPartyId = (String) context.get("teamPartyId");

        // ensure team assign permission on this account
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_TEAM", "_ASSIGN", userLogin, accountPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }
        try {
            // assign the team
            PartyHelper.copyToPartyRelationships(teamPartyId, "ACCOUNT_TEAM", accountPartyId, "ACCOUNT", userLogin, delegator, dispatcher);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAssignFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAssignFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Adds a team member to the specified account or account team.
     * The userLogin must have CRMSFA_TEAM_ASSIGN permission on this account or team.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map addTeamMember(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String teamMemberPartyId = (String) context.get("teamMemberPartyId");
        String accountTeamPartyId = (String) context.get("accountTeamPartyId");
        String securityGroupId = (String) context.get("securityGroupId");

        // ensure team assign permission on this account or team
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_TEAM", "_ASSIGN", userLogin, accountTeamPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }
        try {
            // find out whether the accountTeamPartyId is an account or team
            String roleTypeIdFrom = PartyHelper.getFirstValidRoleTypeId(accountTeamPartyId, UtilMisc.toList("ACCOUNT", "ACCOUNT_TEAM"), delegator);
            if (UtilValidate.isEmpty(roleTypeIdFrom)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPartyNotAccountOrTeam", UtilMisc.toMap("partyId", accountTeamPartyId), locale, MODULE);
            }

            // the the first valid role for the team member (which could be either ACCOUNT_MANAGER, ACCOUNT_REP, or CUST_SERVICE_REP)
            String roleTypeIdTo = PartyHelper.getFirstValidRoleTypeId(teamMemberPartyId, UtilMisc.toList("ACCOUNT_MANAGER", "ACCOUNT_REP", "CUST_SERVICE_REP"), delegator);
            if (UtilValidate.isEmpty(roleTypeIdTo)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPartyNotCrmUser", UtilMisc.toMap("partyId", teamMemberPartyId), locale, MODULE);
            }

            // find out if the candidate is already a member in this role
            List relationships = delegator.findByAnd("PartyRelationship", UtilMisc.toMap("partyIdFrom", accountTeamPartyId, "partyRelationshipTypeId", "ASSIGNED_TO",
                        "roleTypeIdFrom", roleTypeIdFrom, "partyIdTo", teamMemberPartyId, "roleTypeIdTo", roleTypeIdTo));
            List activeRelations = EntityUtil.filterByDate(relationships, UtilDateTime.nowTimestamp()); // filter out expired relationships
            if (activeRelations.size() > 0) {
                return UtilMessage.createAndLogServiceError("CrmErrorAlreadyMember", locale, MODULE);
            }

            // Ensure the PartyRoles
            Map ensureResult = dispatcher.runSync("ensurePartyRole", UtilMisc.toMap("partyId", accountTeamPartyId, "roleTypeId", roleTypeIdFrom, "userLogin", userLogin));
            if (ServiceUtil.isError(ensureResult)) {
                return UtilMessage.createAndLogServiceError(ensureResult, MODULE);
            }
            ensureResult = dispatcher.runSync("ensurePartyRole", UtilMisc.toMap("partyId", teamMemberPartyId, "roleTypeId", roleTypeIdTo, "userLogin", userLogin));
            if (ServiceUtil.isError(ensureResult)) {
                return UtilMessage.createAndLogServiceError(ensureResult, MODULE);
            }

            // create the PartyRelationship
            Map input = UtilMisc.toMap("partyIdFrom", accountTeamPartyId, "roleTypeIdFrom", roleTypeIdFrom, "partyIdTo", teamMemberPartyId, "roleTypeIdTo", roleTypeIdTo);
            input.put("partyRelationshipTypeId", "ASSIGNED_TO");
            input.put("securityGroupId", securityGroupId);
            input.put("fromDate", UtilDateTime.nowTimestamp());
            input.put("userLogin", userLogin);
            Map serviceResults = dispatcher.runSync("createPartyRelationship", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorAssignFail", locale, MODULE);
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAssignFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAssignFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Removes a team member from the specified account or account team.
     * The userLogin must have CRMSFA_TEAM_ASSIGN permission on this account or account team.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map removeTeamMember(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String teamMemberPartyId = (String) context.get("teamMemberPartyId");
        String accountTeamPartyId = (String) context.get("accountTeamPartyId");

        // ensure team remove permission on this account
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_TEAM", "_REMOVE", userLogin, accountTeamPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }
        try {
            // expire any active relationships between the team member and the account or team
            String roleTypeIdFrom = PartyHelper.getFirstValidRoleTypeId(accountTeamPartyId, UtilMisc.toList("ACCOUNT", "ACCOUNT_TEAM"), delegator);
            List relationships = TeamHelper.findActiveAccountOrTeamRelationships(accountTeamPartyId, roleTypeIdFrom, teamMemberPartyId, delegator);
            PartyHelper.expirePartyRelationships(relationships, UtilDateTime.nowTimestamp(), dispatcher, userLogin);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAssignFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAssignFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Changes a team member's security group (privilege). This only works on team members ASSIGNED_TO the account.
     * The userLogin must have CRMSFA_TEAM_UPDATE permission on this account or account team.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map setTeamMemberSecurityGroup(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String teamMemberPartyId = (String) context.get("teamMemberPartyId");
        String accountTeamPartyId = (String) context.get("accountTeamPartyId");
        String securityGroupId = (String) context.get("securityGroupId");

        // ensure team update permission on this account
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_TEAM", "_UPDATE", userLogin, accountTeamPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }
        try {
            // expire any active relationships between the team member and the account or team
            String roleTypeIdFrom = PartyHelper.getFirstValidRoleTypeId(accountTeamPartyId, UtilMisc.toList("ACCOUNT", "ACCOUNT_TEAM"), delegator);
            List relationships = TeamHelper.findActiveAccountOrTeamRelationships(accountTeamPartyId, roleTypeIdFrom, teamMemberPartyId, delegator);
            PartyHelper.expirePartyRelationships(relationships, UtilDateTime.nowTimestamp(), dispatcher, userLogin);

            // recreate the relationships with the new security group
            for (Iterator iter = relationships.iterator(); iter.hasNext();) {
                GenericValue relationship = (GenericValue) iter.next();

                Map input = UtilMisc.toMap("partyIdFrom", accountTeamPartyId, "roleTypeIdFrom", roleTypeIdFrom, "partyIdTo", teamMemberPartyId, "roleTypeIdTo", relationship.getString("roleTypeIdTo"));
                input.put("partyRelationshipTypeId", "ASSIGNED_TO");
                input.put("securityGroupId", securityGroupId);
                input.put("fromDate", UtilDateTime.nowTimestamp());
                input.put("userLogin", userLogin);
                Map serviceResults = dispatcher.runSync("createPartyRelationship", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorAssignFail", locale, MODULE);
                }
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAssignFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAssignFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Calls crmsfa.sendAccountTeamMemberNotificationEmails service for each team member in order to send notification emails of team assignment to an account.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map sendTeamAssignmentNotificationEmails(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String accountPartyId = (String) context.get("accountPartyId");
        String teamPartyId = (String) context.get("teamPartyId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        try {

            String accountPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, accountPartyId, false);
            String teamPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, teamPartyId, false);

            // Get all team members for account
            List teamMemberRelations = delegator.findByAnd("PartyToSummaryByRelationship", UtilMisc.toMap("partyIdFrom", accountPartyId, "roleTypeIdFrom", "ACCOUNT", "partyRelationshipTypeId", "ASSIGNED_TO"));
            teamMemberRelations = EntityUtil.filterByDate(teamMemberRelations);
            List teamMembers = EntityUtil.getFieldListFromEntityList(teamMemberRelations, "partyIdTo", true);

            // Assemble a list of team member names names added to the account
            // todo: move this to a bsh script? Or is speed more important?
            List teamMemberNames = new ArrayList();
            Iterator tmnit = teamMembers.iterator();
            while (tmnit.hasNext()) {
                String teamMemberPartyId = (String) tmnit.next();
                String teamMemberPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, teamMemberPartyId, false);
                if (UtilValidate.isNotEmpty(teamMemberPartyName)) {
                    teamMemberNames.add(teamMemberPartyName);
                }
            }
            Collections.sort(teamMemberNames);

            // Include the current responsible party for the account
            GenericValue responsibleParty = PartyHelper.getCurrentResponsibleParty(accountPartyId, "ACCOUNT", delegator);
            if (responsibleParty != null && responsibleParty.getString("partyId") != null && teamMemberNames.contains(responsibleParty.getString("partyId"))) {
                teamMembers.add(responsibleParty.getString("partyId"));
            }

            if (UtilValidate.isEmpty(teamMembers)) {
                return ServiceUtil.returnSuccess();
            }

            Map messageMap = UtilMisc.toMap("teamMemberNames", teamMemberNames, "accountPartyId", accountPartyId, "accountPartyName", accountPartyName, "teamPartyId", teamPartyId, "teamPartyName", teamPartyName);
            String url = UtilProperties.getMessage(notificationResource, "crmsfa.url.account.assignTeam", messageMap, locale);
            messageMap.put("url", url);
            String subject = UtilProperties.getMessage(notificationResource, "subject.account.assignTeam", messageMap, locale);

            Map bodyParameters = UtilMisc.toMap("eventType", "account.assignTeam");
            bodyParameters.putAll(messageMap);

            Iterator tmit = teamMembers.iterator();
            while (tmit.hasNext()) {
                String teamMemberPartyId = (String) tmit.next();
                Map sendEmailsResult = dispatcher.runSync("crmsfa.sendCrmNotificationEmails", UtilMisc.toMap("notifyPartyIds", UtilMisc.toList(teamMemberPartyId), "eventType", "account.assignTeam", "subject", subject, "bodyParameters", bodyParameters, "userLogin", userLogin));
                if (ServiceUtil.isError(sendEmailsResult)) {
                    return sendEmailsResult;
                }
            }
        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        } catch (GenericServiceException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }
}
