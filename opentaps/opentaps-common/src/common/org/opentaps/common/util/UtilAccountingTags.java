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

package org.opentaps.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.entities.AcctgTagEnumType;
import org.opentaps.base.entities.Enumeration;
import org.opentaps.base.entities.EnumerationType;
import org.opentaps.common.domain.organization.OrganizationRepository;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * UtilAccountingTags - Utilities for the accounting tag system.
 */
public final class UtilAccountingTags {

    @SuppressWarnings("unused")
    private static final String MODULE = UtilAccountingTags.class.getName();

    /** Number of tags defined in <code>AcctgTagEnumType</code>. */
    public static final int TAG_COUNT = 10;

    /** The standard prefix used in to post tag values, eg: the value for enumTypeId1 is posted as ${TAG_PARAM_PREFIX}1. */
    public static final String TAG_PARAM_PREFIX = "tag";
    /** Prefix for accounting tags on all the entities. */
    public static final String ENTITY_TAG_PREFIX = "acctgTagEnumId";

    /** Tags for financial reports. */
    public static final String FINANCIALS_REPORTS_TAG = "FINANCIALS_REPORTS";
    /** Tags for the balance reports. */
    public static final String BALANCE_REPORTS_TAG = "BALANCE_REPORTS";
    /** Tags for purchase invoices. */
    public static final String PURCHASE_INVOICES_TAG = "PRCH_INV_ITEMS";
    /** Tags for sales invoices. */
    public static final String SALES_INVOICES_TAG = "SALES_INV_ITEMS";
    /** Tags for return invoices. */
    public static final String RETURN_INVOICES_TAG = "RETN_INV_ITEMS";
    /** Tags for commission invoices. */
    public static final String COMMISSION_INVOICES_TAG = "COMM_INV_ITEMS";
    /** Tags for lookup purchase invoices. */
    public static final String LOOKUP_PURCHASE_INVOICES_TAG = "PRCH_INV_L_ITEMS";
    /** Tags for lookup sales invoices. */
    public static final String LOOKUP_SALES_INVOICES_TAG = "SALES_INV_L_ITEMS";
    /** Tags for lookup commission invoices. */
    public static final String LOOKUP_COMMISSION_INVOICES_TAG = "COMM_INV_L_ITEMS";
    /** Tags for purchase order. */
    public static final String PURCHASE_ORDER_TAG = "PRCH_ORDER_ITEMS";
    /** Tags for sales order. */
    public static final String SALES_ORDER_TAG = "SALES_ORDER_ITEMS";
    /** Tags for disbursement payment. */
    public static final String DISBURSEMENT_PAYMENT_TAG = "DISBURSEMENT";
    /** Tags for receipt payment. */
    public static final String RECEIPT_PAYMENT_TAG = "RECEIPT";
    /** Tags for lookup receipt payment. */
    public static final String LOOKUP_RECEIPT_PAYMENT_TAG = "L_RECEIPT";
    /** Tags for lookup disbursement payment. */
    public static final String LOOKUP_DISBURSEMENT_PAYMENT_TAG = "L_DISBURSEMENT";
    /** Tags for transaction entries. */
    public static final String TRANSACTION_ENTRY_TAG = "TRANSACTION_ENTRY";
    /** Tags for check run feature. */
    public static final String CHECK_RUN_TAG = "CHECK_RUN";

    private UtilAccountingTags() { }

    /**
     * Gets the configured (non-null) Tag Types for the given organization, as a <code>Map</code> of
     * {index value: configured <code>enumTypeId</code>}.
     * @param organizationPartyId the organization party ID
     * @param accountingTagUsageTypeId the usage type for the tags
     * @param delegator a <code>Delegator</code> value
     * @return the <code>List</code> of <code>Map</code> for the given organization
     * @throws RepositoryException if an error occurs
     */
    public static Map<Integer, String> getAccountingTagTypesForOrganization(String organizationPartyId, String accountingTagUsageTypeId, Delegator delegator) throws RepositoryException {
        OrganizationRepository repository = new OrganizationRepository(delegator);
        return repository.getAccountingTagTypes(organizationPartyId, accountingTagUsageTypeId);
    }

