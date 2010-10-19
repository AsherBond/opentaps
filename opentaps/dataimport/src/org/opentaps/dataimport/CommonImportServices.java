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

package org.opentaps.dataimport;

import java.io.File;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.service.ServiceException;

/**
 * Common services and helper methods related to files uploading and management.
 */
public final class CommonImportServices {

    private CommonImportServices() { }

    private static final String MODULE = CommonImportServices.class.getName();

    /**
     * Gets the path for uploaded files.
     * @return a <code>String</code> value
     */
    public static String getUploadPath() {
        return System.getProperty("user.dir") + File.separatorChar + "runtime" + File.separatorChar + "data" + File.separatorChar;
    }

    /**
     * Uploads the standard dataimport file by calling the service corresponding to the file format.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> uploadFileForDataImport(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String fileFormat = (String) context.get("fileFormat");
        String fileName = (String) context.get("_uploadedFile_fileName");
        String mimeTypeId = (String) context.get("_uploadedFile_contentType");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        if (mimeTypeId != null && mimeTypeId.length() > 60) {
            // XXX This is a fix to avoid problems where an OS gives us a mime type that is too long to fit in 60 chars
            // (ex. MS .xlsx as application/vnd.openxmlformats-officedocument.spreadsheetml.sheet)
            Debug.logWarning("Truncating mime type [" + mimeTypeId + "] to 60 characters.", MODULE);
            mimeTypeId = mimeTypeId.substring(0, 60);
        }

        String fullFileName = getUploadPath() + fileName;

        // save the file to the system using the ofbiz service
        Map<String, Object> input = UtilMisc.toMap("dataResourceId", null, "binData", context.get("uploadedFile"), "dataResourceTypeId", "LOCAL_FILE", "objectInfo", fullFileName);
        try {
            Map<String, Object> results = dispatcher.runSync("createAnonFile", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        // for now we only support EXCEL format
        if ("EXCEL".equalsIgnoreCase(fileFormat)) {
            ExcelImportServices excelImportService;
            try {
                excelImportService = new ExcelImportServices(new Infrastructure(dispatcher), new User(userLogin), (Locale) context.get("locale"));
                excelImportService.setUploadedFileName(fileName);
                excelImportService.parseFileForDataImport();
            } catch (ServiceException e) {
                return UtilMessage.createAndLogServiceError(e, MODULE);
            } catch (IllegalArgumentException e) {
                return UtilMessage.createAndLogServiceError(e, MODULE);
            }
        } else {
            return UtilMessage.createAndLogServiceError("[" + fileFormat + "] is not a supported file format.", MODULE);
        }

        return ServiceUtil.returnSuccess();
    }
}
