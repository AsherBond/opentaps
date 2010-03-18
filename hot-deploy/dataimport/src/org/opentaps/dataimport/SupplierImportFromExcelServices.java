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
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.base.entities.DataImportSupplier;
import org.opentaps.common.util.UtilMessage;

/**
 * Supplier importation from Excel sheets.
 */
public final class SupplierImportFromExcelServices {

    private SupplierImportFromExcelServices() { }

    private static final String MODULE = SupplierImportFromExcelServices.class.getName();

    /**
     * Imports products from Excel sheet in DataImportSupplier.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> importSuppliersFromExcel(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();

        // optionally the file name can be specified explicitly, usually when linked to the upload service
        String fileName = (String) context.get("_uploadedFile_fileName");

        List<File> files;
        if (UtilValidate.isNotEmpty(fileName)) {
            files = FastList.newInstance();
            files.add(CommonExcelServices.getUploadedExcelFile(fileName));
        } else {
            files = CommonExcelServices.getUploadedExcelFiles();
        }

        int totalImportedCount = 0;

        for (File item : files) {
            Debug.logInfo("Reading file " + item.getName(), MODULE);

            // read all xls file and create workbook one by one.
            List<DataImportSupplier> suppliers = FastList.newInstance();
            POIFSFileSystem fs = null;
            HSSFWorkbook wb = null;
            try {
                // this will auto close the FileInputStream when the constructor completes
                fs = new POIFSFileSystem(new FileInputStream(item));
                wb = new HSSFWorkbook(fs);
            } catch (IOException e) {
                return UtilMessage.createAndLogServiceError(e, "Unable to read or create workbook from file", MODULE);
            }

            // get first sheet
            int sheetCount = wb.getNumberOfSheets();
            if (sheetCount > 1) {
                Debug.logWarning("Found " + sheetCount + " sheets in " + item.getName() + " but will only try to import the first sheet.", MODULE);
            }
            HSSFSheet sheet = wb.getSheetAt(0);
            int sheetLastRowNumber = sheet.getLastRowNum();
            for (int j = 1; j <= sheetLastRowNumber; j++) {
                HSSFRow row = sheet.getRow(j);
                if (CommonExcelServices.isNotEmpty(row)) {
                    // row index starts at 0 here but is actually 1 in Excel
                    int rowNum = row.getRowNum() + 1;
                    // read supplierId from first column "sheet column index
                    // starts from 0"
                    String id = CommonExcelServices.readStringCell(row, 0);

                    if (UtilValidate.isEmpty(id) || id.indexOf(" ") > -1) {
                        Debug.logWarning("Row number " + rowNum + " not imported from " + item.getName() + " : invalid ID value [" + id + "].", MODULE);
                        continue;
                    }

                    if (SupplierImportHelper.checkSupplierExists(id, delegator)) {
                        Debug.logWarning("Row number " + rowNum + " not imported from " + item.getName() + " : supplier [" + id + "] already exists in the DataImportProduct table.", MODULE);
                        continue;
                    }

                    DataImportSupplier supplier = new DataImportSupplier();
                    supplier.setSupplierId(id);
                    supplier.setSupplierName(CommonExcelServices.readStringCell(row, 1));
                    supplier.setAddress1(CommonExcelServices.readStringCell(row, 2));
                    supplier.setAddress2(CommonExcelServices.readStringCell(row, 3));
                    supplier.setCity(CommonExcelServices.readStringCell(row, 4));
                    supplier.setStateProvinceGeoId(CommonExcelServices.readStringCell(row, 5));
                    supplier.setPostalCode(CommonExcelServices.readStringCell(row, 6));
                    supplier.setCountryGeoId(CommonExcelServices.readStringCell(row, 7));
                    supplier.setPrimaryPhoneCountryCode(CommonExcelServices.readStringCell(row, 8));
                    supplier.setPrimaryPhoneAreaCode(CommonExcelServices.readStringCell(row, 9));
                    supplier.setPrimaryPhoneNumber(CommonExcelServices.readStringCell(row, 10));
                    supplier.setNetPaymentDays(CommonExcelServices.readLongCell(row, 11));
                    supplier.setIsIncorporated(CommonExcelServices.readStringCell(row, 12));
                    supplier.setFederalTaxId(CommonExcelServices.readStringCell(row, 13));
                    supplier.setRequires1099(CommonExcelServices.readStringCell(row, 14));
                    suppliers.add(supplier);
                }
            }
            // create and store values in "DataImportSupplier" in database
            try {
                CommonExcelServices.makeValues(delegator, suppliers);
            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogServiceError(e, "Cannot store DataImportSupplier", MODULE);
            }
            int uploadedCount = suppliers.size();
            if (uploadedCount > 0) {
                Debug.logInfo("Imported " + uploadedCount + " products from file " + item.getName(), MODULE);
                totalImportedCount += uploadedCount;
            }
        }

        Map<String, Object> responseMsgs = ServiceUtil.returnSuccess("Wrote " + totalImportedCount + " DataImportSupplier.");
        responseMsgs.put("importedRecords", totalImportedCount);
        return responseMsgs;
    }

}
