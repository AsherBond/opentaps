package com.opensourcestrategies.crmsfa.partners;

import java.util.Locale;
import java.util.Map;

import com.opensourcestrategies.crmsfa.party.PartyHelper;
import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.*;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

/**
 * CRMSFA specific partner services.
 */
public class PartnerServices {

    public static final String module = PartnerServices.class.getName();

    public static Map createPartner(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        if (!security.hasPermission("CRMSFA_PARTNER_CREATE", userLogin)) {
            return UtilMessage.createServiceError("CrmErrorPermissionDenied", locale);
        }

        String organizationPartyId = (String) context.get("organizationPartyId");
        try {
            String partnerPartyId = null;

            // make sure user has the right crmsfa roles defined.  otherwise the partner will be created as deactivated.
            if (UtilValidate.isEmpty(PartyHelper.getFirstValidTeamMemberRoleTypeId(userLogin.getString("partyId"), delegator))) {
                return UtilMessage.createServiceError("CrmError_NoRoleForCreateParty", locale, UtilMisc.toMap("userPartyName", org.ofbiz.party.party.PartyHelper.getPartyName(delegator, userLogin.getString("partyId"), false), "requiredRoleTypes", PartyHelper.TEAM_MEMBER_ROLES));
            }

            // create the Party and PartyGroup, which results in a partyId
            Map input = UtilMisc.toMap("groupName", context.get("groupName"), "groupNameLocal", context.get("groupNameLocal"),
                    "officeSiteName", context.get("officeSiteName"), "description", context.get("description"));
            Map serviceResults = dispatcher.runSync("createPartyGroup", input);
            if (ServiceUtil.isError(serviceResults)) {
                return serviceResults;
            }
            partnerPartyId = (String) serviceResults.get("partyId");

            serviceResults = dispatcher.runSync("createPartyRole", UtilMisc.toMap("partyId", partnerPartyId, "roleTypeId", "PARTNER", "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResults)) {
                return serviceResults;
            }

            // create the relationship between partner and organization (note: no security group yet)
            PartyHelper.createNewPartyToRelationship(organizationPartyId, partnerPartyId, "PARTNER", "PARTNER_OF",
                null, UtilMisc.toList("INTERNAL_ORGANIZATIO"), false, userLogin, delegator, dispatcher);

            // create PartySupplementalData
            GenericValue partyData = delegator.makeValue("PartySupplementalData", UtilMisc.toMap("partyId", partnerPartyId));
            partyData.setNonPKFields(context);
            partyData.create();

            // set the responsible party to the user who invoked this service
            createResponsiblePartnerRelationshipForParty(userLogin.getString("partyId"), partnerPartyId, userLogin, delegator, dispatcher);

            // create basic contact info
            ModelService service = dctx.getModelService("crmsfa.createBasicContactInfoForParty");
            input = service.makeValid(context, "IN");
            input.put("partyId", partnerPartyId);
            serviceResults = dispatcher.runSync(service.name, input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreatePartnerFail", locale, module);
            }

            // Send email re: responsible party to all involved parties TODO implement this?
            //Map sendEmailResult = dispatcher.runSync("crmsfa.sendPartnerResponsibilityNotificationEmails", UtilMisc.toMap("newPartyId", userLogin.getString("partyId"), "partnerPartyId", partnerPartyId, "userLogin", userLogin));

            // return the partyId of the newly created Partner
            Map results = ServiceUtil.returnSuccess();
            results.put("partyId", partnerPartyId);
            return results;
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreatePartnerFail", locale, module);
        }
    }

    public static Map updatePartner(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String partnerPartyId = (String) context.get("partyId");

        // make sure userLogin has CRMSFA_PARTNER_UPDATE permission for this partner
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_PARTNER", "_UPDATE", userLogin, partnerPartyId)) {
            return UtilMessage.createServiceError("CrmErrorPermissionDenied", locale);
        }
        try {
            // update the Party and PartyGroup
            Map input = UtilMisc.toMap("groupName", context.get("groupName"), "groupNameLocal", context.get("groupNameLocal"),
                    "officeSiteName", context.get("officeSiteName"), "description", context.get("description"));
            input.put("partyId", partnerPartyId);
            input.put("userLogin", userLogin);
            Map serviceResults = dispatcher.runSync("updatePartyGroup", input);
            if (ServiceUtil.isError(serviceResults)) {
                return serviceResults;
            }

            // update PartySupplementalData
            GenericValue partyData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", partnerPartyId));
            if (partyData == null) {
                // create a new one
                partyData = delegator.makeValue("PartySupplementalData", UtilMisc.toMap("partyId", partnerPartyId));
                partyData.create();
            }
            partyData.setNonPKFields(context);
            partyData.store();

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdatePartnerFail", locale, module);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdatePartnerFail", locale, module);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Creates an partner relationship of a given type for the given party and removes all previous relationships of that type.
     * This method helps avoid semantic mistakes and typos from the repeated use of this code pattern.
     */
    public static boolean createResponsiblePartnerRelationshipForParty(String partyId, String partnerPartyId,
            GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher)
        throws GenericServiceException, GenericEntityException {
        return PartyHelper.createNewPartyToRelationship(partyId, partnerPartyId, "PARTNER", "RESPONSIBLE_FOR",
                "PARTNER_OWNER", PartyHelper.TEAM_MEMBER_ROLES, true, userLogin, delegator, dispatcher);
    }

    public static Map createPartnerSalesAgreement(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String partnerPartyId = (String) context.get("partyIdFrom");
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_PARTNER", "_CREATE", userLogin, partnerPartyId)) {
            return UtilMessage.createServiceError("CrmErrorPermissionDenied", locale);
        }

        String partyIdTo = (String) context.get("partyIdTo");
        try {
            String roleTypeIdTo = PartyHelper.getFirstValidInternalPartyRoleTypeId(partyIdTo, dctx.getDelegator());
            if (roleTypeIdTo == null) {
                return UtilMessage.createServiceError("CrmError_MissingClientRole", locale, UtilMisc.toMap("partyId", partyIdTo));
            }

            ModelService service = dctx.getModelService("opentaps.createAgreementAndRole");
            Map input = service.makeValid(context, "IN");
            input.put("roleTypeIdTo", roleTypeIdTo);
            return dispatcher.runSync(service.name, input);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module);
        }
    }
}
