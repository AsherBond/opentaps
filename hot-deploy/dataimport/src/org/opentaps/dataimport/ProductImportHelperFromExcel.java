/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
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

import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.product.spreadsheetimport.ImportProductHelper;

public class ProductImportHelperFromExcel {

	static String module = ImportProductHelper.class.getName();

    public static Map<String, Object> prepareProduct(String productId, String productTypeId, String description, String price,
			 											String priceCurrencyUomId, String supplierPartyId, String purchasePrice) {
    	Map<String, Object> fields = FastMap.newInstance();
    	fields.put("productId", productId);
    	fields.put("productTypeId", productTypeId);
    	fields.put("description", description);
    	fields.put("price", price);
    	fields.put("priceCurrencyUomId", priceCurrencyUomId);
    	fields.put("supplierPartyId", supplierPartyId);
    	fields.put("purchasePrice", purchasePrice);
    	return fields;
}

	
    // check if product already exists in database
    public static boolean checkProductExistsExcel(String productId,
            GenericDelegator delegator) {
        GenericValue tmpProductGV;
        boolean productExists = false;
        try {
            tmpProductGV = delegator.findByPrimaryKey("DataImportProduct", UtilMisc
                .toMap("productId", productId));
            if (tmpProductGV != null
                    && tmpProductGV.getString("productId") == productId)
                productExists = true;
        } catch (GenericEntityException e) {
            Debug.logError("Problem in reading data of product", module);
        }
        return productExists;
    }
}