    /**
     * Gets the configured Tag enums for the given organization, as a <code>List</code> of <code>Map</code> of
     * {<code>index</code>: configuration field index, <code>type</code>: <code>enumTypeId</code>, <code>description</code>: enum type description, <code>tagValues</code>: list of possible <code>Enumeration</code> values}.
     * @param organizationPartyId the organization party ID
     * @param accountingTagUsageTypeId the usage type for the tags
     * @param delegator a <code>Delegator</code> value
     * @return the <code>List</code> of <code>Map</code> for the given organization
     * @throws RepositoryException if an error occurs
     */
    public static List<AccountingTagConfigurationForOrganizationAndUsage> getAccountingTagsForOrganization(String organizationPartyId, String accountingTagUsageTypeId, Delegator delegator) throws RepositoryException {
        OrganizationRepository repository = new OrganizationRepository(delegator);
        return repository.getAccountingTagConfiguration(organizationPartyId, accountingTagUsageTypeId);
    }

    /**
     * Gets the configured Tag enums for the given organization plus the common filters ANY and NONE, as a <code>List</code> of <code>Map</code> of
     * {<code>index</code>: configuration field index, <code>type</code>: <code>enumTypeId</code>, <code>description</code>: enum type description, <code>tagValues</code>: list of possible <code>Enumeration</code> values}.
     * @param organizationPartyId the organization party ID
     * @param accountingTagUsageTypeId the usage type for the tags
     * @param delegator a <code>Delegator</code> value
     * @param locale the <code>Locale</code>, used for the generic filter labels
     * @return the <code>List</code> of <code>Map</code> for the given organization
     * @throws RepositoryException if an error occurs
     */
    public static List<AccountingTagConfigurationForOrganizationAndUsage> getAccountingTagFiltersForOrganization(String organizationPartyId, String accountingTagUsageTypeId, Delegator delegator, Locale locale) throws RepositoryException {

        List<AccountingTagConfigurationForOrganizationAndUsage> tagTypesAndValues = getAccountingTagsForOrganization(organizationPartyId, accountingTagUsageTypeId, delegator);
        // insert the common filters at the beginning of the value list for each tag configuration
        for (AccountingTagConfigurationForOrganizationAndUsage tag : tagTypesAndValues) {
            List<Enumeration> values = tag.getTagValues();
            List<Enumeration> activeValues = tag.getActiveTagValues();
            Enumeration any = new Enumeration();
            any.setEnumTypeId(tag.getType());
            any.setEnumId("ANY");
            any.setDescription(UtilMessage.expandLabel("CommonAny", locale));
            values.add(0, any);
            activeValues.add(0, any);
            Enumeration none = new Enumeration();
            none.setEnumTypeId(tag.getType());
            none.setEnumId("NONE");
            none.setDescription(UtilMessage.expandLabel("CommonNone", locale));
            values.add(1, none);
            activeValues.add(1, none);
        }
        return tagTypesAndValues;
    }

