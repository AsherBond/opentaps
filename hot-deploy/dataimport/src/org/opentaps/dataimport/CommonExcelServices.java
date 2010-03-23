/*
 * Copyright (c) 2009 - 2010 Open Source Strategies, Inc.
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.foundation.entity.EntityInterface;

/**
 * Common services and helper methods related to Excel files uploading and management.
 */
public final class CommonExcelServices {

    private CommonExcelServices() { }

    private static final String MODULE = CommonExcelServices.class.getName();

    /**
     * Gets the data import directory path for Excel files.
     * @return a <code>String</code> value
     */
    public static String getExcelUploadPath() {
        return System.getProperty("user.dir") + File.separatorChar + "hot-deploy" + File.separatorChar + "dataimport" + File.separatorChar + "data" + File.separatorChar + "xls" + File.separatorChar;
    }

    /**
     * Gets the specified Excel File in the given directory.
     * @param path the path <code>String</code> of the directory to look files into
     * @param fileName the name of the file to find in the path
     * @return the File found
     */
    public static File getUploadedExcelFile(String path, String fileName) {
        String name = path;
        if (File.separatorChar == name.charAt(name.length() - 1)) {
            name += File.separatorChar;
        }
        name += fileName;

        if (UtilValidate.isNotEmpty(name)) {
            File file = new File(name);
            if (file.canRead()) {
                return file;
            } else {
                Debug.logWarning("File not found or can't be read " + name, MODULE);
                return null;
            }
        } else {
            Debug.logWarning("No path specified, doing nothing", MODULE);
            return null;
        }
    }

    /**
     * Gets the specified Excel File in the default directory.
     * @param fileName the name of the file to find in the path
     * @return the File found
     */
    public static File getUploadedExcelFile(String fileName) {
        return getUploadedExcelFile(getExcelUploadPath(), fileName);
    }

    /**
     * Gets the List of Excel Files in the default directory.
     * @param path the path <code>String</code> of the directory to look files into
     * @return the List of File found
     */
    public static List<File> getUploadedExcelFiles(String path) {
        List<File> fileItems = FastList.newInstance();
        if (UtilValidate.isNotEmpty(path)) {
            File importDir = new File(path);
            if (importDir.isDirectory() && importDir.canRead()) {
                File[] files = importDir.listFiles();
                // loop for all the containing xls file in the spreadsheet
                // directory
                for (int i = 0; i < files.length; i++) {
                    if (files[i].getName().toUpperCase().endsWith("XLS")) {
                        fileItems.add(files[i]);
                    }
                }
            } else {
                Debug.logWarning("Directory not found or can't be read " + path, MODULE);
                return fileItems;
            }
        } else {
            Debug.logWarning("No path specified, doing nothing", MODULE);
            return fileItems;
        }
        return fileItems;
    }

    /**
     * Gets the List of Excel Files in the standard Excel upload directory.
     * @return the List of File found
     */
    public static List<File> getUploadedExcelFiles() {
        return getUploadedExcelFiles(getExcelUploadPath());
    }

    /**
     * Helper method to check if an Excel row is empty.
     * @param row a <code>HSSFRow</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean isNotEmpty(HSSFRow row) {
        if (row == null) {
            return false;
        }
        String s = row.toString();
        if (s == null) {
            return false;
        }
        return !"".equals(s.trim());
    }

    /**
     * Helper method to store the DataImportXXX values.
     * @param delegator a <code>GenericDelegator</code> value
     * @param entityName a <code>String</code> value
     * @param maps the list of Map containing the values to store
     * @exception GenericEntityException if an error occurs
     */
    public static void makeValues(GenericDelegator delegator, String entityName, List<Map<String, Object>> maps) throws GenericEntityException {
        for (int j = 0; j < maps.size(); j++) {
            GenericValue value = delegator.makeValue(entityName, maps.get(j));
            delegator.create(value);
        }
    }

    /**
     * Helper method to store the DataImportXXX values.
     * @param delegator a <code>GenericDelegator</code> value
     * @param entities the list of <code>EntityInterface</code> to store
     * @exception GenericEntityException if an error occurs
     */
    public static void makeValues(GenericDelegator delegator, List<? extends EntityInterface> entities) throws GenericEntityException {
        for (EntityInterface entity : entities) {
            ModelEntity model = delegator.getModelReader().getModelEntity(entity.getBaseEntityName());
            GenericValue value = GenericValue.create(model, entity.toMap());
            delegator.create(value);
        }
    }

