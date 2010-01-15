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

public class SupplierImportHelper {

	public static String module = SupplierImportServices.class.getName();
							
	public static Map<String, Object> prepareProduct(String supplierId, String supplierName, String	address1, String addres2,
														String city, String stateProvinceGeoId, String postalCode,
														String countryGeoId, String primaryPhoneCountryCode, String primaryPhoneAreaCode,
														String primaryPhoneNumber,	String netPaymentDays, String isIncorporated,
														String federalTaxId, String requires1099) {
		Map<String, Object> fields = FastMap.newInstance();
		fields.put("supplierId", supplierId);
		fields.put("supplierName", supplierName);
		fields.put("address1", address1);
		fields.put("address2", addres2);
		fields.put("city", city);
		fields.put("stateProvinceGeoId", stateProvinceGeoId);
		fields.put("postalCode", postalCode);
		fields.put("countryGeoId", countryGeoId);
		fields.put("primaryPhoneCountryCode", primaryPhoneCountryCode);
		fields.put("primaryPhoneAreaCode", primaryPhoneAreaCode);
		fields.put("primaryPhoneNumber", primaryPhoneNumber);
		fields.put("netPaymentDays", netPaymentDays);
		fields.put("isIncorporated", isIncorporated);
		fields.put("federalTaxId", federalTaxId);
		fields.put("requires1099", requires1099);
		return fields;
}
	// check if product already exists in database
    public static boolean checkSupplierExistsExcel(String supplierId,
            GenericDelegator delegator) {
        GenericValue tmpSupplierGV;
        boolean supplierExists = false;
        try {
            tmpSupplierGV = delegator.findByPrimaryKey("DataImportSupplier", UtilMisc
                .toMap("supplierId", supplierId));
            if (tmpSupplierGV != null
                    && tmpSupplierGV.getString("SupplierId") == supplierId)
                supplierExists = true;
        } catch (GenericEntityException e) {
            Debug.logError("Problem in reading data of supplier", module);
        }
        return supplierExists;
    }

}
