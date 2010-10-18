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
package org.opentaps.gwt.common.server.form;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.service.GenericServiceException;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

public class WarehouseService extends GenericService {

    public WarehouseService(InputProviderInterface provider) {
        super(provider);
    }

    @SuppressWarnings("unused")
    private static final String MODULE = WarehouseService.class.getName();

    /**
     * Calls <code>reReserveProductInventory</code> service.
     */
    public static String reReserveProduct(HttpServletRequest request, HttpServletResponse response) {
        JsonResponse json = new JsonResponse(response);
        try {
            InputProviderInterface provider = new HttpInputProvider(request);
            WarehouseService service = new WarehouseService(provider);
            return json.makeResponse(service.callService());
        } catch (Throwable e) {
            return json.makeResponse(e);
        }
    }

    @Override
    protected Map<String, Object> callService() throws GenericServiceException {
        return callService("reReserveProductInventory");
    }

}
