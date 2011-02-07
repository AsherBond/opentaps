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
package org.opentaps.controllerinjectex;

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

/**
 * Example services.
 */
public final class ExampleServices {

    private ExampleServices() { }

    private static String MODULE = ExampleServices.class.getName();

    /**
     * Simply echoes the user input.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return the service result <code>Map</code>
     */
    public static Map<String, Object> echo(DispatchContext dctx, Map<String, Object> context) {
        String input = (String) context.get("exampleInput");
        Debug.logInfo("Echoing the input [" + input + "]", MODULE);
        Map<String, Object> res = ServiceUtil.returnSuccess();
        res.put("exampleOutput", input);
        return res;
    }

}
