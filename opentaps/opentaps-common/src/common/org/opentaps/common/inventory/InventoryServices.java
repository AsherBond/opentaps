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

package org.opentaps.common.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.*;
import org.ofbiz.common.CommonWorkers;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.domain.organization.OrganizationRepository;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;

/**
 * Inventory services for Opentaps-Common.
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev$
 */
public final class InventoryServices {

    private InventoryServices() { }

    private static final String MODULE = InventoryServices.class.getName();

    /**
     * Create an InventoryItem.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map createInventoryItem(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Boolean validateAccountingTags = (Boolean) context.get("validateAccountingTags");
        if (validateAccountingTags == null) {
            validateAccountingTags = false;
        }

        Map result = ServiceUtil.returnSuccess();

        if (!(security.hasEntityPermission("CATALOG", "_CREATE", userLogin) || (security.hasEntityPermission("FACILITY", "_CREATE", userLogin)))) {
            return ServiceUtil.returnError("Security Error: to run updateInventoryItem you must have the CATALOG_CREATE, CATALOG_ADMIN, FACILITY_CREATE, or FACILITY_ADMIN permission.");
        }

        try {
            GenericValue inventoryItem = delegator.makeValue("InventoryItem");

            // set values
            inventoryItem.setNonPKFields(context);

            // if ownerPartyId is empty, set to the facility owner
            if (UtilValidate.isEmpty(inventoryItem.get("ownerPartyId"))) {
                GenericValue facility = delegator.findByPrimaryKey("Facility", UtilMisc.toMap("facilityId", inventoryItem.get("facilityId")));
                inventoryItem.put("ownerPartyId", facility.get("ownerPartyId"));
                // if still empty error
                if (UtilValidate.isEmpty(inventoryItem.get("ownerPartyId"))) {
                    return ServiceUtil.returnError("Cannot create InventoryItem without an ownerPartyId.");
                }
            }

            // if currencyUomId is empty, set to the owner base currency
            if (UtilValidate.isEmpty(inventoryItem.get("currencyUomId"))) {
                GenericValue acctgPreference = delegator.findByPrimaryKeyCache("PartyAcctgPreference", UtilMisc.toMap("partyId", inventoryItem.get("ownerPartyId")));
                if (acctgPreference != null) {
                    inventoryItem.put("currencyUomId", acctgPreference.get("baseCurrencyUomId"));
                }
                // if still empty get the general default currency
                if (UtilValidate.isEmpty(inventoryItem.get("currencyUomId"))) {
                    inventoryItem.put("currencyUomId", UtilProperties.getPropertyValue("general", "currency.uom.id.default"));
                }
                // if still empty error
                if (UtilValidate.isEmpty(inventoryItem.get("currencyUomId"))) {
                    return ServiceUtil.returnError("Cannot create InventoryItem without a currencyUomId; you can set a default currency in the PartyAcctgPreference for the party [" + inventoryItem.get("ownerPartyId") + "], or in the general.properties file under the key [currency.uom.id.default].");
                }
            }

            // if unitCost is empty, use getProductCost to get the standard cost
            if (UtilValidate.isEmpty(inventoryItem.get("unitCost"))) {
                Map costResult = dispatcher.runSync("getProductCost", UtilMisc.toMap("userLogin", userLogin, "productId", inventoryItem.get("productId"), "currencyUomId", inventoryItem.get("currencyUomId"), "costComponentTypePrefix", "EST_STD"));
                inventoryItem.put("unitCost", costResult.get("productCost"));
                // this set to 0 if getProductCost did not find a standard cost, but we still allow that
                if (UtilValidate.isEmpty(inventoryItem.get("unitCost"))) {
                    return ServiceUtil.returnError("Cannot create InventoryItem without an unitCost.");
                }
                if (inventoryItem.getDouble("unitCost") < 0) {
                    return ServiceUtil.returnError("Cannot create InventoryItem with a negative unitCost.");
                }
            }

            // validate the accounting tags if necessary
            if (validateAccountingTags) {
                OrganizationRepository repository = new OrganizationRepository(delegator);
                List<AccountingTagConfigurationForOrganizationAndUsage> missings = repository.validateTagParameters(inventoryItem, inventoryItem.getString("ownerPartyId"), UtilAccountingTags.PURCHASE_ORDER_TAG, UtilAccountingTags.ENTITY_TAG_PREFIX);
                if (!missings.isEmpty()) {
                    return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missings.get(0).getDescription()), locale, MODULE);
                }
            }

            // set the next sequence id and persist
            inventoryItem.put("inventoryItemId", delegator.getNextSeqId("InventoryItem"));
            delegator.create(inventoryItem);
            result.put("inventoryItemId", inventoryItem.get("inventoryItemId"));

        } catch (GeneralException ex) {
            return ServiceUtil.returnError("Cannot create InventoryItem." + ex.getMessage());
        }

        return result;
    }

    /**
     * Update an InventoryItem, same as the Ofbiz service but also output the old accounting tags for SECA.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map updateInventoryItem(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String inventoryItemId = (String) context.get("inventoryItemId");
        Boolean validateAccountingTags = (Boolean) context.get("validateAccountingTags");
        if (validateAccountingTags == null) {
            validateAccountingTags = false;
        }

        String oldOwnerPartyId = (String) context.get("ownerPartyId");

        Map result = ServiceUtil.returnSuccess();

        if (!(security.hasEntityPermission("CATALOG", "_UPDATE", userLogin) || (security.hasEntityPermission("FACILITY", "_UPDATE", userLogin)))) {
            return ServiceUtil.returnError("Security Error: to run updateInventoryItem you must have the CATALOG_UPDATE, CATALOG_ADMIN, FACILITY_UPDATE, or FACILITY_ADMIN permission.");
        }

        try {
            GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
            if (inventoryItem == null) {
                return ServiceUtil.returnError("InventoryItem [" + inventoryItemId + "] not found.");
            }

            // return the oldOwnerPartyId
            oldOwnerPartyId = inventoryItem.getString("ownerPartyId");
            if (UtilValidate.isEmpty(oldOwnerPartyId)) {
                GenericValue oldFacility = inventoryItem.getRelatedOne("Facility");
                oldOwnerPartyId = oldFacility.getString("ownerPartyId");
            }
            result.put("oldOwnerPartyId", oldOwnerPartyId);

            // return the old status
            result.put("oldStatusId", inventoryItem.get("statusId"));

            // return the old accounting tags
            UtilAccountingTags.putAllAccountingTags(inventoryItem, result, "oldTag");

            // update values
            inventoryItem.setNonPKFields(context);

            // validate the new accounting tags if necessary
            if (validateAccountingTags) {
                OrganizationRepository repository = new OrganizationRepository(delegator);
                List<AccountingTagConfigurationForOrganizationAndUsage> missings = repository.validateTagParameters(inventoryItem, inventoryItem.getString("ownerPartyId"), UtilAccountingTags.PURCHASE_ORDER_TAG, UtilAccountingTags.ENTITY_TAG_PREFIX);
                if (!missings.isEmpty()) {
                    return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missings.get(0).getDescription()), locale, MODULE);
                }
            }

            delegator.store(inventoryItem);

        } catch (GeneralException ex) {
            return ServiceUtil.returnError("Cannot update InventoryItem for [" + inventoryItemId + "] " + ex.getMessage());
        }

        return result;
    }

    /**
     * Maintains the InventoryItemValueHistory entity with creations or changes to inventory item unit costs.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map updateInventoryItemValueHistory(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String inventoryItemId = (String) context.get("inventoryItemId");

        Map result = ServiceUtil.returnSuccess();

        try {

            // Make sure the InventoryItem exists
            GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
            if (UtilValidate.isEmpty(inventoryItem)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_InventoryItemNotFound", context, locale, MODULE);
            }
            BigDecimal newUnitCost = BigDecimal.ZERO;
            if (UtilValidate.isEmpty(inventoryItem.get("unitCost"))) {
                UtilMessage.logServiceWarning("OpentapsError_InventoryItemValueHistory_UnitCostZero", context, locale, MODULE);
            } else {
                newUnitCost = inventoryItem.getBigDecimal("unitCost").setScale(4, BigDecimal.ROUND_HALF_UP);
            }

            // Get the last recorded unitCost for the inventoryItem
            EntityFindOptions findOpt = new EntityFindOptions();
            findOpt.setMaxRows(1);
            GenericValue oldInventoryItemValue = EntityUtil.getFirst(delegator.findByCondition("InventoryItemValueHistory", EntityCondition.makeCondition("inventoryItemId", inventoryItemId), null, null, UtilMisc.toList("dateTime DESC", "inventoryItemValueHistId DESC"), findOpt));
            BigDecimal oldUnitCost = null;
            if (UtilValidate.isNotEmpty(oldInventoryItemValue)) {
                oldUnitCost = UtilValidate.isEmpty(oldInventoryItemValue.get("unitCost")) ? BigDecimal.ZERO : oldInventoryItemValue.getBigDecimal("unitCost").setScale(4, BigDecimal.ROUND_HALF_UP);
            }

            // Return if the unitCost hasn't changed
            if (UtilValidate.isNotEmpty(oldUnitCost) && newUnitCost.compareTo(oldUnitCost) == 0) {
                UtilMessage.logServiceInfo("OpentapsError_InventoryItemValueHistory_UnitCostNotChanged", context, locale, MODULE);
                return result;
            }

            // Create a new InventoryItemValueHistory record
            GenericValue newInventoryItemValue = delegator.makeValue("InventoryItemValueHistory", UtilMisc.toMap("inventoryItemValueHistId", delegator.getNextSeqId("InventoryItemValueHistory"), "inventoryItemId", inventoryItemId, "setByUserLogin", userLogin.getString("userLoginId")));
            newInventoryItemValue.set("dateTime", UtilDateTime.nowTimestamp());
            newInventoryItemValue.set("unitCost", new BigDecimal(newUnitCost.doubleValue()));
            newInventoryItemValue.create();

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        return result;
    }

    /**
     * Ensures that a request to reserve inventory for an order item does not over reserve it.
     * This should be run as an invoke SECA on all reserveProductInventory services.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map checkInventoryAlreadyReserved(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);

        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        String productId = (String) context.get("productId");
        BigDecimal reserving = BigDecimal.valueOf((Double) context.get("quantity"));

        try {
            // applies only to sales orders
            GenericValue order = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (!"SALES_ORDER".equals(order.get("orderTypeId"))) {
                Debug.logInfo("Order [" + orderId + "] is not a sales order.  No need to check if inventory is already reserved.", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // count quantity ordered
            BigDecimal ordered = BigDecimal.ZERO;
            Map input = UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId);
            GenericValue item = delegator.findByPrimaryKey("OrderItem", input);
            if (item == null) {
                return UtilMessage.createServiceError("OrderErrorOrderItemNotFound", locale);
            }
            ordered = item.getBigDecimal("quantity");
            if (item.get("cancelQuantity") != null) {
                ordered = ordered.subtract(item.getBigDecimal("cancelQuantity"));
            }

            GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", item.get("productId")));
            if (product == null) {
                return UtilMessage.createAndLogServiceError("Cannot reserve " + reserving + " of Product [" + item.get("productId") + "], product not found.", MODULE);
            }

            // check if the item is a marketing package (MARKETING_PKG_PICK) as the products reserved for this item would be the components
            // if this is the case we get the total quantity of components ordered (which is the quantity of package item times quantity of component) matching the given productId
            boolean productIsMarketingPkgPick;
            // special case, if productId is the product id of the package, always do as if it is a normal product
            if (item.get("productId").equals(productId)) {
                productIsMarketingPkgPick = false;
            } else {
                productIsMarketingPkgPick = CommonWorkers.hasParentType(delegator, "ProductType", "productTypeId", product.getString("productTypeId"), "parentTypeId", "MARKETING_PKG_PICK");
            }

            if (productIsMarketingPkgPick) {
                if (UtilValidate.isEmpty(productId)) {
                    return UtilMessage.createAndLogServiceError("Cannot reserve " + reserving + " of Product [" + productId + "], not found as a component of MARKETING_PKG_PICK product [" + item.get("productId") + "].", MODULE);
                }

                Debug.logInfo("Product [" + product.get("productId") + "] is a MARKETING_PKG_PICK, actually trying to reserve [" + productId + "] ...", MODULE);
                Map componentsRes = dispatcher.runSync("getAssociatedProducts", UtilMisc.toMap("productId", item.getString("productId"), "type", "PRODUCT_COMPONENT"));
                if (ServiceUtil.isError(componentsRes)) {
                    return UtilMessage.createAndLogServiceError(componentsRes, MODULE);
                } else {
                    // get the total quantity to be reserved for the given productId
                    BigDecimal compToReserve = null;
                    List<GenericValue> assocProducts = (List<GenericValue>) componentsRes.get("assocProducts");
                    for (GenericValue productAssoc : assocProducts) {
                        // only interested in the product we are trying to reserve
                        if (!productId.equals(productAssoc.get("productIdTo"))) {
                            continue;
                        } else {
                            BigDecimal compQty = productAssoc.getBigDecimal("quantity");
                            if (compQty == null) {
                                compQty = BigDecimal.ZERO;
                            }
                            compToReserve = compQty.multiply(ordered);
                        }
                    }
                    // if the component was not found return an error
                    if (compToReserve == null) {
                        return UtilMessage.createAndLogServiceError("Cannot reserve " + reserving + " of Product [" + productId + "], not found as a component of MARKETING_PKG_PICK product [" + item.get("productId") + "].", MODULE);
                    }
                    ordered = compToReserve;
                }
            }

            // count up the quantity already reserved for this item  (note that canceling a reservation deletes it, thus this data represents what's actually reserved)
            BigDecimal reserved = BigDecimal.ZERO;
            Map<String, String> resFind = UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId);
            if (productIsMarketingPkgPick) {
                // filter the reservation to get the ones corresponding to the component we are trying to reserve
                resFind.put("productId", productId);
            }

            List<GenericValue> reservations = delegator.findByAnd("OrderItemShipGrpInvResAndItem", resFind);
            for (GenericValue reservation : reservations) {
                if (reservation.get("quantity") == null) {
                    continue; // paranoia
                }
                reserved = reserved.add(reservation.getBigDecimal("quantity"));
            }

            // make sure we're not over reserving the item TODO label
            if (reserving.compareTo(ordered.subtract(reserved)) > 0) {
                return UtilMessage.createAndLogServiceError("Cannot reserve " + reserving + " of Product [" + item.get("productId") + "].  There are already " + reserved + " reserved out of " + ordered + " ordered for order [" + orderId + "] line item [" + orderItemSeqId + "].", MODULE);
            }

            return ServiceUtil.returnSuccess();
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
    }
 }
