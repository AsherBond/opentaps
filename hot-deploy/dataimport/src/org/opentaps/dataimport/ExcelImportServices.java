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
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
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
import org.opentaps.base.entities.DataImportProduct;
import org.opentaps.base.entities.DataImportSupplier;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.foundation.entity.EntityInterface;

/**
 * Common services and helper methods related to Excel files uploading and management.
 */
public final class ExcelImportServices {

    private static final String MODULE = ExcelImportServices.class.getName();
    
    private static final String EXCEL_PRODUCT_TAB = "Products";
    private static final String EXCEL_SUPPLIERS_TAB = "Suppliers";
    private static final List<String> EXCEL_TABS = Arrays.asList(EXCEL_PRODUCT_TAB, EXCEL_SUPPLIERS_TAB);

    /**
     * Gets the data import directory path for Excel files.
     * @return a <code>String</code> value
     */
    public static String getExcelUploadPath() {
        return System.getProperty("user.dir") + File.separatorChar + "runtime" + File.separatorChar + "data" + File.separatorChar;
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
     * Take each row of an Excel sheet and put it into DataImportProduct
     * @param sheet
     * @param delegator
     * @throws GenericEntityException
     */
    protected static void createDataImportProducts(HSSFSheet sheet, GenericDelegator delegator) throws GenericEntityException {
    	 int sheetLastRowNumber = sheet.getLastRowNum();
    	 List<DataImportProduct> products = FastList.newInstance();
         
         for (int j = 1; j <= sheetLastRowNumber; j++) {
             HSSFRow row = sheet.getRow(j);
             if (isNotEmpty(row)) {
                 // row index starts at 0 here but is actually 1 in Excel
                 int rowNum = row.getRowNum() + 1;
                 // read productId from first column "sheet column index
                 // starts from 0"
                 String id = readStringCell(row, 0);

                 if (UtilValidate.isEmpty(id) || id.indexOf(" ") > -1 || id.equalsIgnoreCase("productId")) {
                     Debug.logWarning("Row number " + rowNum + " not imported from Products tab: invalid ID value [" + id + "].", MODULE);
                     continue;
                 }

                 DataImportProduct product = new DataImportProduct();
                 product.setProductId(id);
                 product.setProductTypeId(readStringCell(row, 1));
                 product.setDescription(readStringCell(row, 2));
                 product.setPrice(readBigDecimalCell(row, 3));
                 product.setPriceCurrencyUomId(readStringCell(row, 4));
                 product.setSupplierPartyId(readStringCell(row, 5));
                 product.setPurchasePrice(readBigDecimalCell(row, 6));
                 products.add(product);
             }
         }
         // create and store values in "DataImportProduct" in database
         makeValues(delegator, products);
    }
    
    /**
     * Take each row of an Excel sheet and put it into DataImportSupplier
     * @param sheet
     * @param delegator
     * @throws GenericEntityException
     */
    protected static void createDataImportSuppliers(HSSFSheet sheet, GenericDelegator delegator) throws GenericEntityException {
    	
    	List<DataImportSupplier> suppliers = FastList.newInstance();
    	int sheetLastRowNumber = sheet.getLastRowNum();
    	for (int j = 1; j <= sheetLastRowNumber; j++) {
    		HSSFRow row = sheet.getRow(j);
    		if (isNotEmpty(row)) {
    			// row index starts at 0 here but is actually 1 in Excel
    			int rowNum = row.getRowNum() + 1;
    			// read supplierId from first column "sheet column index
    			// starts from 0"
    			String id = readStringCell(row, 0);

    			if (UtilValidate.isEmpty(id) || id.indexOf(" ") > -1 || id.equalsIgnoreCase("supplierId")) {
    				Debug.logWarning("Row number " + rowNum + " not imported from Suppliers tab: invalid ID value [" + id + "].", MODULE);
    				continue;
    			}

    			DataImportSupplier supplier = new DataImportSupplier();
    			supplier.setSupplierId(id);
    			supplier.setSupplierName(readStringCell(row, 1));
    			supplier.setAddress1(readStringCell(row, 2));
    			supplier.setAddress2(readStringCell(row, 3));
    			supplier.setCity(readStringCell(row, 4));
    			supplier.setStateProvinceGeoId(readStringCell(row, 5));
    			supplier.setPostalCode(readStringCell(row, 6));
    			supplier.setCountryGeoId(readStringCell(row, 7));
    			supplier.setPrimaryPhoneCountryCode(readStringCell(row, 8));
    			supplier.setPrimaryPhoneAreaCode(readStringCell(row, 9));
    			supplier.setPrimaryPhoneNumber(readStringCell(row, 10));
    			supplier.setNetPaymentDays(readLongCell(row, 11));
    			supplier.setIsIncorporated(readStringCell(row, 12));
    			supplier.setFederalTaxId(readStringCell(row, 13));
    			supplier.setRequires1099(readStringCell(row, 14));
    			suppliers.add(supplier);
    		}
    	}
    	// create and store values in "DataImportSupplier" in database
    	makeValues(delegator, suppliers);
    }
    
    /**
     * Uploads an Excel file in the correct directory.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> uploadExcelFileAndRunImportService(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        
        try {
            // upload first
            ModelService service = dctx.getModelService("uploadExcelFileForDataImport");
            Map<String, Object> input = service.makeValid(context, "IN");
            Map<String, Object> servResults = dispatcher.runSync("uploadExcelFileForDataImport", input);
            if (ServiceUtil.isError(servResults)) {
                return UtilMessage.createAndLogServiceError(servResults, MODULE);
            }

            // Get the uploaded file
            String fileName = (String) context.get("_uploadedFile_fileName");
            File file = getUploadedExcelFile(fileName);
            
            // set it up as an Excel workbook
            POIFSFileSystem fs = null;
            HSSFWorkbook wb = null;
            try {
                // this will auto close the FileInputStream when the constructor completes
                fs = new POIFSFileSystem(new FileInputStream(file));
                wb = new HSSFWorkbook(fs);
            } catch (IOException e) {
                return UtilMessage.createAndLogServiceError(e, "Unable to read or create workbook from file", MODULE);
            }
            
            // loop through the tabs and import them one by one
            for (String excelTab: EXCEL_TABS) {
            	 HSSFSheet sheet = wb.getSheet(excelTab);
                 if (sheet == null) {
                     Debug.logWarning("Did not find a sheet named " + excelTab + " in " + file.getName() + ".  Will not be importing anything.", MODULE);
                 } else {
                	 try {
                		 if (EXCEL_PRODUCT_TAB.equals(excelTab)) {
                			 createDataImportProducts(sheet, dctx.getDelegator());
                		 } else if (EXCEL_SUPPLIERS_TAB.equals(excelTab)) {
                			 createDataImportSuppliers(sheet, dctx.getDelegator());
                		 } // etc.
                	 } catch (GenericEntityException e) {
                         return UtilMessage.createAndLogServiceError(e, "Cannot store DataImportSupplier", MODULE);
                     } 
                 }
            	
            }
          
            // remove the uploaded file now
            if (!file.delete()) {
                Debug.logWarning("Could not delete the file : " + file.getName(), MODULE);
            }

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

}
