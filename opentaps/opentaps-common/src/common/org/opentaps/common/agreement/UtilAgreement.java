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

import javolution.util.FastList;
import javolution.util.FastSet;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.builder.EntityListBuilder;
import org.opentaps.common.builder.ListBuilder;
import org.opentaps.common.builder.PageBuilder;

import java.util.*;

/**
 * Some utility methods related to Agreements.
 */
public final class UtilAgreement {

    private UtilAgreement() { }

    private static final String MODULE = AgreementServices.class.getName();

    /**
     * This method controls what fields are available for each agreement term type.
     * @deprecated Use <code>getValidFields(String, Delegator)</code> instead.  This will always use the "default" delegator
     * @param termTypeId a <code>String</code> value
     * @return a <code>List</code> value
     */
    public static List<String> getValidFields(String termTypeId) {
        Debug.logWarning("Deprecated method UtilAgreement.getValidFields(termTypeId) called: You should be using getValidFields(termTypeId, delegator)", MODULE);
        return getValidFields(termTypeId, DelegatorFactory.getDelegator("default"));
    }

    /**
     * This method controls what fields are available for each agreement term type.
     * It will first use TermTypeFields entity but if none are found there then it will use a hardcoded entries in this method
     * Returns a List of fields for AgreementTerm which should be displayed for each termTypeId defined in TermType
     * If null then assume that all fields should be displayed
     * @param termTypeId String
     * @param delegator a <code>Delegator</code> value
     * @return a <code>List</code> value
     */
    public static List<String> getValidFields(String termTypeId, Delegator delegator) {

        List<String> fieldNames = new ArrayList<String>();
        try {
            GenericValue termTypeFields = delegator.findByPrimaryKeyCache("TermTypeFields", UtilMisc.toMap("termTypeId", termTypeId));
            if (UtilValidate.isNotEmpty(termTypeFields)) {
                // don't look -- this is ugly but necessary ;)
                for (int i = 1; i < 9; i++) {
                    if (UtilValidate.isNotEmpty(termTypeFields.get("field" + i))) {
                        fieldNames.add(termTypeFields.getString("field" + i));
                    }
                }
            }
        } catch (GenericEntityException ex) {
            Debug.logError("Exception while trying to get term type fields for [" + termTypeId + "]", MODULE);
            // but continue on--maybe it's one of the ones below?
        }

        if (UtilValidate.isNotEmpty(fieldNames)) {
            return fieldNames;
        }

        // note it's better to keep a separate return list for each termTypeId in case it changes later
        if ("PURCH_VENDOR_ID".equals(termTypeId)) {
            return Arrays.asList("textValue");
        } else if ("PURCH_FREIGHT".equals(termTypeId)) {
            return Arrays.asList("textValue");
        } else if ("FIN_PAYMENT_TERM".equals(termTypeId)) {
            return Arrays.asList("termDays");
        } else if ("FIN_PAYMENT_FIXDAY".equals(termTypeId)) {
            return Arrays.asList("termValue", "minQuantity");
        } else if ("FIN_PAYMENT_DISC".equals(termTypeId)) {
            return Arrays.asList("termDays", "termValue");
        } else if ("FIN_LATE_FEE_TERM".equals(termTypeId)) {
            return Arrays.asList("termDays");
        } else if ("CREDIT_LIMIT".equals(termTypeId)) {
            return Arrays.asList("termValue", "currencyUomId");
        } else if ("PROD_CAT_COMMISSION".equals(termTypeId)) {
            return Arrays.asList("termValue", "productCategoryId", "description", "minQuantity", "maxQuantity");
        } else if ("PROD_GRP_COMMISSION".equals(termTypeId)) {
            return Arrays.asList("termValue", "productCategoryId", "description", "minQuantity", "maxQuantity");
        } else if ("PRODUCT_COMMISSION".equals(termTypeId)) {
            return Arrays.asList("productId", "termValue", "minQuantity", "maxQuantity");
        } else if ("FLAT_COMMISSION".equals(termTypeId)) {
            return Arrays.asList("termValue");
        } else if ("COMM_PARTYCLASS_APPL".equals(termTypeId)) {
            return Arrays.asList("partyClassificationGroupId");
        } else if ("COMM_PARTY_APPL".equals(termTypeId)) {
            return Arrays.asList("partyId");
        } else if ("COMM_ORDER_ROLE".equals(termTypeId)) {
            return Arrays.asList("roleTypeId");
        } else {
            Debug.logError("No valid fields found for agreement term type [" + termTypeId + "]", MODULE);
            return null;
        }
    }

