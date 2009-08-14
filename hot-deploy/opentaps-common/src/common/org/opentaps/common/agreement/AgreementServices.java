package org.opentaps.common.agreement;

/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

import java.util.*;
import java.sql.Timestamp;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

/**
 * Agreement-related services for Opentaps-common
 */
public final class AgreementServices {

    private AgreementServices() { }

    public static final String module = AgreementServices.class.getName();
    public static final String resource = "OpentapsUiLabels";
    public static final String errorResource = "OpentapsErrorLabels";

    /**
     * Given an Agreement header, creates the AgreementItem(s) and AgreementTerms based on a template.
     * If agreementItemTypeId found in context the service create item of specified type and its terms.
     * The template is modeled in the entities AgreementToItemMap and AgreementItemToTermMap.
     *
     * @param dctx DispatchContext
     * @param context Map
     * @return Map
     */
    public static Map autoCreateAgreementItemsAndTerms(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();

        Locale locale = UtilCommon.getLocale(context);
        String agreementId = (String) context.get("agreementId");
        String agreementItemTypeId = (String) context.get("agreementItemTypeId");
        String currencyUomId = (String) context.get("currencyUomId");
        String agreementText = (String) context.get("agreementText");

        Timestamp now = UtilDateTime.nowTimestamp();
        try {
            GenericValue agreement = delegator.findByPrimaryKey("Agreement", UtilMisc.toMap("agreementId", agreementId));
            if (agreement == null) {
                return UtilMessage.createAndLogServiceError("OpentapsError_AgreementNotFound", UtilMisc.toMap("agreementId", agreementId), locale, module);
            }

            List agreementItems = FastList.newInstance();

            if (agreementItemTypeId == null) {
                // create the agreement items based on the template definition as modeled in AgreementToItemMap
                List itemMappings = delegator.findByAnd("AgreementToItemMap", UtilMisc.toMap("autoCreate", "Y", "agreementTypeId", agreement.get("agreementTypeId")), UtilMisc.toList("sequenceNum"));
                for (Iterator iter = itemMappings.iterator(); iter.hasNext(); ) {
                    GenericValue mapping = (GenericValue) iter.next();
                    GenericValue item = delegator.makeValue("AgreementItem");
                    item.set("agreementId", agreementId);
                    item.set("agreementItemTypeId", mapping.get("agreementItemTypeId"));
                    item.set("currencyUomId", agreement.get("defaultCurrencyUomId"));
                    item.set("agreementItemSeqId", ((Long) mapping.get("sequenceNum")).toString());
                    item.create();
                    agreementItems.add(item);
                }
            } else {
                // create agreement item of specified type

                // Check if this agreement item type is valid for given agreement
                GenericValue itemMappings = delegator.findByPrimaryKey("AgreementToItemMap", UtilMisc.toMap("agreementTypeId", agreement.get("agreementTypeId"), "agreementItemTypeId", agreementItemTypeId));
                if (itemMappings == null) {
                    return UtilMessage.createAndLogServiceError("OpentapsError_AgreementItemNotValid", UtilMisc.toMap("agreementItemTypeId", agreementItemTypeId, "agreementId", agreementId), locale, module);
                }

                GenericValue item = delegator.makeValue("AgreementItem");
                item.set("agreementId", agreementId);
                item.set("agreementItemTypeId", agreementItemTypeId);
                item.set("currencyUomId", currencyUomId);
                item.set("agreementItemSeqId", delegator.getNextSeqId("AgreementItemSeqId"));
                item.set("agreementText", agreementText);
                item.create();
                agreementItems.add(item);
            }

            // create the agreement terms based on the template definition as modeled in AgreementItemToTermMap
            List agreementTerms = FastList.newInstance();
            for (Iterator iter = agreementItems.iterator(); iter.hasNext(); ) {
                GenericValue item = (GenericValue) iter.next();
                List mappings = delegator.findByAnd("AgreementItemToTermMap", UtilMisc.toMap("autoCreate", "Y", "agreementItemTypeId", item.get("agreementItemTypeId")), UtilMisc.toList("sequenceNum"));
                for (Iterator termIt = mappings.iterator(); termIt.hasNext(); ) {
                    GenericValue mapping = (GenericValue) termIt.next();
                    GenericValue term = delegator.makeValue("AgreementTerm");
                    term.set("agreementTermId", delegator.getNextSeqId("AgreementTerm"));
                    term.set("agreementId", agreementId);
                    term.set("termTypeId", mapping.get("termTypeId"));
                    term.set("agreementItemSeqId", item.get("agreementItemSeqId"));
                    term.set("fromDate", now);
                    term.set("description", mapping.get("defaultDescription"));
                    term.create();
                    agreementTerms.add(term);
                }
            }
            Map results = ServiceUtil.returnSuccess();
            results.put("agreementId", agreementId);
            return results;
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module);
        }
    }

    /**
     * Run as SECA and set initial status of the agreement.
     *
     * @param dctx DispatchContext
     * @param context Map
     * @return Map
     */
    public static Map setInitialAgreementStatus(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String agreementId = (String)context.get("agreementId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        Map params = FastMap.newInstance();
        params.put("agreementId", agreementId);
        params.put("statusId", "AGR_CREATED");
        params.put("userLogin", userLogin);

        try {
            dispatcher.runSync("updateAgreement", params);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, module);
        }

        return ServiceUtil.returnSuccess();

    }

    public static Map removeAgreementItemAndTerms(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();

        Locale locale = (Locale)context.get("locale");

        String agreementId = (String)context.get("agreementId");
        String agreementItemSeqId = (String)context.get("agreementItemSeqId");

        try {

            delegator.removeByAnd("AgreementTerm", UtilMisc.toMap("agreementId", agreementId, "agreementItemSeqId", agreementItemSeqId));
            delegator.removeByAnd("AgreementItem", UtilMisc.toMap("agreementId", agreementId, "agreementItemSeqId", agreementItemSeqId));

        } catch(GenericEntityException gee) {
            return UtilMessage.createAndLogServiceError(gee, locale, module);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Service create commission agreement.
     *
     * @param dctx DispatchContext
     * @param context Map
     * @return Map
     */
    public static Map createAgreementAndRole(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale)context.get("locale");
        Map result = ServiceUtil.returnSuccess();

        String partyId = (String) context.get("partyIdTo");
        String roleTypeId = (String) context.get("roleTypeIdTo");
        String agreementTypeId = (String) context.get("agreementTypeId");

        boolean applicableRole = false;
        if ("COMMISSION_AGREEMENT".equals(agreementTypeId)) {
            applicableRole = "COMMISSION_AGENT".equals(roleTypeId);
        } else if ("PARTNER_SALES_AGR".equals(agreementTypeId)) {
            applicableRole = Arrays.asList("ACCOUNT", "CONTACT", "PROSPECT", "PARTNER").contains(roleTypeId);
        }
        if (!applicableRole) {
            return UtilMessage.createAndLogServiceError("OpentapsError_CreateAgreementFailSinceRole", UtilMisc.toMap("agreementTypeId", agreementTypeId, "roleTypeId", roleTypeId), locale, module);
        }

        try {

            Map ensurePartyRoleResult = dispatcher.runSync("ensurePartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", roleTypeId));
            if (ServiceUtil.isError(ensurePartyRoleResult)) {
                return UtilMessage.createAndLogServiceError(ensurePartyRoleResult, module);
            }

            result = dispatcher.runSync("createAgreement", context);

        } catch (GenericServiceException gse) {
            return UtilMessage.createAndLogServiceError(gse, locale, module);
        }

        return result;
    }
}
