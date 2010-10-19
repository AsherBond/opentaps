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

package org.opentaps.common.invoice;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;

/**
 * InvoiceHelper - Helper functions for invoices.
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev:780 $
 */
public final class InvoiceHelper {

    private InvoiceHelper() { }

    @SuppressWarnings("unused")
    private static final String MODULE = InvoiceHelper.class.getName();

    private static BigDecimal ZERO = BigDecimal.ZERO;
    private static int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");
    public static final List<String> invoiceDueDateAgreementTermTypeIds = Arrays.asList("FIN_PAYMENT_TERM", "FIN_PAYMENT_FIXDAY");

    /**
     * Joins invoice item with data required for simplified presentation.
     * @param invoiceItem the invoice item <code>GenericValue</code>
     * @return Map of all invoice item data and joined in fields.
     * @throws GenericEntityException if an error occurs
     */
    public static Map<String, Object> joinInvoiceItemForPresentation(GenericValue invoiceItem) throws GenericEntityException {
        Map<String, Object> data = FastMap.newInstance();
        data.putAll(invoiceItem.getAllFields());

        String description = invoiceItem.getString("description");
        if (description == null) {
            GenericValue taxRate = invoiceItem.getRelatedOneCache("TaxAuthorityRateProduct");
            if (taxRate != null && taxRate.get("description") != null) {
                description = taxRate.getString("description");
            } else {
                GenericValue itemType = invoiceItem.getRelatedOneCache("InvoiceItemType");
                if (itemType != null) {
                    description = itemType.getString("description");
                }
            }
        }
        data.put("description", description);
        data.put("quantity", invoiceItem.getBigDecimal("quantity"));

        return data;
    }

    /**
     * <p>Given a list of invoice items, aggregates them into one line for presentation.
     * It also adds a new field amountTotal which is the aggregated (quantity * amount) for the items.</p>
     *
     * <p>For items that do not have identical amounts, the isUniform flag must be set to false.
     * In this case, the quantity and amount fields left out of the line.</p>
     *
     * <p>For items that have identical amounts, the isUniform flag may be set to true.
     * If so done, the quantity field will be aggregated and placed in the line, along with
     * the amount.  These may be displayed as quantity and unit cost.</p>
     * @param invoiceItemList the <code>List</code> of invoice item <code>GenericValue</code> to aggregate
     * @param isUniform see description
     * @return the aggregated data
     * @throws GenericEntityException if an error occurs
     */
    public static Map<String, Object> aggregateInvoiceItemsForPresentation(List<GenericValue> invoiceItemList, boolean isUniform) throws GenericEntityException {
        if (invoiceItemList.size() == 0) {
            return FastMap.newInstance();
        }

        Map<String, Object> invoiceLine = null;
        for (GenericValue invoiceItem : invoiceItemList) {
            if (invoiceLine == null) {
                invoiceLine = FastMap.<String, Object>newInstance();
                invoiceLine.putAll(joinInvoiceItemForPresentation(invoiceItem));
            } else if (isUniform) {
                BigDecimal itemQuantity = invoiceItem.getBigDecimal("quantity");
                BigDecimal quantity = (BigDecimal) invoiceLine.get("quantity");
                quantity = itemQuantity.add(quantity == null ? ZERO : quantity);
                invoiceLine.put("quantity", quantity);
            }

            // keep a running amountTotal
            BigDecimal amountTotal = (BigDecimal) invoiceLine.get("amountTotal");
            if (amountTotal == null) {
                amountTotal = ZERO;
            }
            BigDecimal quantity = invoiceItem.getBigDecimal("quantity");
            if (quantity == null) {
                quantity = BigDecimal.ONE;
            }

            BigDecimal amount = invoiceItem.getBigDecimal("amount");
            amountTotal = amountTotal.add(quantity.multiply(amount)).setScale(decimals + 1, rounding);
            invoiceLine.put("amountTotal", amountTotal);
        }

        // round the numerical values properly
        BigDecimal amountTotal = (BigDecimal) invoiceLine.get("amountTotal");
        // round the intermediate values to one more decimal place than the final values
        invoiceLine.put("amountTotal", amountTotal.setScale(decimals + 1, rounding));

        // remove data for non-uniform sets
        if (!isUniform) {
            invoiceLine.remove("productId");
            invoiceLine.remove("quantity");
            invoiceLine.remove("amount");
        }

        return invoiceLine;
    }