    /**
     * Method getTermsByItemId returns list of GenericValues that present agreement term
     * types allowed for given agreementItem.
     *
     * @param agreementItemTypeId String
     * @param agreementId a <code>String</code> value
     * @param agreementItemSeqId a <code>String</code> value
     * @param delegator Delegator
     * @return List
     * @exception GenericEntityException if an error occurs
     */
    public static List<GenericValue> getTermsByItemType(String agreementItemTypeId, String agreementId, String agreementItemSeqId, Delegator delegator) throws GenericEntityException {

        List<String> selectList = UtilMisc.toList("termTypeId", "maxAllowed");
        List<GenericValue> termsMap = delegator.findByConditionCache("AgreementItemToTermMap", EntityCondition.makeCondition("agreementItemTypeId", agreementItemTypeId), selectList, null);
        List<String> listTermTypeId = FastList.newInstance();
        Iterator<GenericValue> iter = termsMap.iterator();
        while (iter.hasNext()) {
            GenericValue termsMapItem = iter.next();
            Long maxAllowed = termsMapItem.getLong("maxAllowed");
            long maxAllowedLng = 0;
            if (maxAllowed != null) {
                maxAllowedLng = maxAllowed.longValue();
            }

            long count = delegator.findCountByAnd("AgreementTerm", UtilMisc.toMap("agreementId", agreementId, "agreementItemSeqId", agreementItemSeqId, "termTypeId", termsMapItem.getString("termTypeId")));
            if (count < maxAllowedLng || maxAllowed == null) {
                listTermTypeId.add(termsMapItem.getString("termTypeId"));
            }
        }
        if (listTermTypeId.size() > 0) {
            List<GenericValue> listTermTypes = delegator.findByConditionCache("TermType", EntityCondition.makeCondition("termTypeId", EntityOperator.IN, listTermTypeId), null, null);
            if (listTermTypes == null) {
                return FastList.<GenericValue>newInstance();
            } else {
                return listTermTypes;
            }
        }
        return FastList.newInstance();
    }


