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
package org.opentaps.common.agreement;

import java.sql.Timestamp;
import java.util.*;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilConfig;
import org.opentaps.common.util.UtilMessage;

/**
 * Agreement-related services for Opentaps-common.
 */
public final class AgreementServices {

    private AgreementServices() { }

    private static final String MODULE = AgreementServices.class.getName();
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
    public static Map<String, Object> autoCreateAgreementItemsAndTerms(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();

        Locale locale = UtilCommon.getLocale(context);
        String agreementId = (String) context.get("agreementId");
        String agreementItemTypeId = (String) context.get("agreementItemTypeId");
        String currencyUomId = (String) context.get("currencyUomId");
        String agreementText = (String) context.get("agreementText");

        Timestamp now = UtilDateTime.nowTimestamp();
        try {
            GenericValue agreement = delegator.findByPrimaryKey("Agreement", UtilMisc.toMap("agreementId", agreementId));
            if (agreement == null) {
                return UtilMessage.createAndLogServiceError("OpentapsError_AgreementNotFound", UtilMisc.toMap("agreementId", agreementId), locale, MODULE);
            }

            // ensure currency is set
            if (UtilValidate.isEmpty(currencyUomId)) {
                currencyUomId = agreement.getString("defaultCurrencyUomId");
                if (UtilValidate.isEmpty(currencyUomId)) {
                    currencyUomId = UtilConfig.getPropertyValue("opentaps-common", "defaultCurrencyUomId");
                }
            }

            List<GenericValue> agreementItems = FastList.newInstance();

            if (agreementItemTypeId == null) {
                // create the agreement items based on the template definition as modeled in AgreementToItemMap
                List<GenericValue> itemMappings = delegator.findByAnd("AgreementToItemMap", UtilMisc.toMap("autoCreate", "Y", "agreementTypeId", agreement.get("agreementTypeId")), UtilMisc.toList("sequenceNum"));
                for (GenericValue mapping : itemMappings) {
                    GenericValue item = delegator.makeValue("AgreementItem");
                    item.set("agreementId", agreementId);
                    item.set("agreementItemTypeId", mapping.get("agreementItemTypeId"));
                    item.set("currencyUomId", UtilValidate.isNotEmpty(agreement.getString("defaultCurrencyUomId")) ? agreement.get("defaultCurrencyUomId") : currencyUomId);
                    item.set("agreementItemSeqId", UtilFormatOut.padString(((Long) mapping.get("sequenceNum")).toString(), 5, false, '0'));
                    item.create();
                    agreementItems.add(item);
                }
            } else {
                // create agreement item of specified type

                // Check if this agreement item type is valid for given agreement
                GenericValue itemMappings = delegator.findByPrimaryKey("AgreementToItemMap", UtilMisc.toMap("agreementTypeId", agreement.get("agreementTypeId"), "agreementItemTypeId", agreementItemTypeId));
                if (itemMappings == null) {
                    return UtilMessage.createAndLogServiceError("OpentapsError_AgreementItemNotValid", UtilMisc.toMap("agreementItemTypeId", agreementItemTypeId, "agreementId", agreementId), locale, MODULE);
                }

                GenericValue item = delegator.makeValue("AgreementItem");
                item.set("agreementId", agreementId);
                item.set("agreementItemTypeId", agreementItemTypeId);
                item.set("currencyUomId", currencyUomId);
                item.set("agreementText", agreementText);
                delegator.setNextSubSeqId(item, "agreementItemSeqId", 5, 1);
                item.create();
                agreementItems.add(item);
            }

            // create the agreement terms based on the template definition as modeled in AgreementItemToTermMap
            List<GenericValue> agreementTerms = FastList.newInstance();
            for (GenericValue item : agreementTerms) {
                List<GenericValue> mappings = delegator.findByAnd("AgreementItemToTermMap", UtilMisc.toMap("autoCreate", "Y", "agreementItemTypeId", item.get("agreementItemTypeId")), UtilMisc.toList("sequenceNum"));
                for (GenericValue mapping : mappings) {
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
            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("agreementId", agreementId);
            return results;
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
    }

    /**
     * Run as SECA and set initial status of the agreement.
     *
     * @param dctx DispatchContext
     * @param context Map
     * @return Map
     */
    public static Map<String, Object> setInitialAgreementStatus(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String agreementId = (String) context.get("agreementId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        Map<String, Object> params = FastMap.newInstance();
        params.put("agreementId", agreementId);
        params.put("statusId", "AGR_CREATED");
        params.put("userLogin", userLogin);

        try {
            dispatcher.runSync("updateAgreement", params);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();

    }

    public static Map<String, Object> removeAgreementItemAndTerms(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();

        Locale locale = UtilCommon.getLocale(context);

        String agreementId = (String) context.get("agreementId");
        String agreementItemSeqId = (String) context.get("agreementItemSeqId");

        try {

            delegator.removeByAnd("AgreementTerm", UtilMisc.toMap("agreementId", agreementId, "agreementItemSeqId", agreementItemSeqId));
            delegator.removeByAnd("AgreementItem", UtilMisc.toMap("agreementId", agreementId, "agreementItemSeqId", agreementItemSeqId));

        } catch (GenericEntityException gee) {
            return UtilMessage.createAndLogServiceError(gee, locale, MODULE);
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
    public static Map<String, Object> createAgreementAndRole(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        Map<String, Object> result = ServiceUtil.returnSuccess();

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
            return UtilMessage.createAndLogServiceError("OpentapsError_CreateAgreementFailSinceRole", UtilMisc.toMap("agreementTypeId", agreementTypeId, "roleTypeId", roleTypeId), locale, MODULE);
        }

        try {

            Map<String, Object> ensurePartyRoleResult = dispatcher.runSync("ensurePartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", roleTypeId));
            if (ServiceUtil.isError(ensurePartyRoleResult)) {
                return UtilMessage.createAndLogServiceError(ensurePartyRoleResult, MODULE);
            }

            result = dispatcher.runSync("createAgreement", context);

        } catch (GenericServiceException gse) {
            return UtilMessage.createAndLogServiceError(gse, locale, MODULE);
        }

        return result;
    }
}