    /**
     * <p>Gets the list of lines as should appear in a presentation invoice.  Each line
     * can either be a single invoice item or an aggregation of invoice items based
     * on some type.  For instance, if an order line item is associated with several
     * invoice items, they will be grouped into one line with the product ID, quantity
     * ordered, quantity shipped, and so on.</p>
     *
     * <p>The description field is either the invoice item's description, the tax rate
     * description, or invoice item type's description.</p>
     *
     * <ul>Currently implemented groupings:
     * <li> One line per order item with quantity ordered, shipped, backOrdered, and the amount (unit price).  The amountTotal field is (shipped * amount).
     *      A field orderItem contains the actual order item.
     * </li>
     * <li> One line per tax rate group, which is based on taxAuthorityRateSeqId.  The amountTotal field is the total tax amount for this group. </li>
     * </ul>
     * @param delegator a <code>Delegator</code> value
     * @param invoiceId a <code>String</code> value
     * @return the invoice lines
     * @exception GenericEntityException if an error occurs
     */
    public static List<Map<String, Object>> getInvoiceLinesForPresentation(Delegator delegator, String invoiceId) throws GenericEntityException {
        return getInvoiceLinesForPresentation(delegator, invoiceId, Boolean.FALSE);
    }

    /**
     * <p>Gets the list of lines as should appear in a presentation invoice.  Each line
     * can either be a single invoice item or an aggregation of invoice items based
     * on some type.  For instance, if an order line item is associated with several
     * invoice items, they will be grouped into one line with the product ID, quantity
     * ordered, quantity shipped, and so on.</p>
     *
     * <p>The description field is either the invoice item's description, the tax rate
     * description, or invoice item type's description.</p>
     *
     * <ul>Currently implemented groupings:
     * <li> One line per order item with quantity ordered, shipped, backOrdered, and the amount (unit price).  The amountTotal field is (shipped * amount).
     *      A field orderItem contains the actual order item.
     * </li>
     * <li> One line per tax rate group, which is based on taxAuthorityRateSeqId.  The amountTotal field is the total tax amount for this group. </li>
     * </ul>
     * @param delegator a <code>Delegator</code> value
     * @param invoiceId a <code>String</code> value
     * @param groupSalesTaxOnInvoicePdf  a <code>Boolean</code> value
     * @return the invoice lines
     * @exception GenericEntityException if an error occurs
     */
    public static List<Map<String, Object>> getInvoiceLinesForPresentation(Delegator delegator, String invoiceId, Boolean groupSalesTaxOnInvoicePdf) throws GenericEntityException {

        List<Map<String, Object>> invoiceLines = FastList.newInstance();
        List<GenericValue> invoiceItems = delegator.findByAnd("InvoiceItem", UtilMisc.toMap("invoiceId", invoiceId), UtilMisc.toList("invoiceItemSeqId"));
        // groups of invoice items for presentation
        Map<GenericValue, List<GenericValue>> orderItemGroup = FastMap.newInstance();
        Map<String, List<GenericValue>> taxGroup = FastMap.newInstance();

        // the first step is to group the invoice items by various heuristics, and remove them from list as well
        for (Iterator<GenericValue> iter = invoiceItems.iterator(); iter.hasNext();) {
            GenericValue invoiceItem = iter.next();

            // The relationship to order item is many to many, so we rely on the assumption that a given invoice item will not span multiple order items.
            GenericValue orderItemBilling = EntityUtil.getFirst(invoiceItem.getRelated("OrderItemBilling"));
            if (orderItemBilling != null) {
                GenericValue orderItem = orderItemBilling.getRelatedOne("OrderItem");
                List<GenericValue> invoiceItemList = orderItemGroup.get(orderItem);
                if (invoiceItemList == null) {
                    invoiceItemList = FastList.<GenericValue>newInstance();
                }
                invoiceItemList.add(invoiceItem);
                orderItemGroup.put(orderItem, invoiceItemList);
                iter.remove();
                continue;
            }

            // group the tax items
            if ("ITM_SALES_TAX".equals(invoiceItem.get("invoiceItemTypeId"))) {
                String key = groupSalesTaxOnInvoicePdf ? invoiceItem.getString("description") : invoiceItem.getString("invoiceItemSeqId");

                // avoid NPE in the fast map
                if (key == null) {
                    key = "";
                }

                List<GenericValue> invoiceItemList = taxGroup.get(key);
                if (invoiceItemList == null) {
                    invoiceItemList = FastList.<GenericValue>newInstance();
                }
                invoiceItemList.add(invoiceItem);
                taxGroup.put(key, invoiceItemList);
                iter.remove();
                continue;
            }
        }

        // generate invoice lines from the order group
        for (GenericValue orderItem : orderItemGroup.keySet()) {
            List<GenericValue> invoiceItemList = orderItemGroup.get(orderItem);
            Map<String, Object> invoiceLine = aggregateInvoiceItemsForPresentation(invoiceItemList, true);

            // add the orderItem to the line
            invoiceLine.put("orderItem", orderItem);

            // ordered quantity is the (ordered - canceled) from order item
            BigDecimal ordered = orderItem.getBigDecimal("quantity");
            BigDecimal cancelQuantity = orderItem.getBigDecimal("cancelQuantity");
            ordered = ordered.subtract(cancelQuantity == null ? ZERO : cancelQuantity);
            invoiceLine.put("ordered", ordered);

            // shipped is simply the quantity invoiced
            BigDecimal shipped = new BigDecimal(((Number) invoiceLine.get("quantity")).doubleValue());
            invoiceLine.put("shipped", shipped);

            // backordered is what remains to be shipped
            BigDecimal backOrdered = ordered.subtract(shipped);
            invoiceLine.put("backOrdered", backOrdered);

            List<EntityExpr> oppFields = UtilMisc.toList(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderItem.getString("orderId")), EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_CANCELLED"));
            invoiceLine.put("orderPaymentList", delegator.findByAnd("OrderPaymentPreference", oppFields));

            // shipment ID and tracking codes
            GenericValue billing = EntityUtil.getFirst(orderItem.getRelatedByAnd("OrderItemBilling", UtilMisc.toMap("invoiceId", invoiceLine.get("invoiceId"))));
            if (billing != null) {
                GenericValue issuance = billing.getRelatedOne("ItemIssuance");
                if (issuance != null) {
                    invoiceLine.put("shipmentId", issuance.get("shipmentId"));
                    invoiceLine.put("shipmentId", issuance.get("shipmentId"));
                    GenericValue shipment = issuance.getRelatedOne("Shipment");
                    invoiceLine.put("createdDate", shipment.get("createdDate"));
                    GenericValue shipmentRouteSegment = EntityUtil.getFirst(delegator.findByAnd("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", issuance.get("shipmentId")), UtilMisc.toList("shipmentRouteSegmentId")));
                    invoiceLine.put("carrierPartyId", shipmentRouteSegment.get("carrierPartyId"));
                    invoiceLine.put("shipmentMethodTypeId", shipmentRouteSegment.get("shipmentMethodTypeId"));
                    List<EntityExpr> conditions = Arrays.asList(
                            EntityCondition.makeCondition("shipmentId", EntityOperator.EQUALS, issuance.get("shipmentId")),
                            EntityCondition.makeCondition("trackingCode", EntityOperator.NOT_EQUAL, null)
                    );
                    List<GenericValue> codes = delegator.findByAnd("ShipmentPackageRouteSeg", conditions, UtilMisc.toList("shipmentPackageSeqId", "shipmentRouteSegmentId"));
                    List<String> trackingCodes = FastList.newInstance();
                    for (GenericValue code : codes) {
                        trackingCodes.add(code.getString("trackingCode"));
                    }
                    invoiceLine.put("trackingCodes", trackingCodes);
                }
            }

            // the order lines will be at the top of the list
            invoiceLines.add(invoiceLine);
        }