    /**
     * Checks if an invoice is covered by an agreement.
     * @param invoice a <code>GenericValue</code> value
     * @param agreement a <code>GenericValue</code> value
     * @return a <code>boolean</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static boolean isInvoiceCoveredByAgreement(GenericValue invoice, GenericValue agreement) throws GenericEntityException {
        if (invoice == null) {
            throw new IllegalArgumentException("Must supply invoice when checking if invoice is covered by agreement.");
        }
        if (agreement == null) {
            throw new IllegalArgumentException("Must supply agreement when checking if invoice is covered by agreement.");
        }
        Delegator delegator = agreement.getDelegator();
        if ("COMMISSION_AGREEMENT".equals(agreement.get("agreementTypeId"))) {
            List<GenericValue> customerTerms = delegator.findByAnd("AgreementAndItemAndTerm", UtilMisc.toMap("agreementId", agreement.get("agreementId"), "agreementItemTypeId", "COMM_CUSTOMERS"));
            boolean isInvoiceCovered = false;
            for (Iterator<GenericValue> iter = customerTerms.iterator(); iter.hasNext();) {
                GenericValue term = iter.next();

                // check if the agent can earn commission on all orders where he or she is named
                if ("COMM_ORDER_ROLE".equals(term.get("termTypeId")) && "COMMISSION_AGENT".equals(term.get("roleTypeId"))) {
                    Debug.logInfo("Invoice [" + invoice.get("invoiceId") + "] is covered by agreement term [ " + term.get("agreementTermId") + "]", MODULE);
                    isInvoiceCovered = true;
                    break;
                } else {
                    Debug.logInfo("Agreement term [" + term.get("agreementTermId") + "] is not a COMM_ORDER_ROLE term for COMMISSION_AGENT.  Term type is [" + term.get("termTypeId") + "] Role type is [" + term.get("roleTypeId") + "]", MODULE);
                }

                // check if the agent can earn commission for any order created for the customer
                if ("COMM_PARTY_APPL".equals(term.get("termTypeId")) && invoice.get("partyId").equals(term.get("partyId"))) {
                    Debug.logInfo("Invoice [" + invoice.get("invoiceId") + "] is covered by agreement term [ " + term.get("agreementTermId") + "]", MODULE);
                    isInvoiceCovered = true;
                    break;
                } else {
                    Debug.logInfo("Agreement term [" + term.get("agreementTermId") + "] with party ID [" + term.get("partyId") + "] would not apply to invoice with party ID [" + invoice.get("partyId") + "]", MODULE);
                }
            }

            // done with all checks for commission agreements, return results
            Debug.logInfo("returning [" + isInvoiceCovered + "] for whether agreement [" + agreement.get("agreementId") + "] is covered by invoice [" + invoice.get("invoiceId") + "]", MODULE);
            return isInvoiceCovered;
        } else if ("PARTNER_AGREEMENT".equals(agreement.get("agreementTypeId"))) {
            return true;
        }
        // TODO should really throw an exception
        Debug.logWarning("Unable to determine if invoice [" + invoice.get("invoiceId") + "] is covered by agreement [" + agreement.get("agreementId") + "].", MODULE);
        return false;
    }

    /**
     * Checks an agreement if commission is earned on payment or when invoice is set to ready.  By default,
     * commission is earned when invoices are set to ready.  To make it on payment, ensure that there is an agreement item
     * of type COMM_TIMING with a term type of COMM_TIMING_AT and value of COMM_AT_PAYMENT.
     * @param agreement a <code>GenericValue</code> value
     * @return a <code>boolean</code> value
     * @exception GeneralException if an error occurs
     */
    public static boolean isCommissionEarnedOnPayment(GenericValue agreement) throws GeneralException {
        List<GenericValue> commTimingTerms = agreement.getDelegator().findByAnd("AgreementAndItemAndTerm", UtilMisc.toMap("agreementId", agreement.get("agreementId"), "agreementItemTypeId", "COMM_TIMING", "termTypeId", "COMM_TIMING_AT"));
        if (UtilValidate.isNotEmpty(commTimingTerms)) {
            GenericValue term = EntityUtil.getFirst(commTimingTerms);
            if ("COMM_AT_PAYMENT".equals(term.getString("valueEnumId"))) {
                Debug.logInfo("Commission is earned on payment for agreement [" + agreement.get("agreementId") + "]", MODULE);
                return true;
            }
        }
        Debug.logInfo("Commission is not earned on payment for agreement [" + agreement.get("agreementId") + "]", MODULE);
        return false;
    }

    /**
     * Method helps to build lists of agreements based on conditions.
     * Returns EntityListBuilder for use in paginator.
     *
     * @param conditions an <code>EntityCondition</code> value
     * @param orderBy a <code>List</code> value
     * @param locale a <code>Locale</code> value
     * @return a <code>ListBuilder</code> value
     */
    public static ListBuilder getAgreementsListBuilder(EntityCondition conditions, List<String> orderBy, final Locale locale) {

        PageBuilder pageBuilder = new PageBuilder() {
            @SuppressWarnings("unchecked")
            public List<Map<String, Object>> build(List page) throws GenericEntityException {
                List<Map<String, Object>> newPage = new ArrayList<Map<String, Object>>();
                for (GenericValue agreement : (List<GenericValue>) page) {
                    Map<String, Object> row = new HashMap<String, Object>();
                    row.putAll(agreement);
                    row.put("statusDescription", agreement.getRelatedOne("StatusItem").get("description", locale));
                    newPage.add(row);
                }
                return newPage;
            };
        };

        EntityListBuilder listBuilder = new EntityListBuilder("Agreement", conditions, orderBy);
        listBuilder.setPageBuilder(pageBuilder);

        return listBuilder;
    }

