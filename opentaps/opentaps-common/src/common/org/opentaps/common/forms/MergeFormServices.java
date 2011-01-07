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
package org.opentaps.common.forms;

import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;

/**
 * Merge Form services. The service documentation is in services_forms.xml.
 */
public final class MergeFormServices {

    private MergeFormServices() { }

    private static final String MODULE = MergeFormServices.class.getName();
    public static final String errorResource = "OpentapsErrorLabels";

    /* For MergeForm */

    public static Map<String, Object> createMergeForm(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        Boolean privateForm = "Y".equals(context.get("privateForm"));

        GenericValue mergeForm = null;
        String mergeFormId = delegator.getNextSeqId("MergeForm");
        Map<String, Object> newMergeFormMap = UtilMisc.<String, Object>toMap("mergeFormId", mergeFormId);
        mergeForm = delegator.makeValue("MergeForm", newMergeFormMap);
        mergeForm.setNonPKFields(context);
        if (!privateForm) {
            mergeForm.set("partyId", null);
        }
        try {
            delegator.create(mergeForm);
        } catch (GenericEntityException e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_CreateMergeFormFail", locale);
            Debug.logError(errorMessage, MODULE);
            return ServiceUtil.returnError(errorMessage);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateMergeForm(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        String mergeFormId = (String) context.get("mergeFormId");
        Boolean privateForm = "Y".equals(context.get("privateForm"));

        GenericValue mergeForm = null;
        try {
            Map<String, Object> newMergeFormMap = UtilMisc.<String, Object>toMap("mergeFormId", mergeFormId);
            mergeForm = delegator.findByPrimaryKey("MergeForm", newMergeFormMap);
            mergeForm.setNonPKFields(context);
            if ((!privateForm)) {
                mergeForm.set("partyId", null);
            }
            delegator.store(mergeForm);
        } catch (GenericEntityException e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_UpdateMergeFormFail", locale);
            Debug.logError(errorMessage, MODULE);
            return ServiceUtil.returnError(errorMessage);
        }

        result.put("mergeFormId", mergeFormId);
        return result;
    }

    public static Map<String, Object> deleteMergeForm(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        String mergeFormId = (String) context.get("mergeFormId");

        try {
            // first we remove all association MergeFormToCategory
            delegator.removeByAnd("MergeFormToCategory", UtilMisc.toMap("mergeFormId", mergeFormId));
            // then remove the form
            delegator.removeByAnd("MergeForm", UtilMisc.toMap("mergeFormId", mergeFormId));
        } catch (GenericEntityException e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_DeleteMergeFormFail", locale);
            Debug.logError(errorMessage, MODULE);
            return ServiceUtil.returnError(errorMessage);
        }

        return result;
    }

    /* For MergeFormCategory */

    public static Map<String, Object> createMergeFormCategory(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        GenericValue mergeFormCategory = null;
        String mergeFormCategoryId = delegator.getNextSeqId("MergeFormCategory");
        Map<String, Object> newMergeFormCategoryMap = UtilMisc.<String, Object>toMap("mergeFormCategoryId", mergeFormCategoryId);
        mergeFormCategory = delegator.makeValue("MergeFormCategory", newMergeFormCategoryMap);
        mergeFormCategory.setNonPKFields(context);

        try {
            delegator.create(mergeFormCategory);
        } catch (GenericEntityException e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_CreateMergeFormCategoryFail", locale);
            Debug.logError(errorMessage, MODULE);
            return ServiceUtil.returnError(errorMessage);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateMergeFormCategory(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        String mergeFormCategoryId = (String) context.get("mergeFormCategoryId");

        GenericValue mergeFormCategory = null;
        try {
            Map<String, Object> newMergeFormCategoryMap = UtilMisc.<String, Object>toMap("mergeFormCategoryId", mergeFormCategoryId);
            mergeFormCategory = delegator.findByPrimaryKey("MergeFormCategory", newMergeFormCategoryMap);
            mergeFormCategory.setNonPKFields(context);
            delegator.store(mergeFormCategory);
        } catch (GenericEntityException e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_UpdateMergeFormCategoryFail", locale);
            Debug.logError(errorMessage, MODULE);
            return ServiceUtil.returnError(errorMessage);
        }

        result.put("mergeFormCategoryId", mergeFormCategoryId);
        return result;
    }

    public static Map<String, Object> deleteMergeFormCategory(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        String mergeFormCategoryId = (String) context.get("mergeFormCategoryId");

        try {
            // first we remove all association MergeFormToCategory
            delegator.removeByAnd("MergeFormToCategory", UtilMisc.toMap("mergeFormCategoryId", mergeFormCategoryId));
            // then we can remove the category
            delegator.removeByAnd("MergeFormCategory", UtilMisc.toMap("mergeFormCategoryId", mergeFormCategoryId));
        } catch (GenericEntityException e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_DeleteMergeFormCategoryFail", locale);
            Debug.logError(errorMessage, MODULE);
            return ServiceUtil.returnError(errorMessage);
        }

        return result;
    }
}
