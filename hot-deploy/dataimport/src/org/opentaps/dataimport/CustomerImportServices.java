/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

package org.opentaps.dataimport;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilMessage;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

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

    public static Map importCustomers(DispatchContext dctx, Map context) {
        int imported = 0;
        try {
            OpentapsImporter customerImporter = new OpentapsImporter("DataImportCustomer", dctx, new CustomerDecoder(context));
            imported += customerImporter.runImport();
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }
        Map result = ServiceUtil.returnSuccess();
        result.put("customersImported", imported);
        return result;
    }

    public static Map importCustomersCommissions(DispatchContext dctx, Map context) {
        String organizationPartyId = (String) context.get("organizationPartyId");
        int imported = 0;

        try {
            OpentapsImporter commissionRatesImporter = new OpentapsImporter("DataImportCommissionRates", dctx, new CommissionRatesDecoder(organizationPartyId));
            imported += commissionRatesImporter.runImport();
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }
        Map result = ServiceUtil.returnSuccess();
        result.put("importedRecords", imported);
        return result;
    }
}

