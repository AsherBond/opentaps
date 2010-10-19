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

package org.opentaps.purchasing.facility;

import javolution.util.FastMap;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.party.PartyContactHelper;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Facility services specific to the purchasing application.
 */
public final class FacilityServices {

    private FacilityServices() { }

    private static final String MODULE = FacilityServices.class.getName();

    public static Map<String, Object> createFacilityAssoc(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        Timestamp fromDate = (Timestamp) context.get("fromDate");
        if (fromDate == null) {
            fromDate = UtilDateTime.nowTimestamp();
        }
        try {
            if (!security.hasEntityPermission("PRCH", "_WRHS_CONFIG", userLogin)) {
                return ServiceUtil.returnError(UtilMessage.getPermissionDeniedError(UtilCommon.getLocale(context)));
            }

            GenericValue assoc = delegator.makeValue("FacilityAssoc");
            assoc.set("facilityId", context.get("facilityId"));
            assoc.set("facilityIdTo", context.get("facilityIdTo"));
            assoc.set("facilityAssocTypeId", context.get("facilityAssocTypeId"));
            assoc.set("fromDate", fromDate);
            assoc.set("thruDate", context.get("thruDate"));
            assoc.set("sequenceNum", context.get("sequenceNum"));
            assoc.create();
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    // TODO: at some point we need to decide a good set of rules for updating entities with fromDate as pk so
    // we don't have to pass in the fromDate.
    public static Map<String, Object> updateFacilityAssoc(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        try {
            if (!security.hasEntityPermission("PRCH", "_WRHS_CONFIG", userLogin)) {
                return ServiceUtil.returnError(UtilMessage.getPermissionDeniedError(locale));
            }

            Map<String, Object> lookup = FastMap.newInstance();
            lookup.put("facilityId", context.get("facilityId"));
            lookup.put("facilityIdTo", context.get("facilityIdTo"));
            lookup.put("facilityAssocTypeId", context.get("facilityAssocTypeId"));
            lookup.put("fromDate", context.get("fromDate"));

            GenericValue assoc = delegator.findByPrimaryKey("FacilityAssoc", lookup);
            if (assoc == null) {
                return ServiceUtil.returnError(UtilMessage.expandLabel("WarehouseError_CannotFindFacilityAssoc", locale, lookup));
            }
            assoc.set("thruDate", context.get("thruDate"));
            assoc.set("sequenceNum", context.get("sequenceNum"));
            assoc.store();
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createBackupWarehouse(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String ownerPartyId = (String) context.get("ownerPartyId");
        String organizationPartyId = (String) context.get("organizationPartyId");
        try {
            if (!security.hasEntityPermission("PRCH", "_WRHS_CONFIG", userLogin)) {
                return ServiceUtil.returnError(UtilMessage.getPermissionDeniedError(UtilCommon.getLocale(context)));
            }

            Map<String, Object> input = FastMap.newInstance();
            input.put("userLogin", userLogin);
            input.put("facilityTypeId", "WAREHOUSE");
            input.put("openedDate", context.get("fromDate"));
            input.put("ownerPartyId", ownerPartyId);
            input.put("defaultInventoryItemTypeId", context.get("defaultInventoryItemTypeId"));
            input.put("facilityName", context.get("facilityName"));
            Map<String, Object> results = dispatcher.runSync("createFacility", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }
            String facilityId = (String) results.get("facilityId");

            // make the organization the receiving inventory role of the facility unless the owner is the organization, in which case this is unnecessary
            if (ownerPartyId != organizationPartyId) {
                results = dispatcher.runSync("ensurePartyRole", UtilMisc.toMap("partyId", organizationPartyId, "roleTypeId", "RECV_INV_FOR"));
                if (ServiceUtil.isError(results)) {
                    return results;
                }
                input = FastMap.newInstance();
                input.put("userLogin", userLogin);
                input.put("facilityId", facilityId);
                input.put("partyId", organizationPartyId);
                input.put("roleTypeId", "RECV_INV_FOR");
                results = dispatcher.runSync("addPartyToFacility", input);
                if (ServiceUtil.isError(results)) {
                    return results;
                }
            }

            // make backup association
            input = FastMap.newInstance();
            input.put("userLogin", userLogin);
            input.put("facilityId", facilityId);
            input.put("facilityIdTo", context.get("facilityIdTo"));
            input.put("facilityAssocTypeId", "BACKUP_INVENTORY");
            input.put("fromDate", context.get("fromDate"));
            input.put("thruDate", context.get("thruDate"));
            input.put("sequenceNum", context.get("sequenceNum"));
            results = dispatcher.runSync("createFacilityAssoc", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }

            // create an accounting preference for the owner party in the given currency if none exists yet
            input = FastMap.newInstance();
            input.put("partyId", ownerPartyId);
            GenericValue pref = delegator.findByPrimaryKey("PartyAcctgPreference", UtilMisc.toMap("partyId", ownerPartyId));
            if (pref == null) {
                input.put("baseCurrencyUomId", context.get("currencyUomId"));
                pref = delegator.makeValue("PartyAcctgPreference", input);
                pref.create();
            }

            // if the owner party has any shipping addresses, then also use it for facility shipping (if none, try using the general address)
            List<GenericValue> addresses = PartyContactHelper.getContactMechsByPurpose(ownerPartyId, "POSTAL_ADDRESS", "SHIPPING_LOCATION", true, delegator);
            if (addresses.size() == 0) {
                addresses = PartyContactHelper.getContactMechsByPurpose(ownerPartyId, "POSTAL_ADDRESS", "GENERAL_LOCATION", true, delegator);
            }
            for (GenericValue address : addresses) {
                input = FastMap.newInstance();
                input.put("userLogin", userLogin);
                input.put("facilityId", facilityId);
                input.put("contactMechId", address.get("contactMechId"));
                results = dispatcher.runSync("createFacilityContactMech", input);
                if (ServiceUtil.isError(results)) {
                    return results;
                }

                input.put("contactMechPurposeTypeId", "SHIPPING_LOCATION");
                results = dispatcher.runSync("createFacilityContactMechPurpose", input);
                if (ServiceUtil.isError(results)) {
                    return results;
                }

                input.put("contactMechPurposeTypeId", "SHIP_ORIG_LOCATION");
                results = dispatcher.runSync("createFacilityContactMechPurpose", input);
                if (ServiceUtil.isError(results)) {
                    return results;
                }
            }

            // assign the given manager party as the manager of the facility (note: this is not the user login)
            GenericValue permission = delegator.makeValue("FacilityPartyPermission");
            permission.set("facilityId", facilityId);
            permission.set("partyId", context.get("managerPartyId"));
            permission.set("securityGroupId", "WRHS_MANAGER");
            permission.set("fromDate", UtilDateTime.nowTimestamp());
            permission.create();
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

}
