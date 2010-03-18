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

import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * Helper methods for Product importation.
 */
public final class ProductImportHelper {

    private ProductImportHelper() { }

    private static final String MODULE = ProductImportHelper.class.getName();

    /**
     * Checks if the product already exists in the database.
     *
     * @param productId a <code>String</code> value
     * @param delegator a <code>GenericDelegator</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean checkProductExists(String productId, GenericDelegator delegator) {
        boolean productExists = false;
        try {
            GenericValue tmpProductGV = delegator.findByPrimaryKey("DataImportProduct", UtilMisc.toMap("productId", productId));
            if (tmpProductGV != null) {
                productExists = true;
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem in reading data of product", MODULE);
        }
        return productExists;
    }
}