    /**
     * Gets the commission agent partyIds which have agreements that cover any invoice between the organization and the customer.
     * These agreements have a COMM_PARTY_APPL term which identifies the customer party.
     * @param organizationPartyId a <code>String</code> value
     * @param customerPartyId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @return the <code>Set</code> of commission agent ids for the given customer and organization
     * @exception GenericEntityException if an error occurs
     */
    public static Set<String> getCommissionAgentIdsForCustomer(String organizationPartyId, String customerPartyId, Delegator delegator) throws GenericEntityException {
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
            EntityCondition.makeCondition("termTypeId", "COMM_PARTY_APPL"),
            EntityCondition.makeCondition("partyId", customerPartyId)
        );
        return getActiveCommissionAgentIds(organizationPartyId, conditions, delegator);
    }

    /**
     * Gets the agent partyIds that have agreements which allow them to earn commission on orders they place for
     * the organization.  These agreements have a COMM_ORDER_ROLE term that implies coverage of all orders.
     * @param organizationPartyId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @return the <code>Set</code> of commission agent ids for the given organization
     * @exception GenericEntityException if an error occurs
     */
    public static Set<String> getCommissionAgentIdsForOrganizationOrders(String organizationPartyId, Delegator delegator) throws GenericEntityException {
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
            EntityCondition.makeCondition("termTypeId", "COMM_ORDER_ROLE"),
            EntityCondition.makeCondition("roleTypeId", "COMMISSION_AGENT")
        );
        return getActiveCommissionAgentIds(organizationPartyId, conditions, delegator);
    }


    /**
     * Checks if the given agent has an agreement where they can earn commission on orders they place for the
     * organization.  These agreements have a COMM_ORDER_ROLE term that implies coverage of all orders.
     * @param organizationPartyId a <code>String</code> value
     * @param agentPartyId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @return a <code>boolean</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static boolean doesAgentEarnCommissionForOrganizationOrders(String organizationPartyId, String agentPartyId, Delegator delegator) throws GenericEntityException {
        Set<String> agentIds = getCommissionAgentIdsForOrganizationOrders(organizationPartyId, delegator);
        return agentIds.contains(agentPartyId);
    }

    /**
     * Get the active commission agent partyIds which have agreements with an organization.  Specify additional
     * constraints to narrow down the results.  This is really meant to be used by other UtilAgreement methods.
     * @param organizationPartyId a <code>String</code> value
     * @param constraintConditions an <code>EntityCondition</code> value
     * @param delegator a <code>Delegator</code> value
     * @return the <code>Set</code> of commission agent ids for the given organization and constraintConditions
     * @exception GenericEntityException if an error occurs
     */
    public static Set<String> getActiveCommissionAgentIds(String organizationPartyId, EntityCondition constraintConditions, Delegator delegator) throws GenericEntityException {
        Set<String> commissionAgentIds = FastSet.newInstance();

        List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(
            EntityCondition.makeCondition("statusId", "AGR_ACTIVE"),
            EntityCondition.makeCondition("partyIdFrom", organizationPartyId),
            EntityCondition.makeCondition("roleTypeIdFrom", "INTERNAL_ORGANIZATIO"),
            EntityCondition.makeCondition("roleTypeIdTo", "COMMISSION_AGENT"),
            EntityCondition.makeCondition("agreementTypeId", "COMMISSION_AGREEMENT"),
            EntityCondition.makeCondition("agreementItemTypeId", "COMM_CUSTOMERS")
        );
        conditions.add(EntityUtil.getFilterByDateExpr());
        conditions.add(EntityUtil.getFilterByDateExpr("termFromDate", "termThruDate"));
        conditions.add(constraintConditions);

        List<GenericValue> terms = delegator.findByAnd("AgreementAndItemAndTerm", conditions);
        for (GenericValue term : terms) {
            commissionAgentIds.add(term.getString("partyIdTo"));
        }
        return commissionAgentIds;
    }

}