    /**
     * Helper method to read a String cell and auto trim it.
     * @param row a <code>HSSFRow</code> value
     * @param index the column index <code>int</code> value which is then casted to a short
     * @return a <code>String</code> value
     */
    public static String readStringCell(HSSFRow row, int index) {
        HSSFCell cell = row.getCell((short) index);
        if (cell == null) {
            return null;
        }

        String s = cell.toString();
        return s.trim();
    }

    /**
     * Helper method to read a Long cell and auto trim it.
     * @param row a <code>HSSFRow</code> value
     * @param index the column index <code>int</code> value which is then casted to a short
     * @return a <code>Long</code> value
     */
    public static Long readLongCell(HSSFRow row, int index) {
        HSSFCell cell = row.getCell((short) index);
        if (cell == null) {
            return null;
        }

        BigDecimal bd = BigDecimal.valueOf(cell.getNumericCellValue());
        if (bd == null) {
            return null;
        }
        return bd.longValue();
    }

    /**
     * Helper method to read a BigDecimal cell and auto trim it.
     * @param row a <code>HSSFRow</code> value
     * @param index the column index <code>int</code> value which is then casted to a short
     * @return a <code>BigDecimal</code> value
     */
    public static BigDecimal readBigDecimalCell(HSSFRow row, int index) {
        HSSFCell cell = row.getCell((short) index);
        if (cell == null) {
            return null;
        }

        return BigDecimal.valueOf(cell.getNumericCellValue());
    }

    /**
     * Uploads an Excel file in the correct directory.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> uploadExcelFile(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String mimeTypeId = (String) context.get("_uploadedFile_contentType");
        if (mimeTypeId != null && mimeTypeId.length() > 60) {
            // XXX This is a fix to avoid problems where an OS gives us a mime type that is too long to fit in 60 chars
            // (ex. MS .xlsx as application/vnd.openxmlformats-officedocument.spreadsheetml.sheet)
            Debug.logWarning("Truncating mime type [" + mimeTypeId + "] to 60 characters.", MODULE);
            mimeTypeId = mimeTypeId.substring(0, 60);
        }
        String fileName = (String) context.get("_uploadedFile_fileName");

        String fileAndPath = getExcelUploadPath() + fileName;

        // save the file to the system using the ofbiz service
        Map<String, Object> input = UtilMisc.toMap("dataResourceId", null, "binData", context.get("uploadedFile"), "dataResourceTypeId", "LOCAL_FILE", "objectInfo", fileAndPath);
        try {
            Map<String, Object> results = dispatcher.runSync("createAnonFile", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Uploads an Excel file in the correct directory.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> uploadExcelFileAndRunImportService(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        List<String> msg = new ArrayList<String>();

        try {
            // upload first
            ModelService service = dctx.getModelService("uploadExcelFile");
            Map<String, Object> input = service.makeValid(context, "IN");
            Map<String, Object> servResults = dispatcher.runSync("uploadExcelFile", input);
            if (ServiceUtil.isError(servResults)) {
                return UtilMessage.createAndLogServiceError(servResults, MODULE);
            }

            List<String> services = Arrays.asList("importProductsFromExcel", "importSuppliersFromExcel");

            // run the import services
            for (String importService : services) {
                Debug.logInfo("Running import service: " + importService, MODULE);
                service = dctx.getModelService(importService);
                input = service.makeValid(context, "IN");
                servResults = dispatcher.runSync(importService, input);
                if (ServiceUtil.isError(servResults)) {
                    return UtilMessage.createAndLogServiceError(servResults, MODULE);
                }
                Object servMsg = servResults.get(ModelService.SUCCESS_MESSAGE);
                if (servMsg != null) {
                    msg.add(servMsg.toString());
                }
            }

            // remove the uploaded file now
            String fileName = (String) context.get("_uploadedFile_fileName");
            File file = getUploadedExcelFile(fileName);
            if (!file.delete()) {
                Debug.logWarning("Could not delete the file : " + file.getName(), MODULE);
            }

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        StringBuilder outMsg = new StringBuilder();
        for (Iterator<String> iter = msg.iterator(); iter.hasNext();) {
            String m = iter.next();
            outMsg.append(m);
            if (iter.hasNext()) {
                outMsg.append("\n");
            }
        }

        return ServiceUtil.returnSuccess(outMsg.toString());
    }

}
