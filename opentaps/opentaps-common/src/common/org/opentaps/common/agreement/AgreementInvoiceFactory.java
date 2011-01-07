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

import java.math.BigDecimal;
import java.util.*;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

/**
 * Factory class that generates invoices from an agreement.
 *
 * Note that the design of this system has one entry point and one exit point.  It is conceivable we can treat this like
 * a tunable machine, where a business could adjust the way the invoices are generated using either configuration
 * properties or perhaps even special agreement terms.  In light of this idea, it would be pertinent to make a
 * configuration object to encapsulate the arguments for createInvoiceFromAgreement().
 */
public class AgreementInvoiceFactory {

    private static final String MODULE = AgreementInvoiceFactory.class.getName();
    private static int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

    // InvoiceItemTypes that identify which invoice line items are the base (non-adjustment) product values
    public static final List<String> PRODUCT_INVOICE_ITEM_TYPES = Arrays.asList(
            "INV_PROD_ITEM",
            "INV_DPROD_ITEM",
            "INV_FPROD_ITEM",
            "INV_FDPROD_ITEM",
            "INV_SPROD_ITEM"
    );

    /**
     * Creates an invoice based on the given agreement and invoices.  This is a parameterized routine for
     * services and must be called from within a service.
     * If the context includes a paymentApplicationId and paymentInvoiceTotal, the commission earned will be pro-rated
     * by the amount applied to the invoice total.  In thise case, only one invoice is passed to this method at a time.
     * TODO: implement conversion
     *
     * @param dctx The dispatch context.
     * @param context The service context.
     * @param agreement The agreement to use
     * @param invoices The invoices to process
     * @param invoiceTypeId What kind of invoice to generate
     * @param agentRoleTypeId The role of the agent, whether a commission agent, partner, etc.
     * @param currencyUomId The currency of the invoice and items
     * @param isDisbursement If set to true, then the invoice will be from the agent to the organization.  If false, then the other way around.
     * @param group Whether to group the invoice items by productId and invoiceItemTypeId.  The parentInvoiceId and parentInvoiceItemTypeId will be discarded.
     */
    public static Map<String, Object> createInvoiceFromAgreement(DispatchContext dctx, Map<String, ?> context, GenericValue agreement, Collection<GenericValue> invoices, String invoiceTypeId, String agentRoleTypeId, String currencyUomId, boolean isDisbursement, boolean group) throws GeneralException {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));

        // this is always true whether the agreement is for disbursement invoices or receipts
        String agentPartyId = agreement.getString("partyIdTo");
        String organizationPartyId = agreement.getString("partyIdFrom");

        // create the commission invoice items based on these terms
        Map<String, Collection<Map<String, Object>>> invoiceItems = AgreementInvoiceFactory.createInvoiceItemsForAgreement(dctx, context, agreement, invoices);

        // pro-rate the item amounts according to actual payment amount against an invoice
        String paymentApplicationId = (String) context.get("paymentApplicationId");
        BigDecimal paymentInvoiceTotal = (BigDecimal) context.get("paymentInvoiceTotal");
        if (invoices.size() == 1 && UtilValidate.isNotEmpty(paymentApplicationId) && paymentInvoiceTotal != null && paymentInvoiceTotal.signum() > 0) {
            GenericValue application = delegator.findByPrimaryKeyCache("PaymentApplication", UtilMisc.toMap("paymentApplicationId", paymentApplicationId));
            BigDecimal appliedAmount = application.getBigDecimal("amountApplied");
            for (Collection<Map<String, Object>> invoiceItemGroup : invoiceItems.values()) {
                for (Map<String, Object> invoiceItem : invoiceItemGroup) {
                    invoiceItem.put("amount", appliedAmount.divide(paymentInvoiceTotal, decimals, rounding).multiply((BigDecimal) invoiceItem.get("amount")));
                    String description = (String) invoiceItem.get("description");
                    invoiceItem.put("description", description);
                }
            }
        }

        // ensure that the invoice has positive value
        if (invoiceItems.size() == 0) {
            if (Debug.infoOn()) {
                Set<String> invoiceIds = FastSet.newInstance();
                for (GenericValue invoice : invoices) {
                    invoiceIds.add(invoice.getString("invoiceId"));
                }
                Debug.logInfo("No commission invoice items created from agreement [" + agreement.get("agreementId") + "] for invoices " + invoiceIds, MODULE);
            }
            return ServiceUtil.returnSuccess();
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Collection<Map<String, Object>> invoiceItemGroup : invoiceItems.values()) {
            for (Map<String, Object> invoiceItem : invoiceItemGroup) {
                total = total.add(getItemAmount(invoiceItem));
            }
        }
        if (total.signum() != 1) {
            Debug.logInfo("Not creating invoice for agent [" + agentPartyId + "] from agreement [" + agreement + "] because total is zero.  Invoices are: " + invoices, MODULE);
            return ServiceUtil.returnSuccess();
        }

        // create the invoice header
        ModelService createInvoiceService = dctx.getModelService("createInvoice");
        Map<String, Object> input = createInvoiceService.makeValid(context, "IN");
        input.put("invoiceId", delegator.getNextSeqId("Invoice"));
        input.put("invoiceTypeId", invoiceTypeId);
        input.put("partyIdFrom", isDisbursement ? agentPartyId : organizationPartyId);
        input.put("partyId", isDisbursement ? organizationPartyId : agentPartyId);
        input.put("currencyUomId", currencyUomId);
        input.put("roleTypeId", isDisbursement ? agentRoleTypeId : "INTERNAL_ORGANIZATIO");
        input.put("statusId", "INVOICE_IN_PROCESS");
        if (context.get("invoiceDate") == null) {
            input.put("invoiceDate", UtilDateTime.nowTimestamp());
        }
        Map<String, Object> result = dispatcher.runSync("createInvoice", input);
        if (ServiceUtil.isError(result)) {
            return result;
        }
        String invoiceId = (String) result.get("invoiceId");

        // create the invoice items depending, using separate methods depending on whether we're grouping or not
        if (group) {
            Map<String, Object> results = AgreementInvoiceFactory.createGroupedInvoiceItems(agreement, invoiceItems, invoiceId, userLogin, dctx);
            if (ServiceUtil.isError(results)) {
                return results;
            }
        } else {
            Map<String, Object> results = AgreementInvoiceFactory.createCollatedInvoiceItems(agreement, invoiceItems, invoiceId, userLogin, dctx, context);
            if (ServiceUtil.isError(results)) {
                return results;
            }
        }

        // record what we commissioned
        createAgreementBilling(agreement, paymentApplicationId, invoiceId, invoiceItems, dl);

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("invoiceId", invoiceId);
        return results;
    }

    // helper method to add up one of the invoice items
    private static BigDecimal getItemAmount(Map<String, Object> invoiceItem) {
        BigDecimal amount = (BigDecimal) invoiceItem.get("amount");
        BigDecimal quantity = (BigDecimal) invoiceItem.get("quantity");
        if (quantity == null) {
            quantity = BigDecimal.ONE; // just in case
        }
        BigDecimal itemAmount = amount.multiply(quantity);
        return itemAmount.setScale(decimals, rounding);
    }

    /**
     * Generates invoice items based on the given agreement and set of invoices. This is a generic routine that does no
     * validation and blindly processes the input items.  The output invoice items are raw and can be further processed
     * for ordering, grouping and tracking via InvoiceItem.parentInvoiceId or InvoiceItemAssoc.  They are grouped by
     * their agreementTermId in a map.
     */
    public static Map<String, Collection<Map<String, Object>>> createInvoiceItemsForAgreement(DispatchContext dctx, Map<String, ?> context, GenericValue agreement, Collection<GenericValue> invoices) throws GeneralException {
        Delegator delegator = dctx.getDelegator();
        Map<String, Collection<Map<String, Object>>> invoiceItems = new FastMap<String, Collection<Map<String, Object>>>();
        Collection<GenericValue> terms = delegator.findByAnd("AgreementAndItemAndTerm", UtilMisc.toMap("agreementId", agreement.get("agreementId")));
        for (GenericValue term : terms) {
            Collection<Map<String, Object>> termInvoiceItems = processAgreementTerm(dctx, context, agreement, term, invoices);
            invoiceItems.put(term.getString("agreementTermId"), termInvoiceItems);
        }
        return invoiceItems;
    }

    /**
     * Generates invoice items based on the given agreement term and invoices.  This method currently supports
     * term items of type COMM_RATES, which applies a flat commission to the invoice item types defined in
     * AgreementInvoiceItemType.
     */
    private static Collection<Map<String, Object>> processAgreementTerm(DispatchContext dctx, Map<String, ?> context, GenericValue agreement, GenericValue term, Collection<GenericValue> invoices) throws GeneralException {
        List<Map<String, Object>> items = FastList.newInstance();
        Locale locale = UtilCommon.getLocale(context);

        if (!"COMM_RATES".equals(term.get("agreementItemTypeId"))) {
            return items;
        }

        for (GenericValue invoice : invoices) {
            if ("FLAT_COMMISSION".equals(term.get("termTypeId"))) {
                items.addAll(processFlatCommission(agreement, term, invoice, locale));
            } else if ("PROD_CAT_COMMISSION".equals(term.get("termTypeId"))) {
                items.addAll(processProductCategoryCommission(agreement, term, invoice, locale, true));
            } else if ("PROD_GRP_COMMISSION".equals(term.get("termTypeId"))) {
                items.addAll(processProductCategoryCommission(agreement, term, invoice, locale, false));
            } else {
                Debug.logWarning("Agreement term type [" + term.get("termTypeId") + "] not supported yet.", MODULE);
            }
        }
        return items;
    }

    /**
     * Flat commission is simply the commission rate times each invoice line item as identified by AgreementInvoiceItemType.
     * Note that the termValue of the AgreementTerm should be in whole number percents.  E.g., a value of 7 means 7%.
     */
    private static Collection<Map<String, Object>> processFlatCommission(GenericValue agreement, GenericValue term, GenericValue invoice, Locale locale) throws GenericEntityException {
        return createCommissionInvoiceItems(agreement, term, invoice.getRelated("InvoiceItem"), locale);
    }

    /**
     * Generate invoice items for terms that apply to groups of products (ProductCategory and ProductCategoryMember).
     * Each PROD_CAT_COMMISSION AgreementTerm has an AgreementItem and AgreementCategoryAppl.  The item is used to identify the currency of the
     * term (unimplemented).  The AgreementCategoryAppl identifies the ProductCategory for the products relevant to the term.
     * The category must directly contain the products in ProductCategoryMember.  No rollups or category trees are inspected.
     * Note that the termValue of the AgreementTerm should be in whole number percents.  E.g., a value of 7 means 7%.
     *
     * The separateProducts boolean controls how the quantity is calculated for purposes of rate lookup.  If set to true,
     * then the term quantity range (minQuantity thru maxQuantity) is compared against the quantity for each product
     * separately.  The products that do qualify are used to determine which invoice items should have the term rate applied.
     *
     * If set to false, then the term quantity range is compared against the total number of products ordered in the category.
     * If the quantity ordered from the category meets the term requirements, then all invoice items with products in this
     * category have the term rate applied.
     *
     * Note that if an order is split across multiple invoices, this algorithm might apply the wrong rates from the perspective
     * of the whole order.  This issue is unavoidable unless the quantity is computed across invoices.
     */
    private static Collection<Map<String, Object>> processProductCategoryCommission(GenericValue agreement, GenericValue term, GenericValue invoice, Locale locale, boolean separateProducts) throws GenericEntityException {
        Collection<Map<String, Object>> noItems = FastList.newInstance();
        if (term.get("productCategoryId") == null) {
            Debug.logWarning("Cannot process product category commission for Term [" + term.get("agreementTermId") + "] of Agreement [" + term.get("agreementId") + "] because the product category was not specified.", MODULE);
            return noItems;
        }

        GenericValue productCategory = term.getRelatedOne("ProductCategory");
        if (productCategory == null) {
            Debug.logWarning("Cannot process product category commission for Term [" + term.get("agreementTermId") + "] of Agreement [" + term.get("agreementId") + "] because the specified product category [" + term.get("productCategoryId") + "] does not exist.", MODULE);
            return noItems;
        }

        List<GenericValue> members = EntityUtil.filterByDate(productCategory.getRelated("ProductCategoryMember"));
        if (members.size() == 0) {
            return noItems;
        }

        // build a set of productIds that are in this category
        Set<String> productIds = FastSet.newInstance();
        for (GenericValue member : members) {
            productIds.add(member.getString("productId"));
        }

        // initialize the quantity range to "unspecified"
        BigDecimal minQuantity = new BigDecimal("-1.0");
        BigDecimal maxQuantity = new BigDecimal("-1.0");

        // set the quantity range if defined
        if (term.get("minQuantity") != null) {
            minQuantity = term.getBigDecimal("minQuantity");
        }
        if (term.get("maxQuantity") != null) {
            maxQuantity = term.getBigDecimal("maxQuantity");
        }

        List<GenericValue> invoiceItems = invoice.getRelated("InvoiceItem");

        BigDecimal quantityTotal = BigDecimal.ZERO; // counts group products
        Map<String, BigDecimal> productQuantities = FastMap.newInstance(); // counts individual products

        // count the products indirectly by using PRODUCT_INVOICE_ITEM_TYPES
        for (GenericValue invoiceItem : invoiceItems) {
            if (!PRODUCT_INVOICE_ITEM_TYPES.contains(invoiceItem.getString("invoiceItemTypeId"))) {
                continue;
            }
            if (UtilValidate.isEmpty(invoiceItem.get("productId")) || (!productIds.contains(invoiceItem.get("productId")))) {
                continue;
            }

            BigDecimal quantity = invoiceItem.getBigDecimal("quantity");
            if (separateProducts) {
                String productId = invoiceItem.getString("productId");
                BigDecimal lastQuantity = productQuantities.get(productId);
                if (lastQuantity != null) {
                    quantity = quantity.add(lastQuantity);
                }
                productQuantities.put(productId, quantity);
            } else {
                quantityTotal = quantityTotal.add(quantity);
            }
        }

        if (separateProducts) {

            // validate range of each product separately, remove the productIds that dont't qualify
            Set<String> removeProducts = FastSet.newInstance();
            for (String productId : productQuantities.keySet()) {
                BigDecimal quantity = productQuantities.get(productId);
                if (!minQuantity.equals(new BigDecimal("-1.0")) && quantity.compareTo(minQuantity) < 0) {
                    removeProducts.add(productId);
                }
                if (!maxQuantity.equals(new BigDecimal("-1.0")) && quantity.compareTo(maxQuantity) > 0) {
                    removeProducts.add(productId);
                }
            }

            // remove the invoice items whose products don't qualify
            for (Iterator<GenericValue> iter = invoiceItems.iterator(); iter.hasNext();) {
                GenericValue invoiceItem = iter.next();
                String productId = invoiceItem.getString("productId");
                if (productId == null) {
                    continue; // skip the whole order adjustments
                }
                if (removeProducts.contains(productId)) {
                    iter.remove();
                }
            }
        } else {
            // validate range for all products counted together
            if (!minQuantity.equals(new BigDecimal("-1.0")) && quantityTotal.compareTo(minQuantity) < 0) {
                return noItems;
            }
            if (!maxQuantity.equals(new BigDecimal("-1.0")) && quantityTotal.compareTo(maxQuantity) > 0) {
                return noItems;
            }
        }

        return createCommissionInvoiceItems(agreement, term, invoiceItems, productIds, locale);
    }

    /**
     * Create the line items based on the commission rate defined in the term.termValue for each member of the
     * invoiceItems List and constrained to products in the given list, if given.
     * Note that the termValue of the AgreementTerm should be in whole number percents.  E.g., a value of 7 means 7%.
     * TODO: Implement InvoiceItemAssoc lookup to check any unfulfilled amounts, but this is advanced behavior
     */
    private static Collection<Map<String, Object>> createCommissionInvoiceItems(GenericValue agreement, GenericValue term, List<GenericValue> invoiceItems, Set productIds, Locale locale) throws GenericEntityException {
        Map<String, GenericValue> typeMap = getTypesToProcess(agreement);
        BigDecimal commissionRate = term.getBigDecimal("termValue").multiply(new BigDecimal("0.01"));

        // process each invoice item
        Collection<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (Iterator<GenericValue> iter = invoiceItems.iterator(); iter.hasNext();) {
            GenericValue invoiceItem = iter.next();

            // skip those not defined in AgreementInvoiceItemType
            GenericValue type = typeMap.get(invoiceItem.getString("invoiceItemTypeId"));
            if (type == null) {
                continue;
            }

            // filter by product if given the product contstraint list
            if (productIds != null) {
                if (invoiceItem.get("productId") == null) {
                    // for terms that apply to products, remove the items that don't have productId specified
                    // these are considered whole order adjustments that will be processed later
                    iter.remove();
                    continue;
                }
                if (!productIds.contains(invoiceItem.get("productId"))) {
                    continue;
                }
            }

            BigDecimal amount = invoiceItem.getBigDecimal("amount");
            BigDecimal quantity = invoiceItem.getBigDecimal("quantity");
            if (quantity == null) {
                quantity = BigDecimal.ONE;
            }

            amount = amount.multiply(quantity);
            BigDecimal commissionAmount = amount.multiply(commissionRate).setScale(decimals, rounding);

            // get the item type, rate and description for the new line item
            String invoiceItemTypeId = type.getString("invoiceItemTypeIdTo");
            String rate = UtilNumber.toPercentString(commissionRate, decimals, rounding);
            String description = UtilMessage.expandLabel(type.getString("descriptionLabel"), locale, UtilMisc.toMap("commissionRate", rate, "invoiceItem", invoiceItem));

            // create a bare invoice item, noting what the parent invoice is
            Map<String, Object> input = FastMap.newInstance();
            input.put("parentInvoiceId", invoiceItem.get("invoiceId"));
            input.put("parentInvoiceItemSeqId", invoiceItem.get("invoiceItemSeqId"));
            input.put("invoiceItemTypeId", invoiceItemTypeId);
            input.put("productId", invoiceItem.get("productId"));
            input.put("amount", commissionAmount);
            input.put("quantity", new BigDecimal("1.0"));
            input.put("description", description);

            items.add(input);
        }

        return items;
    }

    // As above, but don't check product constraint
    private static Collection<Map<String, Object>> createCommissionInvoiceItems(GenericValue agreement, GenericValue term, List<GenericValue> invoiceItems, Locale locale) throws GenericEntityException {
        return createCommissionInvoiceItems(agreement, term, invoiceItems, null, locale);
    }

    /**
     * Given a final list of InvoiceItem maps, sorts them into an order suitable for the final invoice and creates them.
     * The ordering is based on AgreementInvoiceItemTypeMap.sequenceNum and other things.
     * TODO: ordering is not implemented yet
     */
    public static Map<String, Object> createCollatedInvoiceItems(GenericValue agreement, Map<String, Collection<Map<String, Object>>> items, String invoiceId, GenericValue userLogin, DispatchContext dctx, Map<String, ?> context) throws GeneralException {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        int sequence = 1;
        for (String agreementTermId : items.keySet()) {
            for (Map<String, Object> item : items.get(agreementTermId)) {

                // have to create the item seq id here because it is not returned by service
                String invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(sequence++, 5);

                Map<String, Object> input = new FastMap<String, Object>(item);
                input.put("invoiceId", invoiceId);
                input.put("invoiceItemSeqId", invoiceItemSeqId);
                input.put("userLogin", userLogin);

                // create the invoice item
                Map<String, Object> results = dispatcher.runSync("createInvoiceItem", input);
                if (ServiceUtil.isError(results)) {
                    return results;
                }
                createInvoiceItemAssoc(agreement, invoiceId, item, invoiceItemSeqId);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Given a final list of InvoiceItem maps, groups and sorts them for the final invoice.  The ordering and grouping
     * is based on AgreementInvoiceItemTypeMap.  The parentInvoiceId and parentInvoiceItemSeqId will be discarded.
     * The amounts and quantities are merged such that there is 1 quantity of the merged amount.
     * TODO: no actual ordering yet (but they are grouped)
     */
    public static Map<String, Object> createGroupedInvoiceItems(GenericValue agreement, Map<String, Collection<Map<String, Object>>> items, String invoiceId, GenericValue userLogin, DispatchContext dctx) throws GeneralException {
        LocalDispatcher dispatcher = dctx.getDispatcher();

        // group the items
        Map<String, Map<String, Object>> groups = FastMap.newInstance();
        Map<String, List<Map<String, Object>>> assocs = FastMap.newInstance();
        for (String agreementTermId : items.keySet()) {
            for (Map<String, Object> item : items.get(agreementTermId)) {
                String key = item.get("invoiceItemTypeId") + (item.get("productId") == null ? "" : (String) item.get("productId"));
                Map<String, Object> group = groups.get(key);
                if (group == null) {
                    group = FastMap.newInstance();
                    group.putAll(item);
                    group.remove("parentInvoiceId");
                    group.remove("parentInvoiceItemSeqId");
                    groups.put(key, group);

                    // TODO: interesting bug will happen if you try doing assocs.put(key, Arrays.asList(item)) or with UtilMisc.  Why is it?  A java bug?
                    List<Map<String, Object>> assocItems = FastList.newInstance();
                    assocItems.add(item);
                    assocs.put(key, assocItems);
                } else {
                    BigDecimal quantity = (BigDecimal) item.get("quantity");
                    BigDecimal amount = (BigDecimal) item.get("amount");
                    BigDecimal total = quantity.multiply(amount).setScale(decimals, rounding);

                    BigDecimal gquantity = (BigDecimal) group.get("quantity");
                    BigDecimal gamount = (BigDecimal) group.get("amount");
                    BigDecimal gtotal = gquantity.multiply(gamount).setScale(decimals, rounding);

                    group.put("quantity", new BigDecimal("1.0"));
                    group.put("amount", gtotal.add(total));

                    List<Map<String, Object>> assocItems = assocs.get(key);
                    assocItems.add(item);
                }
            }
        }

        // create the grouped invoice items TODO this can be refactored with version above
        int sequence = 1;
        for (String key : groups.keySet()) {
            // have to create the item seq id here because it is not returned by service
            String invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(sequence++, 5);

            Map<String, Object> item = groups.get(key);
            item.put("invoiceId", invoiceId);
            item.put("invoiceItemSeqId", invoiceItemSeqId);
            item.put("userLogin", userLogin);
            Map<String, Object> results = dispatcher.runSync("createInvoiceItem", item);
            if (ServiceUtil.isError(results)) {
                return results;
            }

            // create associations for each group
            for (Map<String, Object> assocItem : assocs.get(key)) {
                createInvoiceItemAssoc(agreement, invoiceId, assocItem, invoiceItemSeqId);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Helper function to get the AgreementInvoiceItemTypes relevant to an agreement and keyed to the invoiceItemTypeIdFrom.
     */
    private static Map<String, GenericValue> getTypesToProcess(GenericValue agreement) throws GenericEntityException {
        Delegator delegator = agreement.getDelegator();
        List<GenericValue> typesToProcessList = delegator.findByAndCache("AgreementInvoiceItemType", UtilMisc.toMap("agreementTypeId", agreement.get("agreementTypeId")));
        Map<String, GenericValue> typeMap = FastMap.newInstance();
        for (GenericValue type : typesToProcessList) {
            typeMap.put(type.getString("invoiceItemTypeIdFrom"), type);
        }
        if (typeMap.size() == 0) {
            Debug.logWarning("Found no invoice item types that correspond to agreement type " + agreement.get("agreementTypeId") + ".  Please make sure AgreementInvoiceItemType entity has seed data loaded.", MODULE);
        }
        return typeMap;
    }

    private static void createInvoiceItemAssoc(GenericValue agreement, String invoiceId, Map<String, Object> item, String invoiceItemSeqId) throws GenericEntityException {
        Delegator delegator = agreement.getDelegator();

        BigDecimal amount = (BigDecimal) item.get("amount");
        amount = amount.multiply((BigDecimal) item.get("quantity"));
        amount = amount.setScale(decimals, rounding);

        GenericValue assoc = delegator.makeValue("InvoiceItemAssoc");
        assoc.put("invoiceItemAssocId", delegator.getNextSeqId("InvoiceItemAssoc"));
        assoc.put("agreementId", agreement.get("agreementId"));
        assoc.put("invoiceIdTo", invoiceId);
        assoc.put("invoiceItemSeqIdTo", invoiceItemSeqId);
        assoc.put("invoiceIdFrom", item.get("parentInvoiceId"));
        assoc.put("invoiceItemSeqIdFrom", item.get("parentInvoiceItemSeqId"));
        assoc.put("amount", amount);
        assoc.create();
    }

    // creates agreement term billing records for each invoice that commission was earned on
    private static void createAgreementBilling(GenericValue agreement, String paymentApplicationId, String invoiceId, Map<String, Collection<Map<String, Object>>> invoiceItems, DomainsLoader dl) throws GeneralException {
        InvoiceRepositoryInterface invoiceRepository = dl.loadDomainsDirectory().getBillingDomain().getInvoiceRepository();
        Delegator delegator = agreement.getDelegator();

        // add up the commission earned per parent invoice
        Map<String, BigDecimal> commissionsEarned = new FastMap<String, BigDecimal>();
        for (Collection<Map<String, Object>> invoiceItemGroup : invoiceItems.values()) {
            for (Map<String, Object> invoiceItem : invoiceItemGroup) {
                String parentInvoiceId = (String) invoiceItem.get("parentInvoiceId");
                BigDecimal total = commissionsEarned.get(parentInvoiceId);
                if (total == null) {
                    total = BigDecimal.ZERO;
                }
                total = total.add(getItemAmount(invoiceItem));
                commissionsEarned.put(parentInvoiceId, total);
            }
        }

        // make records for parent invoices with positive commission earned
        for (String parentInvoiceId : commissionsEarned.keySet()) {
            BigDecimal commissionEarned = commissionsEarned.get(parentInvoiceId);
            if (commissionEarned.signum() <= 0) {
                continue;
            }

            Invoice parentInvoice = invoiceRepository.getInvoiceById(parentInvoiceId);

            Map<String, Object> input = new FastMap<String, Object>();
            input.put("agreementTermBillingId", delegator.getNextSeqId("AgreementTermBilling"));
            input.put("agreementId", agreement.get("agreementId"));
            input.put("invoiceId", invoiceId);
            input.put("amount", commissionEarned);
            input.put("origPaymentApplicationId", paymentApplicationId);
            input.put("origInvoiceId", parentInvoiceId);
            input.put("agentPartyId", agreement.get("partyIdTo"));
            input.put("billingDatetime", UtilDateTime.nowTimestamp());
            input.put("origAmount", parentInvoice.getInvoiceTotal());
            if (UtilValidate.isNotEmpty(paymentApplicationId)) {
                GenericValue application = delegator.findByPrimaryKeyCache("PaymentApplication", UtilMisc.toMap("paymentApplicationId", paymentApplicationId));
                input.put("origPaymentAmount", application.get("amountApplied"));
            }
            GenericValue billing = delegator.makeValue("AgreementTermBilling", input);
            billing.create();
        }
    }
}
