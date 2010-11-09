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

import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilMessage;

/**
 * Import customers via intermediate DataImportCustomer entity.
 * Note that the actual logic for transforming each row into a set of opentaps
 * entities is implemented in the CustomerDecoder class.
 *
 * @author     <a href="mailto:sichen@opensourcestrategies.com">Si Chen</a> 
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a> 
 */
public class CustomerImportServices {

    public static String module = CustomerImportServices.class.getName();

    public static Map<String, Object> importCustomers(DispatchContext dctx, Map<String, ?> context) {
        int imported = 0;
        try {
            OpentapsImporter customerImporter = new OpentapsImporter("DataImportCustomer", dctx, new CustomerDecoder(context));
            imported += customerImporter.runImport();
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("customersImported", imported);
        return result;
    }

    public static Map<String, Object> importCustomersCommissions(DispatchContext dctx, Map<String, ?> context) {
        String organizationPartyId = (String) context.get("organizationPartyId");
        int imported = 0;

        try {
            OpentapsImporter commissionRatesImporter = new OpentapsImporter("DataImportCommissionRates", dctx, new CommissionRatesDecoder(organizationPartyId));
            imported += commissionRatesImporter.runImport();
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("importedRecords", imported);
        return result;
    }
}

