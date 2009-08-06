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

package org.opentaps.common.content;

import java.io.File;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.content.data.DataResourceWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

/**
 * ContentServices - A simplified, pragmatic set of services for uploading files and URLs.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */

public class ContentServices {
    //the order folder's prev string
    public static final String ORDERCONTENT_PREV = "Order_";
    public static final String module = ContentServices.class.getName();

    public static Map uploadFile(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String mimeTypeId = (String) context.get("_uploadedFile_contentType");
        if (mimeTypeId != null && mimeTypeId.length() > 60) {
            // XXX This is a fix to avoid problems where an OS gives us a mime type that is too long to fit in 60 chars
            // (ex. MS .xlsx as application/vnd.openxmlformats-officedoucment.spreadsheetml.sheet)
            Debug.logWarning("Truncating mime type ["+mimeTypeId+"] to 60 characters.", module);
            mimeTypeId = mimeTypeId.substring(0,60);
        }
        String fileName = (String) context.get("_uploadedFile_fileName");
        String contentName = (String) context.get("contentName");
        String uploadFolder = (String) context.get("uploadFolder");

        if (contentName == null || contentName.trim().length() == 0) contentName = fileName;
        try {
            if (uploadFolder != null) {
             // if specific upload path, then use real file name as upload file name
                String uploadPath = getDataResourceContentUploadPath(uploadFolder);
                String fileAndPath = uploadPath + "/" + contentName;
                //if exist same file already, just updating the Content.description and the DataResource entity the original Content is pointing to
                List conditions = UtilMisc.toList(
                        new EntityExpr("objectInfo", EntityOperator.EQUALS, fileAndPath)
                        );
                List<GenericValue> dataResources = delegator.findByAnd("DataResource", conditions);
                if (dataResources.size() > 0) {
                    // exit same file already then using updateFile method to update DataResource and Content entities.
                    GenericValue dataResource = dataResources.get(0);
                    return updateFile(dataResource, dctx, context);
                }
            }
            
            // if not exist same file in DataResource entities, then create the base data resource
            GenericValue dataResource = initializeDataResource(delegator, userLogin);
            String dataResourceId = dataResource.getString("dataResourceId");

            // identify this resource as a local server file with the given mime type and filename (note that the filename here can be arbitrary)
            dataResource.set("dataResourceTypeId", "LOCAL_FILE");
            dataResource.set("mimeTypeId", mimeTypeId);
            dataResource.set("dataResourceName", contentName);

            // Get the upload path, which is configured as content.upload.path.prefix in content.properties
            String uploadPath = getDataResourceContentUploadPath(uploadFolder);

            // Our local filename will be named after the ID, but without any extensions because we're relying on the download service to take care of the mimeTypeId and filename
            String fileAndPath = uploadPath + "/" + dataResourceId;
            if(uploadFolder != null) {
                // if specific upload path, then use real file name as upload file name
                fileAndPath = uploadPath + "/" + contentName;
            }

            dataResource.set("objectInfo", fileAndPath);
            dataResource.create();

            // save the file to the system using the ofbiz service
            Map input = UtilMisc.toMap("dataResourceId", dataResourceId, "binData", context.get("uploadedFile"), "dataResourceTypeId", "LOCAL_FILE", "objectInfo", fileAndPath);
            Map results = dispatcher.runSync("createAnonFile", input);
            if (ServiceUtil.isError(results)) return results;

            // wrap up by creating the Content object
            context.put("contentName", contentName);
            return createContentFromDataResource(delegator, dataResource, context);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }
    
    /**
     * upload file and contech which relate the dataResource.
     * @param dataResource a <code>GenericValue</code> value
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    private static Map updateFile(GenericValue dataResource, DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String contentName = (String) context.get("contentName");
        String fileName = (String) context.get("_uploadedFile_fileName");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (contentName == null || contentName.trim().length() == 0) contentName = fileName;
        // update the file to the system using the ofbiz service
        Map input = UtilMisc.toMap("binData", context.get("uploadedFile"), "dataResourceTypeId", "LOCAL_FILE", "objectInfo", dataResource.getString("objectInfo"),"userLogin", userLogin);
        try {
            String dataResourceId = dataResource.getString("dataResourceId");
            Debug.logInfo("Find same file at server path, using updateFile to update DataResource and Content entities by DataResource [" + dataResourceId + "].", module);
            Map results = dispatcher.runSync("updateFile", input);
            if (ServiceUtil.isError(results)) return results;
            // wrap up by creating the Content object
            context.put("contentName", contentName);
            return updateContentFromDataResource(delegator, dataResource, context);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
       
    }

    /**
     * get DataResourceContent upload path.if specific uploadFolder then use it, else use ofbiz DataResourceWorker.getDataResourceContentUploadPath to get upload path.
     * @param uploadFolder a <code>String</code> value
     * @return the <code>String</code> upload path value.
     */    
    public static String getDataResourceContentUploadPath(String uploadFolder) {
        if(uploadFolder == null) {
            return DataResourceWorker.getDataResourceContentUploadPath();
        }
        String initialPath = UtilProperties.getPropertyValue("content.properties", "content.upload.path.prefix");
        String ofbizHome = System.getProperty("ofbiz.home");
        if (!initialPath.startsWith("/")) {
            initialPath = "/" + initialPath;
        }
        File root = new File(ofbizHome + initialPath);
        if (!root.exists()) {
            boolean created = root.mkdir();
            if (!created) {
                Debug.logWarning("Unable to create upload folder [" + ofbizHome + initialPath + "].", module);
            }
        }
        String parentFolder = ofbizHome + initialPath + "/"  + uploadFolder;
        File parent = new File(parentFolder);
        if (!parent.exists()) {
            boolean created = parent.mkdir();
            if (!created) {
                Debug.logWarning("Unable to create upload folder [" + parentFolder + "].", module);
            }
        }
        Debug.log("Directory Name : " + parentFolder, module);
        return parentFolder.replace('\\','/');
    }

    public static Map uploadUrl(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String url = (String) context.get("url");
        String contentName = (String) context.get("contentName");
        if (contentName == null || contentName.trim().length() == 0) contentName = url;
        try {
            // first create the base data resource
            GenericValue dataResource = initializeDataResource(delegator, userLogin);

            // identify this resource as a plain text URL
            dataResource.set("dataResourceTypeId", "URL_RESOURCE");
            dataResource.set("mimeTypeId", "text/plain");

            // store the actual URL and name
            dataResource.set("objectInfo", url);
            dataResource.set("dataResourceName", contentName);
            dataResource.create();

            // wrap up by creating the Content object
            return createContentFromDataResource(delegator, dataResource, context);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    // Creates a basic data resource to fill out by the respective consumer services
    private static GenericValue initializeDataResource(GenericDelegator delegator, GenericValue userLogin) throws GenericEntityException {
        Map input = FastMap.newInstance();
        input.put("dataResourceId", delegator.getNextSeqId("DataResource"));
        input.put("statusId", "CTNT_PUBLISHED");
        input.put("createdDate", UtilDateTime.nowTimestamp());
        input.put("createdByUserLogin", userLogin.get("userLoginId"));
        return delegator.makeValue("DataResource", input);
    }

    private static Map createContentFromDataResource(GenericDelegator delegator, GenericValue dataResource, Map context) throws GenericEntityException {
        String contentTypeId = null;
        if ("LOCAL_FILE".equals(dataResource.get("dataResourceTypeId"))) {
            contentTypeId = "FILE";
        } else if ("URL_RESOURCE".equals(dataResource.get("dataResourceTypeId"))) {
            contentTypeId = "HYPERLINK";
        } else {
            return ServiceUtil.returnError("Data resource type ["+dataResource.get("dataResourceTypeId")+"] not supported.");
        }
        String contentName = (String) context.get("contentName");
        if (contentName == null || contentName.trim().length() == 0) contentName = (String) dataResource.get("dataResourceName");

        GenericValue content = delegator.makeValue("Content", null);
        String contentId = delegator.getNextSeqId("Content");
        content.set("contentId", contentId);
        content.set("createdDate", dataResource.get("createdDate")); 
        content.set("dataResourceId", dataResource.get("dataResourceId"));
        content.set("statusId", dataResource.get("statusId"));
        content.set("contentName", dataResource.get("dataResourceName"));
        content.set("mimeTypeId", dataResource.get("mimeTypeId"));
        content.set("contentTypeId", contentTypeId);
        content.set("contentName", contentName);
        content.set("description", context.get("description"));
        content.set("createdByUserLogin", context.get("createdByUserLoginId"));
        content.set("classificationEnumId", context.get("classificationEnumId"));
        content.create();

        Map results = ServiceUtil.returnSuccess();
        results.put("contentId", contentId);
        return results;
    }
    
    /**
     * update content which relate the dataResource.
     * @param delegator a <code>GenericDelegator</code> value
     * @param dataResource a <code>GenericValue</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    private static Map updateContentFromDataResource(GenericDelegator delegator, GenericValue dataResource, Map context) throws GenericEntityException {
        List conditions = UtilMisc.toList(
                new EntityExpr("dataResourceId", EntityOperator.EQUALS, dataResource.get("dataResourceId"))
                );
        List<GenericValue> contents = delegator.findByAnd("Content", conditions);
        if (contents.size() > 0) {
            // exit same file already.
            GenericValue content = contents.get(0);
            content.set("description", context.get("description"));
            content.set("classificationEnumId", context.get("classificationEnumId"));
            content.store();
            Map results = ServiceUtil.returnSuccess();
            results.put("contentId", content.getString("contentId"));
            Debug.logInfo("Find same file at server path, updateContentFromDataResource return contentId [" + content.getString("contentId") + "]", module);
            results.put("isOverwrite", "Y");
            return results;
        }
        else {
            //using createContentFromDataResource to create new Content
            return createContentFromDataResource(delegator, dataResource, context);
        }
    }
    
    
    /**
     * update content method.
     * @param delegator a <code>GenericDelegator</code> value
     * @param dataResource a <code>GenericValue</code> value
     * @param context a <code>Map</code> value
     * @return the <code>Map</code> value.
     */
    public static Map updateContent(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String contentId = (String) context.get("contentId");
        String classificationEnumId = (String) context.get("classificationEnumId");
        String contentName = (String) context.get("contentName");
        String description = (String) context.get("description");

        List conditions = UtilMisc.toList(
                new EntityExpr("contentId", EntityOperator.EQUALS, contentId)
                );
        try {
            List<GenericValue> contents = delegator.findByAnd("Content", conditions);
            if (contents.size() > 0) {
                // exit same file already.
                GenericValue content = contents.get(0);
                content.set("description", description);
                content.set("contentName", contentName);
                content.set("classificationEnumId", classificationEnumId);
                content.store();
                Map results = ServiceUtil.returnSuccess();
                results.put("contentId", content.getString("contentId"));
                return results;
            }
            else {
                return ServiceUtil.returnError("Cannot find any Content with given contentId ["+ contentId + "]");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }
}
