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
package com.opensourcestrategies.crmsfa.content;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

import com.opensourcestrategies.crmsfa.party.PartyHelper;
import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;

/**
 * Content services. The service documentation is in services_content.xml.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */
public final class ContentServices {

    private ContentServices() { }

    private static final String MODULE = ContentServices.class.getName();

    /**
     * Creates content for a party.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> createContentForParty(DispatchContext dctx, Map<String, Object> context) {
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = (String) context.get("partyId");
        String roleTypeId = (String) context.get("roleTypeId");
        String contentPurposeEnumId = (String) context.get("contentPurposeEnumId");
        if (contentPurposeEnumId == null) {
            contentPurposeEnumId = "PTYCNT_CRMSFA";
        }

        // figure out the CRMSFA role of the party if not specified
        if (roleTypeId == null) {
            try {
                roleTypeId = PartyHelper.getFirstValidCrmsfaPartyRoleTypeId(partyId, dctx.getDelegator());
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateContentFail", locale, MODULE);
            }
        }
        if (roleTypeId == null || !CrmsfaSecurity.hasPartyRelationSecurity(security, CrmsfaSecurity.getSecurityModuleForRole(roleTypeId), "_UPDATE", userLogin, partyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }
        context.put("roleTypeId", roleTypeId);
        context.put("contentPurposeEnumId", contentPurposeEnumId);
        context.put("partyContentTypeId", "USERDEF");
        Map<String, Object> results = createContent(dctx, context, "crmsfa.createPartyContent");
        if (ServiceUtil.isError(results)) {
            return results;
        }
        results.put("partyId", partyId);
        return results;
    }

    /**
     * Creates content for a case.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> createContentForCase(DispatchContext dctx, Map<String, Object> context) {
        String custRequestId = (String) context.get("custRequestId");
        if (!CrmsfaSecurity.hasCasePermission(dctx.getSecurity(), "_UPDATE", (GenericValue) context.get("userLogin"), custRequestId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", UtilCommon.getLocale(context), MODULE);
        }
        Map<String, Object> results = createContent(dctx, context, "crmsfa.createCustRequestContent");
        if (ServiceUtil.isError(results)) {
            return results;
        }
        results.put("custRequestId", custRequestId);
        return results;

    }

    /**
     * Creates content for an opportunity.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> createContentForOpportunity(DispatchContext dctx, Map<String, Object> context) {
        String salesOpportunityId = (String) context.get("salesOpportunityId");
        if (!CrmsfaSecurity.hasOpportunityPermission(dctx.getSecurity(), "_UPDATE", (GenericValue) context.get("userLogin"), salesOpportunityId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", UtilCommon.getLocale(context), MODULE);
        }
        Map<String, Object> results = createContent(dctx, context, "crmsfa.createSalesOpportunityContent");
        if (ServiceUtil.isError(results)) {
            return results;
        }
        results.put("salesOpportunityId", salesOpportunityId);
        return results;
    }

    /**
     * Creates content for an activity.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> createContentForActivity(DispatchContext dctx, Map<String, Object> context) {
        String workEffortId = (String) context.get("workEffortId");
        if (!CrmsfaSecurity.hasActivityPermission(dctx.getSecurity(), "_UPDATE", (GenericValue) context.get("userLogin"), workEffortId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", UtilCommon.getLocale(context), MODULE);
        }
        context.put("workEffortContentTypeId", "CREATED_MEDIA");
        Map<String, Object> results = createContent(dctx, context, "createWorkEffortContent");
        if (ServiceUtil.isError(results)) {
            return results;
        }
        results.put("workEffortId", workEffortId);
        return results;
    }

    /**
     * Parameterized create content service.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @param createContentAssocService a <code>String</code> value
     * @return the <code>Map</code> value.
     */
    private static Map<String, Object> createContent(DispatchContext dctx, Map<String, Object> context, String createContentAssocService) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);

        String contentTypeId = (String) context.get("contentTypeId");
        try {
            // what we do depends on if we're uploading a file or a URL
            String uploadServiceName = null;
            if ("FILE".equals(contentTypeId)) {
                uploadServiceName = "uploadFile";
            } else if ("HYPERLINK".equals(contentTypeId)) {
                uploadServiceName = "uploadUrl";
            } else {
                return ServiceUtil.returnSuccess();
            }

            ModelService service = dctx.getModelService(uploadServiceName);
            Map<String, Object> input = service.makeValid(context, "IN");
            // if upload order attach file, then add uploadFolder parameter
            String orderId = (String) context.get("orderId");
            if ("FILE".equals(contentTypeId) && orderId != null) {
                //specific upload folder, like Order_100011/
                input.put("uploadFolder", org.opentaps.common.content.ContentServices.ORDERCONTENT_PREV + orderId);
            }
            Map<String, Object> servResults = dispatcher.runSync(uploadServiceName, input);
            if (ServiceUtil.isError(servResults)) {
                return UtilMessage.createAndLogServiceError(servResults, "CrmErrorCreateContentFail", locale, MODULE);
            }
            String contentId = (String) servResults.get("contentId");
            // create the association between the content and the object
            service = dctx.getModelService(createContentAssocService);
            input = service.makeValid(context, "IN");
            input.put("contentId", contentId);
            servResults = dispatcher.runSync(createContentAssocService, input);
            return servResults;

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateContentFail", locale, MODULE);
        }
    }

    /**
     * Creates content for a party.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> createPartyContent(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        try {
            GenericValue value = delegator.makeValue("PartyContent");
            value.setPKFields(context);
            value.setNonPKFields(context);
            value.set("fromDate", context.get("fromDate") == null ? UtilDateTime.nowTimestamp() : context.get("fromDate"));
            value.create();

            value = delegator.makeValue("ContentRole");
            value.setPKFields(context);
            value.setNonPKFields(context);
            value.set("fromDate", context.get("fromDate") == null ? UtilDateTime.nowTimestamp() : context.get("fromDate"));
            value.create();
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateContentFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Creates content for a case.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> createCustRequestContent(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        try {
            GenericValue value = delegator.makeValue("CustRequestContent");
            value.setPKFields(context);
            value.setNonPKFields(context);
            if (context.get("fromDate") == null) {
                value.set("fromDate", UtilDateTime.nowTimestamp());
            }
            value.create();
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateContentFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Creates content for an opportunity.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> createSalesOpportunityContent(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        try {
            GenericValue value = delegator.makeValue("SalesOpportunityContent");
            value.setPKFields(context);
            value.setNonPKFields(context);
            if (context.get("fromDate") == null) {
                value.set("fromDate", UtilDateTime.nowTimestamp());
            }
            value.create();
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateContentFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Updates content for a party.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> updateContentForParty(DispatchContext dctx, Map<String, Object> context) {
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = (String) context.get("partyId");
        String roleTypeId = (String) context.get("roleTypeId");

        // figure out the CRMSFA role of the party if not specified
        if (roleTypeId == null) {
            try {
                roleTypeId = PartyHelper.getFirstValidCrmsfaPartyRoleTypeId(partyId, dctx.getDelegator());
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateContentFail", locale, MODULE);
            }
        }
        if (roleTypeId == null || !CrmsfaSecurity.hasPartyRelationSecurity(security, CrmsfaSecurity.getSecurityModuleForRole(roleTypeId), "_UPDATE", userLogin, partyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }
        Map<String, Object> results = updateContent(dctx, context);
        if (ServiceUtil.isError(results)) {
            return results;
        }
        results.put("partyId", partyId);
        return results;
    }

    /**
     * Updates content for a case.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> updateContentForCase(DispatchContext dctx, Map<String, Object> context) {
        String custRequestId = (String) context.get("custRequestId");
        if (!CrmsfaSecurity.hasCasePermission(dctx.getSecurity(), "_UPDATE", (GenericValue) context.get("userLogin"), custRequestId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", UtilCommon.getLocale(context), MODULE);
        }
        Map<String, Object> results = updateContent(dctx, context);
        if (ServiceUtil.isError(results)) {
            return results;
        }
        results.put("custRequestId", custRequestId);
        return results;
    }

    /**
     * Updates content for an opportunity.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> updateContentForOpportunity(DispatchContext dctx, Map<String, Object> context) {
        String salesOpportunityId = (String) context.get("salesOpportunityId");
        if (!CrmsfaSecurity.hasOpportunityPermission(dctx.getSecurity(), "_UPDATE", (GenericValue) context.get("userLogin"), salesOpportunityId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", UtilCommon.getLocale(context), MODULE);
        }
        Map<String, Object> results = updateContent(dctx, context);
        if (ServiceUtil.isError(results)) {
            return results;
        }
        results.put("salesOpportunityId", salesOpportunityId);
        return results;
    }

    /**
     * Updates content for an activity.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> updateContentForActivity(DispatchContext dctx, Map<String, Object> context) {
        String workEffortId = (String) context.get("workEffortId");
        if (!CrmsfaSecurity.hasActivityPermission(dctx.getSecurity(), "_UPDATE", (GenericValue) context.get("userLogin"), workEffortId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", UtilCommon.getLocale(context), MODULE);
        }
        Map<String, Object> results = updateContent(dctx, context);
        if (ServiceUtil.isError(results)) {
            return results;
        }
        results.put("workEffortId", workEffortId);
        return results;
    }

    /**
     * Updates content.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    private static Map<String, Object> updateContent(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // the forms control what gets updated an how, this service simply updates the contentName, description and url fields
        String contentId = (String) context.get("contentId");
        String classificationEnumId = (String) context.get("classificationEnumId");
        String contentName = (String) context.get("contentName");
        String description = (String) context.get("description");
        String url = (String) context.get("url");
        try {
            // first update content
            Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin, "contentId", contentId, "classificationEnumId", classificationEnumId, "contentName", contentName, "description", description);
            Map<String, Object> results = dispatcher.runSync("updateContent", input);
            if (ServiceUtil.isError(results)) {
                return UtilMessage.createAndLogServiceError(results, "CrmErrorUpdateContentFail", locale, MODULE);
            }

            // if url is supplied, then we update the related DataResource
            if (UtilValidate.isNotEmpty(url)) {
                GenericValue content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
                GenericValue dataResource = content.getRelatedOne("DataResource");
                if (dataResource != null) {
                    input = UtilMisc.toMap("userLogin", userLogin, "dataResourceId", dataResource.get("dataResourceId"), "objectInfo", url);
                    results = dispatcher.runSync("updateDataResource", input);
                    if (ServiceUtil.isError(results)) {
                        return UtilMessage.createAndLogServiceError(results, "CrmErrorUpdateContentFail", locale, MODULE);
                    }
                }
            }

            return ServiceUtil.returnSuccess();
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateContentFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateContentFail", locale, MODULE);
        }
    }

    /**
     * Removes content.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> removeContent(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String contentId = (String) context.get("contentId");
        String partyId = (String) context.get("partyId");
        String workEffortId = (String) context.get("workEffortId");
        String custRequestId = (String) context.get("custRequestId");
        String salesOpportunityId = (String) context.get("salesOpportunityId");
        String orderId = (String) context.get("orderId");
        String quoteId = (String) context.get("quoteId");

        if (UtilValidate.isEmpty(partyId) && UtilValidate.isEmpty(workEffortId) && UtilValidate.isEmpty(custRequestId) && UtilValidate.isEmpty(salesOpportunityId) && UtilValidate.isEmpty(orderId) && UtilValidate.isEmpty(quoteId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }

        try {
            boolean hasPermission = false;
            if (partyId != null) {
                String roleTypeId = PartyHelper.getFirstValidCrmsfaPartyRoleTypeId(partyId, delegator);
                hasPermission = (roleTypeId != null && CrmsfaSecurity.hasPartyRelationSecurity(security, CrmsfaSecurity.getSecurityModuleForRole(roleTypeId), "_UPDATE", userLogin, partyId));
            } else if (workEffortId != null) {
                hasPermission = CrmsfaSecurity.hasActivityPermission(dctx.getSecurity(), "_UPDATE", (GenericValue) context.get("userLogin"), workEffortId);
            } else if (custRequestId != null) {
                hasPermission = CrmsfaSecurity.hasCasePermission(dctx.getSecurity(), "_UPDATE", (GenericValue) context.get("userLogin"), custRequestId);
            } else if (salesOpportunityId != null) {
                hasPermission = CrmsfaSecurity.hasOpportunityPermission(dctx.getSecurity(), "_UPDATE", (GenericValue) context.get("userLogin"), salesOpportunityId);
            } else if (orderId != null) {
                hasPermission = CrmsfaSecurity.hasOrderPermission(dctx.getSecurity(), "_UPDATE", (GenericValue) context.get("userLogin"), orderId);
            } else if (quoteId != null) {
                hasPermission = security.hasEntityPermission("CRMSFA_QUOTE", "_UPDATE", userLogin);
            }
            if (!hasPermission) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }

            // We're going to expire the content relationship first
            String entityName = null;
            List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("contentId", contentId), EntityUtil.getFilterByDateExpr());
            if (partyId != null) {
                entityName = "ContentRole";
                conditions.add(EntityCondition.makeCondition("partyId", partyId));
            } else if (workEffortId != null) {
                entityName = "WorkEffortContent";
                conditions.add(EntityCondition.makeCondition("workEffortId", workEffortId));
            } else if (custRequestId != null) {
                entityName = "CustRequestContent";
                conditions.add(EntityCondition.makeCondition("custRequestId", custRequestId));
            } else if (salesOpportunityId != null) {
                entityName = "SalesOpportunityContent";
                conditions.add(EntityCondition.makeCondition("salesOpportunityId", salesOpportunityId));
            } else if (orderId != null) {
                entityName = "OrderHeaderContent";
                conditions.add(EntityCondition.makeCondition("orderId", orderId));
            } else if (quoteId != null) {
                entityName = "QuoteContent";
                conditions.add(EntityCondition.makeCondition("quoteId", quoteId));
            }
            List<GenericValue> relationships = delegator.findByAnd(entityName, conditions);
            if (relationships.size() == 0) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }
            for (GenericValue relationship : relationships) {
                relationship.set("thruDate", UtilDateTime.nowTimestamp());
                relationship.store();
            }

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorRemoveContentFail", locale, MODULE);
        }
    }

    /**
     * Create OrderHeaderContent for an order.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> createOrderContent(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        try {
            String contentId = (String) context.get("contentId");
            String orderId = (String) context.get("orderId");
            List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("contentId", contentId), EntityUtil.getFilterByDateExpr());
            conditions.add(EntityCondition.makeCondition("orderId", orderId));
            List<GenericValue> relationships = delegator.findByAnd("OrderHeaderContent", conditions);
            if (relationships.size() == 0) {
                // if not exist the OrderHeaderContent, then create one.
                GenericValue value = delegator.makeValue("OrderHeaderContent");
                value.setPKFields(context);
                value.setNonPKFields(context);
                if (context.get("fromDate") == null) {
                    value.set("fromDate", UtilDateTime.nowTimestamp());
                }
                value.create();
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateContentFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Creates content for an order.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> createContentForOrder(DispatchContext dctx, Map<String, Object> context) {
        String orderId = (String) context.get("orderId");
        Map<String, Object> results = createContent(dctx, context, "crmsfa.createOrderContent");
        if (ServiceUtil.isError(results)) {
            return results;
        }
        results.put("orderId", orderId);
        return results;
    }

    /**
     * Updates content for an order.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> updateContentForOrder(DispatchContext dctx, Map<String, Object> context) {
        String orderId = (String) context.get("orderId");
        Map<String, Object> results = updateContent(dctx, context);
        if (ServiceUtil.isError(results)) {
            return results;
        }
        results.put("orderId", orderId);
        return results;
    }

    /**
     * Creates content for a quote.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> createContentForQuote(DispatchContext dctx, Map<String, Object> context) {
        String quoteId = (String) context.get("quoteId");
        Map<String, Object> results = createContent(dctx, context, "crmsfa.createQuoteContent");
        if (ServiceUtil.isError(results)) {
            return results;
        }
        results.put("quoteId", quoteId);
        return results;
    }

    /**
     * Updates content for a quote .
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> updateContentForQuote(DispatchContext dctx, Map<String, Object> context) {
        String quoteId = (String) context.get("quoteId");
        Map<String, Object> results = updateContent(dctx, context);
        if (ServiceUtil.isError(results)) {
            return results;
        }
        results.put("quoteId", quoteId);
        return results;
    }

    /**
     * Creates content for a quote.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map<String, Object> createQuoteContent(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        String contentId = (String) context.get("contentId");
        String quoteId = (String) context.get("quoteId");

        try {
            List<EntityCondition> conditions = UtilMisc.toList(EntityCondition.makeCondition("contentId", EntityOperator.EQUALS, contentId), EntityUtil.getFilterByDateExpr());
            conditions.add(EntityCondition.makeCondition("quoteId", EntityOperator.EQUALS, quoteId));

            List<GenericValue> relationships = delegator.findByAnd("QuoteContent", conditions);
            if (UtilValidate.isEmpty(relationships)) {
                // create if not exist.
                GenericValue value = delegator.makeValue("QuoteContent");
                value.setPKFields(context);
                value.setNonPKFields(context);
                if (context.get("fromDate") == null) {
                    value.set("fromDate", UtilDateTime.nowTimestamp());
                }
                value.create();
            }

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateContentFail", locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

}
