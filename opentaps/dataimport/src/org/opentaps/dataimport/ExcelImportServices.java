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

import javolution.util.FastList;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.base.entities.DataImportCustomer;
import org.opentaps.base.entities.DataImportGlAccount;
import org.opentaps.base.entities.DataImportInventory;
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
    private static final String EXCEL_CUSTOMERS_TAB = "Customers";
    private static final String EXCEL_INVENTORY_TAB = "Inventory";
    private static final String EXCEL_GL_ACCOUNTS_TAB = "GL Accounts";
    private static final List<String> EXCEL_TABS = Arrays.asList(EXCEL_PRODUCT_TAB, EXCEL_SUPPLIERS_TAB, 
                                                                 EXCEL_CUSTOMERS_TAB, EXCEL_INVENTORY_TAB, 
                                                                 EXCEL_GL_ACCOUNTS_TAB);
    
    private static final String USER_ABSOLUTE_FILE_PATH_PARAM = "USER_FILE_PATH";

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

        //check if cell contains a number
        BigDecimal bd = null;
        try{
            double d = cell.getNumericCellValue();
            bd = BigDecimal.valueOf(d);
        }catch(Exception e){
            //do nothing
        }
        
        String s = null;
        if(bd == null){
            s = cell.toString().trim();
        }else{            
            //if cell contains number parse it as long
            s = Long.toString(bd.longValue());
        }
        
        return s;
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
                 product.setProductName(readStringCell(row, 1));
                 product.setInternalName(readStringCell(row, 1));
                 product.setProductTypeId(readStringCell(row, 2));
                 product.setDescription(readStringCell(row, 3));
                 product.setPrice(readBigDecimalCell(row, 4));
                 product.setPriceCurrencyUomId(readStringCell(row, 5));
                 product.setSupplierPartyId(readStringCell(row, 6));
                 product.setPurchasePrice(readBigDecimalCell(row, 7));
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
     * Take each row of an Excel sheet and put it into DataImportCustomer
     * @param sheet
     * @param delegator
     * @throws GenericEntityException
     */
    protected Collection<? extends EntityInterface> createDataImportCustomers(HSSFSheet sheet) throws RepositoryException {
    	
    	List<DataImportCustomer> customers = FastList.newInstance();
    	int sheetLastRowNumber = sheet.getLastRowNum();
    	for (int j = 1; j <= sheetLastRowNumber; j++) {
    		HSSFRow row = sheet.getRow(j);
    		if (isNotEmpty(row)) {
    			// row index starts at 0 here but is actually 1 in Excel
    			int rowNum = row.getRowNum() + 1;
    			// read customerId from first column "sheet column index
    			// starts from 0"
    			String id = readStringCell(row, 0);

    			if (UtilValidate.isEmpty(id) || id.indexOf(" ") > -1 || id.equalsIgnoreCase("customerId")) {
    				Debug.logWarning("Row number " + rowNum + " not imported from Customers tab: invalid ID value [" + id + "].", MODULE);
    				continue;
    			}
                        
                        DataImportCustomer customer = new DataImportCustomer();
                        customer.setCustomerId(id);
                        customer.setCompanyName(this.readStringCell(row, 1));
                        customer.setFirstName(this.readStringCell(row, 2));
                        customer.setLastName(this.readStringCell(row, 3));
                        customer.setAttnName(this.readStringCell(row, 4));
                        customer.setAddress1(this.readStringCell(row, 5));
                        customer.setAddress2(this.readStringCell(row, 6));
                        customer.setCity(this.readStringCell(row, 7));
                        customer.setStateProvinceGeoId(this.readStringCell(row, 8));
                        customer.setPostalCode(this.readStringCell(row, 9));
                        customer.setPostalCodeExt(this.readStringCell(row, 10));
                        customer.setStateProvinceGeoName(this.readStringCell(row, 11));
                        customer.setCountryGeoId(this.readStringCell(row, 12));
                        customer.setPrimaryPhoneCountryCode(this.readStringCell(row, 13));
                        customer.setPrimaryPhoneAreaCode(this.readStringCell(row, 14));
                        customer.setPrimaryPhoneNumber(this.readStringCell(row, 15));
                        customer.setPrimaryPhoneExtension(this.readStringCell(row, 16));
                        customer.setSecondaryPhoneCountryCode(this.readStringCell(row, 17));
                        customer.setSecondaryPhoneAreaCode(this.readStringCell(row, 18));
                        customer.setSecondaryPhoneNumber(this.readStringCell(row, 19));
                        customer.setSecondaryPhoneExtension(this.readStringCell(row, 20));
                        customer.setFaxCountryCode(this.readStringCell(row, 21));
                        customer.setFaxAreaCode(this.readStringCell(row, 22));
                        customer.setFaxNumber(this.readStringCell(row, 23));
                        customer.setDidCountryCode(this.readStringCell(row, 24));
                        customer.setDidAreaCode(this.readStringCell(row, 25));
                        customer.setDidNumber(this.readStringCell(row, 26));
                        customer.setDidExtension(this.readStringCell(row, 27));
                        customer.setEmailAddress(this.readStringCell(row, 28));
                        customer.setWebAddress(this.readStringCell(row, 29));
                        customer.setDiscount(this.readBigDecimalCell(row, 30));
                        customer.setPartyClassificationTypeId(this.readStringCell(row, 31));
                        customer.setCreditCardNumber(this.readStringCell(row, 32));
                        customer.setCreditCardExpDate(this.readStringCell(row, 33));
                        customer.setOutstandingBalance(this.readBigDecimalCell(row, 34));
                        customer.setCreditLimit(this.readBigDecimalCell(row, 35));
                        customer.setCurrencyUomId(this.readStringCell(row, 36));
                        customer.setDisableShipping(this.readStringCell(row, 37));
                        customer.setNetPaymentDays(this.readLongCell(row, 38));
                        customer.setShipToCompanyName(this.readStringCell(row, 39));
                        customer.setShipToFirstName(this.readStringCell(row, 40));
                        customer.setShipToLastName(this.readStringCell(row, 41));
                        customer.setShipToAttnName(this.readStringCell(row, 42));
                        customer.setShipToAddress1(this.readStringCell(row, 43));
                        customer.setShipToAddress2(this.readStringCell(row, 44));
                        customer.setShipToCity(this.readStringCell(row, 45));
                        customer.setShipToStateProvinceGeoId(this.readStringCell(row, 46));
                        customer.setShipToPostalCode(this.readStringCell(row, 47));
                        customer.setShipToPostalCodeExt(this.readStringCell(row, 48));
                        customer.setShipToStateProvGeoName(this.readStringCell(row, 49));
                        customer.setShipToCountryGeoId(this.readStringCell(row, 50));
                        customer.setNote(this.readStringCell(row, 51));
                        customers.add(customer);    			
    		}
    	}
    	
    	return customers;
    }
    
    /**
     * Take each row of an Excel sheet and put it into DataImportInventory
     * @param sheet
     * @param delegator
     * @throws GenericEntityException
     */
    protected Collection<? extends EntityInterface> createDataImportInventory(HSSFSheet sheet) throws RepositoryException {
    	
    	List<DataImportInventory> inventory = FastList.newInstance();
    	int sheetLastRowNumber = sheet.getLastRowNum();
    	for (int j = 1; j <= sheetLastRowNumber; j++) {
    		HSSFRow row = sheet.getRow(j);
    		if (isNotEmpty(row)) {
    			// row index starts at 0 here but is actually 1 in Excel
    			int rowNum = row.getRowNum() + 1;
    			// read itemId from first column "sheet column index
    			// starts from 0"
    			String id = readStringCell(row, 0);

    			if (UtilValidate.isEmpty(id) || id.indexOf(" ") > -1 || id.equalsIgnoreCase("itemId")) {
    				Debug.logWarning("Row number " + rowNum + " not imported from Inventory tab: invalid ID value [" + id + "].", MODULE);
    				continue;
    			}
                        
                        DataImportInventory inventoryItem = new DataImportInventory();
                        inventoryItem.setItemId(id);
                        inventoryItem.setProductId(this.readStringCell(row, 1));
                        inventoryItem.setFacilityId(this.readStringCell(row, 2));
                        inventoryItem.setAvailableToPromise(this.readBigDecimalCell(row, 3));
                        inventoryItem.setOnHand(this.readBigDecimalCell(row, 4));
                        inventoryItem.setMinimumStock(this.readBigDecimalCell(row, 5));
                        inventoryItem.setReorderQuantity(this.readBigDecimalCell(row, 6));
                        inventoryItem.setDaysToShip(this.readBigDecimalCell(row, 7));
                        inventoryItem.setInventoryValue(this.readBigDecimalCell(row, 8));
                        inventory.add(inventoryItem);   		
    		}
    	}
    	
    	return inventory;
    }
    
    /**
     * Take each row of an Excel sheet and put it into DataImportGlAccount
     * @param sheet
     * @param delegator
     * @throws GenericEntityException
     */
    protected Collection<? extends EntityInterface> createDataImportGlAccounts(HSSFSheet sheet) throws RepositoryException {
    	
    	List<DataImportGlAccount> glAccounts = FastList.newInstance();
    	int sheetLastRowNumber = sheet.getLastRowNum();
    	for (int j = 1; j <= sheetLastRowNumber; j++) {
    		HSSFRow row = sheet.getRow(j);
    		if (isNotEmpty(row)) {
    			// row index starts at 0 here but is actually 1 in Excel
    			int rowNum = row.getRowNum() + 1;
    			// read glAccountrId from first column "sheet column index
    			// starts from 0"
    			String id = readStringCell(row, 0);

    			if (UtilValidate.isEmpty(id) || id.indexOf(" ") > -1 || id.equalsIgnoreCase("glAccountId")) {
    				Debug.logWarning("Row number " + rowNum + " not imported from GL Accounts tab: invalid ID value [" + id + "].", MODULE);
    				continue;
    			}
                        
                        DataImportGlAccount glAccount = new DataImportGlAccount();
                        glAccount.setGlAccountId(id);
                        glAccount.setParentGlAccountId(this.readStringCell(row, 1));
                        glAccount.setClassification(this.readStringCell(row, 2));
                        glAccount.setAccountName(this.readStringCell(row, 3));
                        glAccounts.add(glAccount);
    		}
    	}
    	
    	return glAccounts;
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
                            } else if (EXCEL_CUSTOMERS_TAB.equals(excelTab)) {
                                entitiesToCreate.addAll(createDataImportCustomers(sheet));
                            } else if (EXCEL_INVENTORY_TAB.equals(excelTab)) {
                                entitiesToCreate.addAll(createDataImportInventory(sheet));
                            } else if (EXCEL_GL_ACCOUNTS_TAB.equals(excelTab)) {
                                entitiesToCreate.addAll(createDataImportGlAccounts(sheet));
                            }// etc.                                
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
