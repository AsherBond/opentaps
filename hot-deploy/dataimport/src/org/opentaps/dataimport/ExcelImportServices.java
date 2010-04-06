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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.opentaps.base.entities.DataImportProduct;
import org.opentaps.base.entities.DataImportSupplier;
import org.opentaps.domain.DomainService;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * Common services and helper methods related to Excel files uploading and management.
 */
public final class ExcelImportServices extends DomainService {

    private static final String MODULE = ExcelImportServices.class.getName();
    
    private static final String EXCEL_PRODUCT_TAB = "Products";
    private static final String EXCEL_SUPPLIERS_TAB = "Suppliers";
    private static final List<String> EXCEL_TABS = Arrays.asList(EXCEL_PRODUCT_TAB, EXCEL_SUPPLIERS_TAB);

    private String uploadedFileName;
    
    public ExcelImportServices() {
    	super();
    }
   
    public ExcelImportServices(Infrastructure infrastructure, User user, Locale locale) throws ServiceException {
        super(infrastructure, user, locale);
    } 
    
    /**
     * Gets the specified Excel File in the given directory.
     * @param path the path <code>String</code> of the directory to look files into
     * @param fileName the name of the file to find in the path
     * @return the File found
     */
    public File getUploadedExcelFile(String path, String fileName) {
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
    public File getUploadedExcelFile(String fileName) {
        return getUploadedExcelFile(CommonImportServices.getUploadPath(), fileName);
    }
    
    /**
     * Helper method to check if an Excel row is empty.
     * @param row a <code>HSSFRow</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isNotEmpty(HSSFRow row) {
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
    public void makeValues(GenericDelegator delegator, String entityName, List<Map<String, Object>> maps) throws GenericEntityException {
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
    public void makeValues(GenericDelegator delegator, List<? extends EntityInterface> entities) throws GenericEntityException {
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
    public String readStringCell(HSSFRow row, int index) {
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
    public Long readLongCell(HSSFRow row, int index) {
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
    public BigDecimal readBigDecimalCell(HSSFRow row, int index) {
        HSSFCell cell = row.getCell((short) index);
        if (cell == null) {
            return null;
        }

        return BigDecimal.valueOf(cell.getNumericCellValue());
    }

    /**
     * Take each row of an Excel sheet and put it into DataImportProduct
     * @param sheet
     * @param delegator
     * @throws GenericEntityException
     */
    protected Collection<? extends EntityInterface> createDataImportProducts(HSSFSheet sheet) throws RepositoryException {
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
         return products;
     }
    
    /**
     * Take each row of an Excel sheet and put it into DataImportSupplier
     * @param sheet
     * @param delegator
     * @throws GenericEntityException
     */
    protected Collection<? extends EntityInterface> createDataImportSuppliers(HSSFSheet sheet) throws RepositoryException {
    	
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
    	
    	return suppliers;
    }
    
    /**
     * Uploads an Excel file in the correct directory.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public void parseFileForDataImport() throws ServiceException {

    	// Get the uploaded file
    	File file = getUploadedExcelFile(getUploadedFileName());

    	// set it up as an Excel workbook
    	POIFSFileSystem fs = null;
    	HSSFWorkbook wb = null;
    	try {
    		// this will auto close the FileInputStream when the constructor completes
    		fs = new POIFSFileSystem(new FileInputStream(file));
    		wb = new HSSFWorkbook(fs);
    	} catch (IOException e) {
    		throw new ServiceException("Unable to read or create workbook from file [" + getUploadedFileName() + "] " + e.getMessage());
    	}

    	// loop through the tabs and import them one by one
    	try {
    		// a collection of all the records from all the excel spreadsheet tabs
        	FastList entitiesToCreate = FastList.newInstance();
        	
    		for (String excelTab: EXCEL_TABS) {
    			HSSFSheet sheet = wb.getSheet(excelTab);
    			if (sheet == null) {
    				Debug.logWarning("Did not find a sheet named " + excelTab + " in " + file.getName() + ".  Will not be importing anything.", MODULE);
    			} else {
    				if (EXCEL_PRODUCT_TAB.equals(excelTab)) {
    					entitiesToCreate.addAll(createDataImportProducts(sheet));
    				} else if (EXCEL_SUPPLIERS_TAB.equals(excelTab)) {
    					entitiesToCreate.addAll(createDataImportSuppliers(sheet));
    				} // etc.
    			}
    		} 
        	// create and store values from all the sheets in the workbook in database using the PartyRepositoryInterface
        	// note we're just using the most basic repository method, so any repository could do here
        	PartyRepositoryInterface partyRepo = this.getDomainsDirectory().getPartyDomain().getPartyRepository();
        	partyRepo.createOrUpdate(entitiesToCreate);

    	} catch (RepositoryException e) {
    		throw new ServiceException(e);
    	}


    	// remove the uploaded file now
    	if (!file.delete()) {
    		Debug.logWarning("Could not delete the file : " + file.getName(), MODULE);
    	}
    }

	public void setUploadedFileName(String uploadedFileName) {
		this.uploadedFileName = uploadedFileName;
	}

	public String getUploadedFileName() {
		return uploadedFileName;
	}

}