    /**
     * Builds a list of <code>EntityExpr</code> from the accounting tag given in the context.
     * @param organizationPartyId the organization party ID
     * @param accountingTagUsageTypeId the usage type for the tags
     * @param delegator a <code>Delegator</code> value
     * @param context a context <code>Map</code>
     * @return the list of <code>EntityExpr</code> from the accounting tag given in the context
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static List<EntityCondition> buildTagConditions(String organizationPartyId, String accountingTagUsageTypeId, Delegator delegator, Map context) throws RepositoryException {
        return buildTagConditions(organizationPartyId, accountingTagUsageTypeId, delegator, context, TAG_PARAM_PREFIX);
    }

    /**
     * Builds a list of <code>EntityExpr</code> from the accounting tag given in the context.
     * @param organizationPartyId the organization party ID
     * @param accountingTagUsageTypeId the usage type for the tags
     * @param delegator a <code>Delegator</code> value
     * @param context a context <code>Map</code>
     * @param prefix the part of parameters before the index which holds the values for the accounting tags in the context
     * @return the list of <code>EntityExpr</code> from the accounting tag given in the context
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static List<EntityCondition> buildTagConditions(String organizationPartyId, String accountingTagUsageTypeId, Delegator delegator, Map context, String prefix) throws RepositoryException {
        return buildTagConditions(organizationPartyId, accountingTagUsageTypeId, delegator, context, prefix, "acctgTagEnumId");
    }

    /**
     * Builds a list of <code>EntityExpr</code> from the accounting tag given in the context.
     * @param organizationPartyId the organization party ID
     * @param accountingTagUsageTypeId the usage type for the tags
     * @param delegator a <code>Delegator</code> value
     * @param context a context <code>Map</code>
     * @param prefix the part of parameters before the index which holds the values for the accounting tags in the context
     * @param entityPrefix the part of the field name before the index which holds the values for the accounting tags in the entity to query
     * @return the list of <code>EntityExpr</code> from the accounting tag given in the context
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static List<EntityCondition> buildTagConditions(String organizationPartyId, String accountingTagUsageTypeId, Delegator delegator, Map context, String prefix, String entityPrefix) throws RepositoryException {
        Map<Integer, String> tagTypes = getAccountingTagTypesForOrganization(organizationPartyId, accountingTagUsageTypeId, delegator);
        List<EntityCondition> conditions = new ArrayList<EntityCondition>();

        // get the values from the request
        for (Integer index : tagTypes.keySet()) {
            String value = (String) context.get(prefix + index);
            if (value != null) {
                /*
                 * Two tag values have special meaning:
                 * ANY: any value of the accounting tag, including null
                 *   or
                 * NONE: only values were the accounting tag is null
                 */
                if ("ANY".equals(value)) {
                    continue;
                } else if ("NONE".equals(value)) {
                    conditions.add(EntityCondition.makeCondition(entityPrefix + index, EntityOperator.EQUALS, null));
                } else {
                    conditions.add(EntityCondition.makeCondition(entityPrefix + index, EntityOperator.EQUALS, value));
                }

            }
        }

        return conditions;
    }

    /**
     * Builds a list of <code>EntityExpr</code> from the accounting tag given in the request.
     * @param organizationPartyId the organization party ID
     * @param accountingTagUsageTypeId the usage type for the tags
     * @param delegator a <code>Delegator</code> value
     * @param request a <code>HttpServletRequest</code> value
     * @return the list of <code>EntityExpr</code> from the accounting tag given in the request
     * @throws RepositoryException if an error occurs
     */
    public static List<EntityCondition> buildTagConditions(String organizationPartyId, String accountingTagUsageTypeId, Delegator delegator, HttpServletRequest request) throws RepositoryException {
        return buildTagConditions(organizationPartyId, accountingTagUsageTypeId, delegator, request, TAG_PARAM_PREFIX);
    }

    /**
     * Builds a list of <code>EntityExpr</code> from the accounting tag given in the request.
     * @param organizationPartyId the organization party ID
     * @param accountingTagUsageTypeId the usage type for the tags
     * @param delegator a <code>Delegator</code> value
     * @param request a <code>HttpServletRequest</code> value
     * @param prefix the part of parameters before the index which holds the values for the accounting tags in the request
     * @return the list of <code>EntityExpr</code> from the accounting tag given in the request
     * @throws RepositoryException if an error occurs
     */
    public static List<EntityCondition> buildTagConditions(String organizationPartyId, String accountingTagUsageTypeId, Delegator delegator, HttpServletRequest request, String prefix) throws RepositoryException {
        return buildTagConditions(organizationPartyId, accountingTagUsageTypeId, delegator, request, prefix, "acctgTagEnumId");
    }

    /**
     * Builds a list of <code>EntityExpr</code> from the accounting tag given in the request.
     * @param organizationPartyId the organization party ID
     * @param accountingTagUsageTypeId the usage type for the tags
     * @param delegator a <code>Delegator</code> value
     * @param request a <code>HttpServletRequest</code> value
     * @param prefix the part of parameters before the index which holds the values for the accounting tags in the request
     * @param entityPrefix the part of the field name before the index which holds the values for the accounting tags in the entity to query
     * @return the list of <code>EntityExpr</code> from the accounting tag given in the request
     * @throws RepositoryException if an error occurs
     */
    public static List<EntityCondition> buildTagConditions(String organizationPartyId, String accountingTagUsageTypeId, Delegator delegator, HttpServletRequest request, String prefix, String entityPrefix) throws RepositoryException {
        Map<Integer, String> tagTypes = getAccountingTagTypesForOrganization(organizationPartyId, accountingTagUsageTypeId, delegator);
        List<EntityCondition> conditions = new ArrayList<EntityCondition>();

        // get the values from the request
        for (Integer index : tagTypes.keySet()) {
            String value = UtilCommon.getParameter(request, prefix + index);
            if (value != null) {
                /*
                 * Two tag values have special meaning:
                 * ANY: any value of the accounting tag, including null
                 *   or
                 * NONE: only values were the accounting tag is null
                 */
                if ("ANY".equals(value)) {
                    continue;
                } else if ("NONE".equals(value)) {
                    conditions.add(EntityCondition.makeCondition(entityPrefix + index, EntityOperator.EQUALS, null));
                } else {
                    conditions.add(EntityCondition.makeCondition(entityPrefix + index, EntityOperator.EQUALS, value));
                }
            }
        }

        return conditions;
    }

    /**
     * Adds the given tags from the request to the given <code>Map</code>.
     * Useful when building a parameter <code>Map</code> for a service call.
     * @param request a <code>HttpServletRequest</code> value
     * @param context a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static void addTagParameters(HttpServletRequest request, Map context) {
        addTagParameters(request, context, TAG_PARAM_PREFIX);
    }


    /**
     * Adds the given tags from the request to the given <code>Map</code>.
     * Useful when building a parameter <code>Map</code> for a service call.
     * @param request a <code>HttpServletRequest</code> value
     * @param context a <code>Map</code> value
     * @param prefix the part of parameters before the index which holds the values for the accounting tags in the request
     */
    @SuppressWarnings("unchecked")
    public static void addTagParameters(HttpServletRequest request, Map context, String prefix) {
        for (int i = 1; i <= TAG_COUNT; i++) {
            context.put(TAG_PARAM_PREFIX + i, UtilCommon.getParameter(request, prefix + i));
        }
    }

    /**
     * Adds the given tags from the input <code>Map</code> to the given <code>Map</code>.
     * Useful when building a parameter <code>Map</code> for a service call inside a service.
     * @param input a <code>Map</code> value
     * @param context a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static void addTagParameters(Map input, Map context) {
        addTagParameters(input, context, TAG_PARAM_PREFIX);
    }

    /**
     * Adds the given tags from the input <code>Map</code> to the given <code>Map</code>.
     * Useful when building a parameter <code>Map</code> for a service call inside a service.
     * @param input a <code>Map</code> value
     * @param context a <code>Map</code> value
     * @param inputPrefix the part of parameters before the index which holds the values for the accounting tags in the input map
     */
    @SuppressWarnings("unchecked")
    public static void addTagParameters(Map input, Map context, String inputPrefix) {
        addTagParameters(input, context, inputPrefix, TAG_PARAM_PREFIX);
    }

    /**
     * Adds the given tags from the input <code>Map</code> to the given <code>Map</code>.
     * Useful when building a parameter <code>Map</code> for a service call inside a service.
     * @param input a <code>Map</code> value
     * @param context a <code>Map</code> value
     * @param inputPrefix the part of parameters before the index which holds the values for the accounting tags in the input map
     * @param outputPrefix the part of parameters before the index which holds the values for the accounting tags in the output map
     */
    @SuppressWarnings("unchecked")
    public static void addTagParameters(Map input, Map context, String inputPrefix, String outputPrefix) {
        for (int i = 1; i <= TAG_COUNT; i++) {
            if (input.containsKey(inputPrefix + i)) {
                context.put(outputPrefix + i, input.get(inputPrefix + i));
            }
        }
    }

    /**
     * Gets the tag Maps from the input <code>Map</code>, which is used in services using string-map-prefix.
     * @param input a <code>Map</code> value
     * @return the array of <code>Map</code> of accounting tags found
     */
    @SuppressWarnings("unchecked")
    public static Map[] getTagMapParameters(Map input) {
        return getTagMapParameters(input, "tagsMap");
    }

    /**
     * Gets the tag Maps from the input <code>Map</code>, which is used in services using string-map-prefix.
     * @param input a <code>Map</code> value
     * @param prefix the part of parameters before the index which holds the tag Map in the request
     * @return the array of <code>Map</code> of accounting tags found
     */
    @SuppressWarnings("unchecked")
    public static Map[] getTagMapParameters(Map input, String prefix) {
        Map[] tagMaps = new Map[TAG_COUNT];
        for (int i = 1; i <= TAG_COUNT; i++) {
            if (input.containsKey(prefix + i)) {
                tagMaps[i - 1] = (Map) input.get(prefix + i);
            }
        }
        return tagMaps;
    }

    /**
     * Gets the given tags from the input <code>Map</code>.
     * @param input a <code>Map</code> value
     * @return the <code>Map</code> of accounting tags found
     */
    @SuppressWarnings("unchecked")
    public static Map getTagParameters(Map input) {
        return getTagParameters(input, TAG_PARAM_PREFIX);
    }

    /**
     * Adds the given tags from the input <code>Map</code> to the given <code>Map</code>.
     * Useful when building a parameter <code>Map</code> for a service call inside a service.
     * @param input a <code>Map</code> value
     * @param prefix the part of parameters before the index which holds the values for the accounting tags in the request
     * @return the <code>Map</code> of accounting tags found
     */
    @SuppressWarnings("unchecked")
    public static Map getTagParameters(Map input, String prefix) {
        Map tags = new HashMap();
        for (int i = 1; i <= TAG_COUNT; i++) {
            if (input.containsKey(prefix + i) && UtilValidate.isNotEmpty(input.get(prefix + i))) {
                tags.put("tag" + i, input.get(prefix + i));
            }
        }
        return tags;
    }

    /**
     * Copy all the acctgTagEnumId_ fields from a <code>Map</code> to a <code>Map</code>.
     * @param value the input <code>Map</code>
     * @param target the output <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static void putAllAccountingTags(Map value, Map target) {
        putAllAccountingTags((Map) value, (Map) target, ENTITY_TAG_PREFIX);
    }

    /**
     * Copy all the acctgTagEnumId_ fields from a <code>GenericValue</code> to a <code>Map</code>.
     * @param value the input <code>GenericValue</code>
     * @param target the output <code>GenericValue</code>
     */
    @SuppressWarnings("unchecked")
    public static void putAllAccountingTags(GenericValue value, GenericValue target) {
        putAllAccountingTags(value, (Map) target, ENTITY_TAG_PREFIX);
    }

    /**
     * Copy all the acctgTagEnumId_ fields from a <code>GenericValue</code> to a <code>Map</code>.
     * @param value the input <code>GenericValue</code>
     * @param map the output <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static void putAllAccountingTags(GenericValue value, Map map) {
        putAllAccountingTags(value, map, ENTITY_TAG_PREFIX);
    }

    /**
     * Copy all the acctgTagEnumId_ fields from a <code>GenericValue</code> to a <code>Map</code>.
     * @param value the input <code>GenericValue</code>
     * @param map the output <code>Map</code>
     * @param mapPrefix the prefix used in the output map, defaults to ENTITY_TAG_PREFIX
     */
    @SuppressWarnings("unchecked")
    public static void putAllAccountingTags(GenericValue value, Map map, String mapPrefix) {
        for (int i = 1; i <= TAG_COUNT; i++) {
            map.put(mapPrefix + i, value.get(ENTITY_TAG_PREFIX + i));
        }
    }

    /**
     * Copy all the acctgTagEnumId_ fields from a <code>Map</code> to a <code>Map</code>.
     * @param value the input <code>Map</code>
     * @param map the output <code>Map</code>
     * @param mapPrefix the prefix used in the output map, defaults to ENTITY_TAG_PREFIX
     */
    @SuppressWarnings("unchecked")
    public static void putAllAccountingTags(Map value, Map map, String mapPrefix) {
        for (int i = 1; i <= TAG_COUNT; i++) {
            map.put(mapPrefix + i, value.get(ENTITY_TAG_PREFIX + i));
        }
    }

    /**
     * Copy all the acctgTagEnumId_ fields from a <code>GenericValue</code> to a <code>Map</code>.
     * @param value the input <code>GenericValue</code>
     * @param map the output <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static void putAllAccountingTags(Entity value, Map map) {
        putAllAccountingTags(value, map, ENTITY_TAG_PREFIX);
    }

    /**
     * Copy all the acctgTagEnumId_ fields from a <code>GenericValue</code> to a <code>Map</code>.
     * @param value the input <code>GenericValue</code>
     * @param map the output <code>Map</code>
     * @param mapPrefix the prefix used in the output map, defaults to ENTITY_TAG_PREFIX
     */
    @SuppressWarnings("unchecked")
    public static void putAllAccountingTags(Entity value, Map map, String mapPrefix) {
        for (int i = 1; i <= TAG_COUNT; i++) {
            map.put(mapPrefix + i, value.get(ENTITY_TAG_PREFIX + i));
        }
    }

    /**
     * Copy all the acctgTagEnumId_ fields from a <code>Map</code> to a <code>GenericValue</code>.
     * @param map the input <code>Map</code>
     * @param value the output <code>GenericValue</code>
     */
    @SuppressWarnings("unchecked")
    public static void putAllAccountingTags(Map map, GenericValue value) {
        putAllAccountingTags(map, value, ENTITY_TAG_PREFIX);
    }

    /**
     * Copy all the acctgTagEnumId_ fields from a <code>Map</code> to an <code>EntityInterface</code>.
     * @param map the input <code>Map</code>
     * @param value the output <code>EntityInterface</code>
     */
    @SuppressWarnings("unchecked")
    public static void putAllAccountingTags(Map map, EntityInterface value) {
        putAllAccountingTags(map, value, ENTITY_TAG_PREFIX);
    }

    /**
     * Copy all the acctgTagEnumId_ fields from a <code>Map</code> to an <code>GenericValue</code>.
     * @param map the input <code>Map</code>
     * @param value the output <code>GenericValue</code>
     * @param mapPrefix the prefix used in the input map, defaults to ENTITY_TAG_PREFIX
     */
    @SuppressWarnings("unchecked")
    public static void putAllAccountingTags(Map map, GenericValue value, String mapPrefix) {
        for (int i = 1; i <= TAG_COUNT; i++) {
            String tag = (String) map.get(mapPrefix + i);
            // make sure not to set empty strings
            if (UtilValidate.isEmpty(tag)) {
                tag = null;
            }
            value.put(ENTITY_TAG_PREFIX + i, tag);
        }
    }

    /**
     * Copy all the acctgTagEnumId_ fields from a <code>Map</code> to an <code>EntityInterface</code>.
     * @param map the input <code>Map</code>
     * @param value the output <code>EntityInterface</code>
     * @param mapPrefix the prefix used in the input map, defaults to ENTITY_TAG_PREFIX
     */
    public static void putAllAccountingTags(Map<String, String> map, EntityInterface value, String mapPrefix) {
        for (int i = 1; i <= TAG_COUNT; i++) {
            String tag = map.get(mapPrefix + i);
            // make sure not to set empty strings
            if (UtilValidate.isEmpty(tag)) {
                tag = null;
            }
            value.set(ENTITY_TAG_PREFIX + i, tag);
        }
    }

    /**
     * Compare the tags in the given entity and the tags in the given map.
     * @param value the <code>GenericValue</code>
     * @param map the map of tags
     * @param mapPrefix the prefix used in the map
     * @return <code>true</code> if all tags are equal
     */
    public static boolean sameAccountingTags(GenericValue value, Map<String, String> map, String mapPrefix) {
        for (int i = 1; i <= TAG_COUNT; i++) {
            String mapValue = map.get(mapPrefix + i);
            String entityValue = value.getString(ENTITY_TAG_PREFIX + i);
            // we do not use UtilObject.equalsHelper has we can consider empty and null as the same
            // both empty or null:
            if (UtilValidate.isEmpty(entityValue) && UtilValidate.isEmpty(mapValue)) {
                continue;
            }
            // one is empty or null:
            if (UtilValidate.isEmpty(entityValue) && UtilValidate.isNotEmpty(mapValue)) {
                return false;
            }
            if (UtilValidate.isNotEmpty(entityValue) && UtilValidate.isEmpty(mapValue)) {
                return false;
            }
            // none is empty or null:
            if (!entityValue.equals(mapValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Format tags as string to further usage in reports.
     */
    public static String formatTagsAsString(HttpServletRequest request, String usageTypeId, Delegator delegator) throws RepositoryException {
        Locale locale = UtilHttp.getLocale(request);
        String organizationPartyId = (String) request.getSession().getAttribute("organizationPartyId");
        List<AccountingTagConfigurationForOrganizationAndUsage> tagsData = UtilAccountingTags.getAccountingTagFiltersForOrganization(organizationPartyId, UtilAccountingTags.FINANCIALS_REPORTS_TAG, delegator, locale);
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);

        StringBuilder sb = new StringBuilder("");

        for (int i = 1; i <= TAG_COUNT; i++) {
            String reportTag = UtilCommon.getParameter(request, TAG_PARAM_PREFIX + i);
            if (UtilValidate.isEmpty(reportTag)) {
                continue;
            }

            boolean found = false;
            int counter = 1;
            for (AccountingTagConfigurationForOrganizationAndUsage tagDesc : tagsData) {

                if (counter == i) {
                    if ("ANY".equals(reportTag)) {
                        sb.append(tagDesc.getDescription()).append(": ");
                        sb.append(uiLabelMap.get("CommonAny")).append('\n');
                        break;
                    } else if ("NONE".equals(reportTag)) {
                        sb.append(tagDesc.getDescription()).append(": ");
                        sb.append(uiLabelMap.get("CommonNone")).append('\n');
                        break;
                    }
                }

                if (!"ANY".equals(reportTag) && !"NONE".equals(reportTag)) {
                    List<Enumeration> tagValues = tagDesc.getTagValues();
                    for (Enumeration value : tagValues) {
                        if (reportTag.equals(value.getEnumId())) {
                            sb.append(tagDesc.getDescription()).append(": ");
                            sb.append(value.getDescription()).append('\n');
                            found = true;
                            break;
                        }
                    }
                }

                counter++;
                if (found) {
                    break;
                }
            }
        }

        if (sb.length() > 0) {
            return sb.toString().substring(0, sb.length() - 1);
        } else {
            return "";
        }
    }

    public static List<EnumerationType> getAllAccoutingTagEnumerationTypes(Delegator delegator) throws RepositoryException {
        OrganizationRepository repository = new OrganizationRepository(delegator);
        return repository.findList(EnumerationType.class, repository.map(EnumerationType.Fields.parentTypeId, "ACCOUNTING_TAG"), Arrays.asList(EnumerationType.Fields.enumTypeId.asc()));
    }

    public static Map<EnumerationType, List<Enumeration>> getAllAccoutingTagEnumerations(Delegator delegator) throws RepositoryException {
        OrganizationRepository repository = new OrganizationRepository(delegator);
        List<EnumerationType> enumerationTypes = getAllAccoutingTagEnumerationTypes(delegator);
        List<Enumeration> enumerations = repository.findList(Enumeration.class, EntityCondition.makeCondition(Enumeration.Fields.enumTypeId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(enumerationTypes, EnumerationType.Fields.enumTypeId)), Arrays.asList(Enumeration.Fields.sequenceId.asc()));
        return Entity.groupByFieldValues(EnumerationType.class, enumerations, Enumeration.Fields.enumTypeId, enumerationTypes, EnumerationType.Fields.enumTypeId);
    }

    public static LinkedHashSet<String> getAllAccoutingTagEnumerationTypeIdsForOrganization(String organizationPartyId, Delegator delegator) throws RepositoryException {
        OrganizationRepository repository = new OrganizationRepository(delegator);
        LinkedHashSet<String> typeIds = new LinkedHashSet<String>();
        List<AcctgTagEnumType> tagTypes = repository.findList(AcctgTagEnumType.class, repository.map(AcctgTagEnumType.Fields.organizationPartyId, organizationPartyId));
        for (AcctgTagEnumType type : tagTypes) {
            for (int i = 1; i <= TAG_COUNT; i++) {
                typeIds.add(type.getString("enumTypeId" + i));
            }
        }
        return typeIds;
    }

    public static Map<EnumerationType, List<Enumeration>> getAllAccoutingTagEnumerationsForOrganization(String organizationPartyId, Delegator delegator) throws RepositoryException {
        OrganizationRepository repository = new OrganizationRepository(delegator);
        LinkedHashSet<String> typeIds = getAllAccoutingTagEnumerationTypeIdsForOrganization(organizationPartyId, delegator);
        List<EnumerationType> enumerationTypes = repository.findList(EnumerationType.class, EntityCondition.makeCondition(EnumerationType.Fields.enumTypeId.name(), EntityOperator.IN, typeIds));
        List<Enumeration> enumerations = repository.findList(Enumeration.class, EntityCondition.makeCondition(Enumeration.Fields.enumTypeId.name(), EntityOperator.IN, typeIds), Arrays.asList(Enumeration.Fields.sequenceId.asc()));
        return Entity.groupByFieldValues(EnumerationType.class, enumerations, Enumeration.Fields.enumTypeId, enumerationTypes, EnumerationType.Fields.enumTypeId);
    }

    /**
     * Checks if there is at least one tag set in the given Map, excluding the <code>ANY</code> special value.
     * @param input a <code>Map</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean areTagsSet(Map<String, Object> input) {
        return areTagsSet(input, TAG_PARAM_PREFIX);
    }

    /**
     * Checks if there is at least one tag set in the given Map, excluding the <code>ANY</code> special value.
     * @param input a <code>Map</code> value
     * @param prefix the part of parameters before the index which holds the values for the accounting tags in the request
     * @return a <code>boolean</code> value
     */
    public static boolean areTagsSet(Map<String, Object> input, String prefix) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        for (Object v : getTagParameters(input, prefix).values()) {
            if (v != null && !"".equals(v) && !"ANY".equals(v)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return if set any accounting tag on the entity
     * @param entity a <code>Map</code> value
     * @param tags a <code>List<AccountingTagConfigurationForOrganizationAndUsage></code> value
     * @return a <code>boolean</code> value
     */
    public static boolean hasSetAccountingTags(Map entity, List<AccountingTagConfigurationForOrganizationAndUsage> tags) {
    	for (AccountingTagConfigurationForOrganizationAndUsage tag : tags) {
            if(UtilValidate.isNotEmpty(entity.get("acctgTagEnumId" + tag.getIndex()))) {
            	// find a tag was set, then return true
            	return true;
            }
        }
    	// return false when not found any tag was set
    	return false;
    }
}
