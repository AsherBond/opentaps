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
package org.opentaps.common.forms;

import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;

/**
 * Merge Form services. The service documentation is in services_forms.xml.
 *
 * @author <a href="mailto:richard.a.coss@state.or.us">Richard Coss</a>
 */
public class MergeFormServices {

    public static final String module = MergeFormServices.class.getName();
    public static final String errorResource = "OpentapsErrorLabels";

    /* For MergeForm */

    public static Map createMergeForm(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        Boolean privateForm = "Y".equals((String) context.get("privateForm"));

        GenericValue mergeForm = null;
        String mergeFormId = delegator.getNextSeqId("MergeForm");
        Map newMergeFormMap = UtilMisc.toMap("mergeFormId", mergeFormId);
        mergeForm = delegator.makeValue("MergeForm", newMergeFormMap);
        mergeForm.setNonPKFields(context);
        if (! privateForm) mergeForm.set("partyId", null);
        try {
            delegator.create(mergeForm);
        } catch (GenericEntityException e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_CreateMergeFormFail", locale);
            Debug.logError(errorMessage, module);
            return ServiceUtil.returnError(errorMessage);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map updateMergeForm(DispatchContext dctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        String mergeFormId = (String) context.get("mergeFormId");
        Boolean privateForm = "Y".equals((String) context.get("privateForm"));

        GenericValue mergeForm = null;
        try {
            Map newMergeFormMap = UtilMisc.toMap("mergeFormId", mergeFormId);
            mergeForm = delegator.findByPrimaryKey("MergeForm", newMergeFormMap);
            mergeForm.setNonPKFields(context);
            if ((! privateForm)) mergeForm.set("partyId", null);
            delegator.store(mergeForm);
        } catch (GenericEntityException e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_UpdateMergeFormFail", locale);
            Debug.logError(errorMessage, module);
            return ServiceUtil.returnError(errorMessage);
        }
        
        result.put("mergeFormId", mergeFormId);
        return result;
    }
    
    public static Map deleteMergeForm(DispatchContext dctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        String mergeFormId = (String) context.get("mergeFormId");

        try {
            delegator.removeByAnd("MergeForm", UtilMisc.toMap("mergeFormId", mergeFormId));
        } catch (GenericEntityException e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_DeleteMergeFormFail", locale);
            Debug.logError(errorMessage, module);
            return ServiceUtil.returnError(errorMessage);
        }

        return result;
    }

    /* For MergeFormCategory */

    public static Map createMergeFormCategory(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        GenericValue mergeFormCategory = null;
        String mergeFormCategoryId = delegator.getNextSeqId("MergeFormCategory");
        Map newMergeFormCategoryMap = UtilMisc.toMap("mergeFormCategoryId", mergeFormCategoryId);
        mergeFormCategory = delegator.makeValue("MergeFormCategory", newMergeFormCategoryMap);
        mergeFormCategory.setNonPKFields(context);

        try {
            delegator.create(mergeFormCategory);
        } catch (GenericEntityException e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_CreateMergeFormCategoryFail", locale);
            Debug.logError(errorMessage, module);
            return ServiceUtil.returnError(errorMessage);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map updateMergeFormCategory(DispatchContext dctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        String mergeFormCategoryId = (String) context.get("mergeFormCategoryId");

        GenericValue mergeFormCategory = null;
        try {
            Map newMergeFormCategoryMap = UtilMisc.toMap("mergeFormCategoryId", mergeFormCategoryId);
            mergeFormCategory = delegator.findByPrimaryKey("MergeFormCategory", newMergeFormCategoryMap);
            mergeFormCategory.setNonPKFields(context);
            delegator.store(mergeFormCategory);
        } catch (GenericEntityException e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_UpdateMergeFormCategoryFail", locale);
            Debug.logError(errorMessage, module);
            return ServiceUtil.returnError(errorMessage);
        }
        
        result.put("mergeFormCategoryId", mergeFormCategoryId);
        return result;
    }
    
    public static Map deleteMergeFormCategory(DispatchContext dctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        String mergeFormCategoryId = (String) context.get("mergeFormCategoryId");
        
        try {
            // first we remove all association MergeFormToCategory
            delegator.removeByAnd("MergeFormToCategory", UtilMisc.toMap("mergeFormCategoryId", mergeFormCategoryId));
            // then we can remove the category
            delegator.removeByAnd("MergeFormCategory", UtilMisc.toMap("mergeFormCategoryId", mergeFormCategoryId));
        } catch (GenericEntityException e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_DeleteMergeFormCategoryFail", locale);
            Debug.logError(errorMessage, module);
            return ServiceUtil.returnError(errorMessage);
        }

        return result;
    }
}