        // generate invoice lines from the tax group
        for (String key : taxGroup.keySet()) {
            List<GenericValue> invoiceItemList = taxGroup.get(key);
            Map<String, Object> invoiceLine = aggregateInvoiceItemsForPresentation(invoiceItemList, !groupSalesTaxOnInvoicePdf);
            invoiceLines.add(invoiceLine);
        }

        // add the remaining invoice items to the lines
        for (GenericValue invoiceItem  : invoiceItems) {
            Map<String, Object> invoiceLine = joinInvoiceItemForPresentation(invoiceItem);
            // quantity default to 1, amount default to 0 
            BigDecimal quantity = invoiceItem.getBigDecimal("quantity");
            if (quantity == null) {
                quantity = BigDecimal.ONE;
            }
            BigDecimal amount = invoiceItem.getBigDecimal("amount");
            if (amount == null) {
                amount = BigDecimal.ZERO;
            }
 
            BigDecimal amountTotal = quantity.multiply(amount).setScale(decimals + 1, rounding);
            invoiceLine.put("amountTotal", amountTotal);
            invoiceLines.add(invoiceLine);
        }

        return invoiceLines;

    }

    /**
     * Retrieve a list of AgreementTerms which apply to an invoice.
     * @param delegator a <code>Delegator</code> value
     * @param invoiceId a <code>String</code> value
     * @return List of agreementTerms which apply to the invoice, in descending order by lastUpdatedStamp. AgreementTerms which are present due
     *  to a PartyClassification relationship will be folded into the ordered list based on the lastUpdatedStamp of the PartyClassification record.
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static List getAgreementTermsForInvoice(Delegator delegator, String invoiceId) throws GenericEntityException {

        List terms = new ArrayList();

        EntityCondition dateFilter = EntityUtil.getFilterByDateExpr();
        GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));

        // determine what agreement type we'll be looking up
        boolean isReceipt = false;
        String agreementTypeId = null;
        if ("PURCHASE_INVOICE".equals(invoice.get("invoiceTypeId"))) {
            agreementTypeId = "PURCHASE_AGREEMENT";
            isReceipt = false;
        } else if ("SALES_INVOICE".equals(invoice.get("invoiceTypeId"))) {
            agreementTypeId = "SALES_AGREEMENT";
            isReceipt = true;
        } else if ("COMMISSION_INVOICE".equals(invoice.get("invoiceTypeId"))) {
            agreementTypeId = "COMMISSION_AGREEMENT";
            isReceipt = false;
        }
        if (agreementTypeId == null) {
            return terms;
        }

        // valid agreement item types are based on the Agreeement type
        List validItemTypes = delegator.findByAnd("AgreementToItemMap", UtilMisc.toMap("agreementTypeId", agreementTypeId));
        List validItemTypeIds = EntityUtil.getFieldListFromEntityList(validItemTypes, "agreementItemTypeId", true);

        // Get any agreements between the parties and retrieve the AgreementTerms
        List<EntityCondition> conditions = UtilMisc.toList(
            EntityCondition.makeCondition("agreementTypeId", agreementTypeId),
            EntityCondition.makeCondition("partyIdFrom", invoice.get(isReceipt ? "partyIdFrom" : "partyId")),
            EntityCondition.makeCondition("partyIdTo", invoice.get(isReceipt ? "partyId" : "partyIdFrom")),
            EntityCondition.makeCondition("agreementItemTypeId", EntityOperator.IN, validItemTypeIds),
            EntityCondition.makeCondition("statusId", "AGR_ACTIVE"),
            dateFilter
            );
        // currency is not supported right now--it will be moved from AgreementItem.currencyUomId to an AgreementTerm at some point
        List agreements = delegator.findByAnd("AgreementAndItemAndTerm", conditions);
        List agreementTermIds = EntityUtil.getFieldListFromEntityList(agreements, "agreementTermId", true);

        TreeMap termUpdatedStamps = new TreeMap();

        if (UtilValidate.isNotEmpty(agreementTermIds)) {
            List agreementTerms = delegator.findByCondition("AgreementTerm", EntityCondition.makeCondition("agreementTermId", EntityOperator.IN, agreementTermIds), null, UtilMisc.toList("lastUpdatedStamp DESC"));

            // Construct a sorted map of lists, keyed to the time when the records were last updated
            Iterator tit = agreementTerms.iterator();
            while (tit.hasNext()) {
                GenericValue term = (GenericValue) tit.next();
                Timestamp termUpdated = term.getTimestamp("lastUpdatedStamp");
                List termsForTimestamp = termUpdatedStamps.containsKey(termUpdated) ? (List) termUpdatedStamps.get(termUpdated) : new ArrayList();
                termUpdatedStamps.put(termUpdated, termsForTimestamp);
                termsForTimestamp.add(term);
            }
        }

        // Get the partyClassificationGroupIds of the invoice's parties
        List<GenericValue> fromPartyClassifications = delegator.findByAnd("PartyClassification", UtilMisc.toList(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, invoice.get(isReceipt ? "partyIdFrom" : "partyId")), dateFilter), UtilMisc.toList("lastUpdatedStamp DESC"));
        List<GenericValue> fromPartyClassGroupIds = EntityUtil.getFieldListFromEntityList(fromPartyClassifications, "partyClassificationGroupId", true);
        List<GenericValue> toPartyClassifications = delegator.findByAnd("PartyClassification", UtilMisc.toList(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, invoice.get(isReceipt ? "partyId" : "partyIdFrom")), dateFilter), UtilMisc.toList("lastUpdatedStamp DESC"));
        List<String> toPartyClassGroupIds = EntityUtil.getFieldListFromEntityList(toPartyClassifications, "partyClassificationGroupId", true);

        List classAgreementTerms = new ArrayList();
        List classConditions = null;

        // Add any agreements where both the from- and to- partyClassificationGroupId match one of the classifications for the respective parties
        if (UtilValidate.isNotEmpty(fromPartyClassGroupIds) && UtilValidate.isNotEmpty(toPartyClassGroupIds)) {
            classConditions = UtilMisc.toList(
                EntityCondition.makeCondition("fromPartyClassGroupId", EntityOperator.IN, fromPartyClassGroupIds),
                EntityCondition.makeCondition("toPartyClassGroupId", EntityOperator.IN, toPartyClassGroupIds)
            );
            classAgreementTerms.addAll(delegator.findByAnd("Agreement", classConditions));
        }

        // Add any agreements where the fromPartyClassificationGroupId matches one of the from party's classifications and the toPartyClassificationGroupId is
        //  null (applies to all parties)
        if (UtilValidate.isNotEmpty(fromPartyClassGroupIds)) {
            classConditions = UtilMisc.toList(
                EntityCondition.makeCondition("fromPartyClassGroupId", EntityOperator.IN, fromPartyClassGroupIds),
                EntityCondition.makeCondition("toPartyClassGroupId", EntityOperator.EQUALS, null)
            );
            classAgreementTerms.addAll(delegator.findByAnd("Agreement", classConditions));
        }

        // Add any agreements where the fromPartyClassificationGroupId is null (applies to all parties) and the toPartyClassificationGroupId matches
        //  one of the to party's classifications
        if (UtilValidate.isNotEmpty(toPartyClassGroupIds)) {
            classConditions = UtilMisc.toList(
                EntityCondition.makeCondition("fromPartyClassGroupId", EntityOperator.EQUALS, null),
                EntityCondition.makeCondition("toPartyClassGroupId", EntityOperator.IN, toPartyClassGroupIds)
            );
            classAgreementTerms.addAll(delegator.findByAnd("AgreementAndItemAndTerm", classConditions));
        }

        // Filter the partyClassification-based agreements by AgreementType, AgreementItemType, currency and date
        classAgreementTerms = EntityUtil.filterByAnd(classAgreementTerms, UtilMisc.toList(
                EntityCondition.makeCondition("agreementTypeId", EntityOperator.EQUALS, agreementTypeId),
                EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "AGR_ACTIVE"),
                EntityCondition.makeCondition("agreementItemTypeId", EntityOperator.IN, validItemTypeIds)
            ));
        classAgreementTerms = EntityUtil.filterByDate(classAgreementTerms); // Not using dateFilter here because it filters out seemingly valid agreements

        // Iterate through the AgreementTerms which are present due to a PartyClassification relationship and add them to the main sorted collection
        Iterator catit = classAgreementTerms.iterator();
        while (catit.hasNext()) {
            GenericValue agreementAndItemAndTerm = (GenericValue) catit.next();
            GenericValue agreementTerm = delegator.findByPrimaryKeyCache("AgreementTerm", UtilMisc.toMap("agreementTermId", agreementAndItemAndTerm.get("agreementTermId")));

            // Start with the lastUpdatedStamp of the AgreementTerm
            Timestamp lastUpdated = agreementTerm.getTimestamp("lastUpdatedStamp");

            String fromPartyClassGroupId = agreementAndItemAndTerm.getString("fromPartyClassGroupId");
            if (UtilValidate.isNotEmpty(fromPartyClassGroupId)) {

                // Get the most recent (first) of the fromParty's PartyClassifications matching the fromPartyClassGroupId
                GenericValue fromPartyClassification = EntityUtil.getFirst(EntityUtil.filterByAnd(fromPartyClassifications, UtilMisc.toMap("partyClassificationGroupId", fromPartyClassGroupId)));

                // Use the later of the agreementTerm' lastUpdatedStamp and the PartyClassification's lastUpdatedStamp
                if (UtilValidate.isNotEmpty(fromPartyClassification)) {
                    lastUpdated = lastUpdated.after(fromPartyClassification.getTimestamp("lastUpdatedStamp")) ? lastUpdated : fromPartyClassification.getTimestamp("lastUpdatedStamp");
                }
            }
            String toPartyClassGroupId = agreementAndItemAndTerm.getString("toPartyClassGroupId");
            if (UtilValidate.isNotEmpty(toPartyClassGroupId)) {

                // Get the most recent (first) of the toParty's PartyClassifications matching the toPartyClassGroupId
                GenericValue toPartyClassification = EntityUtil.getFirst(EntityUtil.filterByAnd(toPartyClassifications, UtilMisc.toMap("partyClassificationGroupId", toPartyClassGroupId)));

                // Use the latest of the agreementTerm' lastUpdatedStamp and the PartyClassification's lastUpdatedStamp
                if (UtilValidate.isNotEmpty(toPartyClassification)) {
                    lastUpdated = lastUpdated.after(toPartyClassification.getTimestamp("lastUpdatedStamp")) ? lastUpdated : toPartyClassification.getTimestamp("lastUpdatedStamp");
                }
            }

            // Add the AgreementTerm into the main sorted collection using the determined timestamp
            List termsForTimestamp = termUpdatedStamps.containsKey(lastUpdated) ? (List) termUpdatedStamps.get(lastUpdated) : new ArrayList();
            termUpdatedStamps.put(lastUpdated, termsForTimestamp);
            termsForTimestamp.add(agreementTerm);
        }

        // Iterate through the main sorted collection adding the collected terms to the return list
        Iterator tusit = termUpdatedStamps.keySet().iterator();
        while (tusit.hasNext()) {
            Timestamp lastUpdated = (Timestamp) tusit.next();
            List termsForTimestamp = (List) termUpdatedStamps.get(lastUpdated);
            terms.addAll(termsForTimestamp);
        }

        // Reverse the list so that the most-recently-modified values are first
        Collections.reverse(terms);
        return terms;
    }

    /**
     * Determines the amount that is currently authorized for the invoice, which will be captured at some later point.
     * @param invoice a <code>GenericValue</code> value
     * @return a <code>BigDecimal</code> value
     * @exception GeneralException if an error occurs
     */
    public static BigDecimal getInvoiceAuthorizedAmount(GenericValue invoice) throws GeneralException {
        Delegator delegator = invoice.getDelegator();
        String invoiceCurrencyUomId = invoice.getString("currencyUomId");

        // presently, the only authorizations are order payment preferences with statusId PAYMENT_AUTHORIZED
        List<GenericValue> billings = invoice.getRelated("OrderItemBilling");
        Set<String> orderIds = FastSet.newInstance();
        for (GenericValue billing : billings) {
            orderIds.add(billing.getString("orderId"));
        }
        if (orderIds.size() == 0) {
            return ZERO;
        }
        List<EntityExpr> conditions = UtilMisc.toList(
                EntityCondition.makeCondition("orderId", EntityOperator.IN, orderIds),
                EntityCondition.makeCondition("paymentStatusId", EntityOperator.EQUALS, "PAYMENT_AUTHORIZED")
        );
        List<GenericValue> prefs = delegator.findByAnd("OrderHeaderAndPaymentPref", conditions);

        BigDecimal authorizedAmount = ZERO;
        for (GenericValue pref : prefs) {
            // this check ensures that an invoice has orders with the same currency and is probably the only check of its kind in the entire system
            if (!invoiceCurrencyUomId.equals(pref.get("currencyUom"))) {
                throw new GeneralException("Invoice [" + invoice.get("invoiceId") + "] contains an order with a different currency. The order ID is [" + pref.get("orderId") + "].  Current operation does not support this data.");
            }
            if (UtilValidate.isNotEmpty(pref.getBigDecimal("maxAmount"))) {
                authorizedAmount = authorizedAmount.add(pref.getBigDecimal("maxAmount")).setScale(decimals, rounding);
            }
        }

        return authorizedAmount;
    }

}
