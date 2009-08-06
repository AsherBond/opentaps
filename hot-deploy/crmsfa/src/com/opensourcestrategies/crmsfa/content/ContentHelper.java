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
package com.opensourcestrategies.crmsfa.content;

import java.util.*;

import javolution.util.FastList;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;

/**
 * Content Helper methods which are designed to provide a consistent set of APIs that can be reused by
 * higher level services.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */
public final class ContentHelper {

    private ContentHelper() { }

    private static final String MODULE = ContentHelper.class.getName();

    /**
     * Gets all active content metadata for a given CRMSFA party with the specified role and purpose.
     * This is useful for listing the content associated with the party.  By default, the
     * contentPurposeEnumId is PTYCNT_CRMSFA.  Return values are a List of ContentAndRole.
     * @param partyId the Party Id
     * @param roleTypeId the Party role type
     * @param contentPurposeEnumId a <code>String</code> value
     * @param delegator a <code>GenericDelegator</code> value
     * @return a <code>List</code> value
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static List<GenericValue> getContentInfoForParty(String partyId, String roleTypeId, String contentPurposeEnumId, GenericDelegator delegator) throws GenericEntityException {

        // First get the PartyContent with the desired purpose and build a list of contentIds from it
        List<GenericValue> contents = delegator.findByAnd("PartyContent", UtilMisc.toMap("partyId", partyId, "contentPurposeEnumId", contentPurposeEnumId));
        if (contents.size() == 0) {
            return FastList.newInstance();
        }

        Set<String> contentIds = new HashSet<String>();
        for (GenericValue content : contents) {
            contentIds.add(content.getString("contentId"));
        }

        // get the unexpired contents for the party in the given role
        EntityCondition conditions = new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("contentId", EntityOperator.IN, contentIds),
                    new EntityExpr("partyId", EntityOperator.EQUALS, partyId),
                    new EntityExpr("roleTypeId", EntityOperator.EQUALS, roleTypeId),
                    EntityUtil.getFilterByDateExpr()
                    ), EntityOperator.AND);
        return delegator.findByCondition("ContentAndRole", conditions, null, null);
    }

    /**
     * As above but specifically the default PTYCNT_CRMSFA content.
     * @param partyId the Party Id
     * @param roleTypeId the Party role type
     * @param delegator a <code>GenericDelegator</code> value
     * @return list of entities
     * @throws GenericEntityException if an error occurs
     */
    public static List<GenericValue> getContentInfoForParty(String partyId, String roleTypeId, GenericDelegator delegator) throws GenericEntityException {
        return getContentInfoForParty(partyId, roleTypeId, "PTYCNT_CRMSFA", delegator);
    }

    /**
     * Gets all active content metadata for a given Case.
     * @param custRequestId Id of the Case
     * @param delegator a <code>GenericDelegator</code> value
     * @return list of entities
     * @throws GenericEntityException if an error occurs
    */
    @SuppressWarnings("unchecked")
    public static List<GenericValue> getContentInfoForCase(String custRequestId, GenericDelegator delegator) throws GenericEntityException {
        return delegator.findByAnd("ContentAndCustRequest", Arrays.asList(new EntityExpr("custRequestId", EntityOperator.EQUALS, custRequestId), EntityUtil.getFilterByDateExpr()));
    }

    /**
     * Gets all active content metadata for a given Opportunity.
     * @param salesOpportunityId Id of the Opportunity
     * @param delegator a <code>GenericDelegator</code> value
     * @return list of entities
     * @throws GenericEntityException if an error occurs
    */
    @SuppressWarnings("unchecked")
    public static List<GenericValue> getContentInfoForOpportunity(String salesOpportunityId, GenericDelegator delegator) throws GenericEntityException {
        return delegator.findByAnd("ContentAndSalesOpportunity", Arrays.asList(new EntityExpr("salesOpportunityId", EntityOperator.EQUALS, salesOpportunityId), EntityUtil.getFilterByDateExpr()));
    }

    /**
     * Gets all active content metadata for a given Activity.
     * @param workEffortId Id of the Activity
     * @param delegator a <code>GenericDelegator</code> value
     * @return list of entities
     * @throws GenericEntityException if an error occurs
    */
    @SuppressWarnings("unchecked")
    public static List<GenericValue> getContentInfoForActivity(String workEffortId, GenericDelegator delegator) throws GenericEntityException {
        return delegator.findByAnd("ContentAndWorkEffort", Arrays.asList(new EntityExpr("workEffortId", EntityOperator.EQUALS, workEffortId), EntityUtil.getFilterByDateExpr()));
    }

    /**
     * Gets all active content metadata for a given Order.
     * @param orderId Id of the Order
     * @param delegator a <code>GenericDelegator</code> value
     * @return list of entities
     * @throws GenericEntityException if an error occurs
    */
    @SuppressWarnings("unchecked")
    public static List<GenericValue> getContentInfoForOrder(String orderId, GenericDelegator delegator) throws GenericEntityException {
        return delegator.findByAnd("ContentAndOrder", Arrays.asList(new EntityExpr("orderId", EntityOperator.EQUALS, orderId), EntityUtil.getFilterByDateExpr()));
    }

    /**
     * Gets all active content metadata for given quote.
     * @param quoteId quote identifier
     * @param delegator a <code>GenericDelegator</code> value
     * @return list of entities
     * @throws GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static List<GenericValue> getContentInfoForQuote(String quoteId, GenericDelegator delegator) throws GenericEntityException {
        return delegator.findByAnd("ContentAndQuote", Arrays.asList(new EntityExpr("quoteId", EntityOperator.EQUALS, quoteId), EntityUtil.getFilterByDateExpr()));
    }
}
