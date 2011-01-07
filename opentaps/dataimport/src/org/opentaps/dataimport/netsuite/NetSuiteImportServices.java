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
package org.opentaps.dataimport.netsuite;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.dataimport.ImportDecoder;
import org.opentaps.dataimport.OpentapsImporter;

import java.util.Map;

/**
 * Import Net Suite objects into opentaps.
 */
public class NetSuiteImportServices {

    public static String module = NetSuiteImportServices.class.getName();

    public static Map importItems(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        int imported = 0;
        String parentCategoryId = (String) context.get("parentCategoryId");
        try {
            // find or create the parent category that we'll put all created categories in
            if (parentCategoryId != null) {
                Map findMap = UtilMisc.toMap("productCategoryId", parentCategoryId);
                GenericValue parentCategory = delegator.findByPrimaryKey("ProductCategory", findMap);
                if (parentCategory == null) {
                    return ServiceUtil.returnError("Parent Category ["+parentCategoryId+"] not found.");
                }
            }

            OpentapsImporter importer = new OpentapsImporter("NetSuiteItem", dctx, new NetSuiteItemDecoder(parentCategoryId));
            importer.configure(context);
            importer.setOrderBy("itemId"); // ordering is important because of dependencies between items
            imported += importer.runImport();
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }
        Map result = ServiceUtil.returnSuccess();
        result.put("importedRecords", imported);
        return result;
    }

    public static Map importCustomers(DispatchContext dctx, Map context) {
        int imported = 0;
        try {
            // create the customer decoder first, since it performs useful validation
            ImportDecoder customerDecoder = new NetSuiteCustomerDecoder(context, dctx.getDelegator());
            
            // import the enumerations first
            OpentapsImporter importer = new OpentapsImporter("NetSuiteCustomerType", dctx, new NetSuiteEnumDecoder("PARTY_INDUSTRY"));
            importer.runImport();
            importer = new OpentapsImporter("NetSuiteSalesOrderType", dctx, new NetSuiteEnumDecoder("ORDER_SALES_CHANNEL"));
            importer.runImport();

            // import the customers
            importer = new OpentapsImporter("NetSuiteCustomer", dctx, customerDecoder);
            importer.configure(context);
            importer.setOrderBy("customerId"); // ordering is important because of parent relationships
            imported += importer.runImport();
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }
        Map result = ServiceUtil.returnSuccess();
        result.put("customersImported", imported);
        return result;
    }

    public static Map importCustomerAddresses(DispatchContext dctx, Map context) {
        int imported = 0;
        try {
            OpentapsImporter importer = new OpentapsImporter("NetSuiteAddressBook", dctx, new NetSuiteAddressDecoder((GenericValue) context.get("userLogin")));
            importer.configure(context);
            imported = importer.runImport();
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }
        Map result = ServiceUtil.returnSuccess();
        result.put("importedRecords", imported);
        return result;
    }
}

