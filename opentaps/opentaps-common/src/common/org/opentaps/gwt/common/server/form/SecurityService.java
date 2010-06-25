/*
 * Copyright (c) 2010 - 2011 Open Source Strategies, Inc.
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


import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.opentaps.common.event.AjaxEvents;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

public class SecurityService{
    
    @SuppressWarnings("unused")
    private static final String MODULE = WarehouseService.class.getName();
    
    private static final String PERMISSIONS = "permissions";
    
    public SecurityService() {
    }
    
    /**
     * AJAX event to obtain user permissions.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String userPermissions(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        Set<String> permissions = provider.getDomainsDirectory().getUser().getPermissions();
        JSONObject map = new JSONObject();
        map.put(PERMISSIONS, JSONArray.fromObject(permissions).toString());
        
        return AjaxEvents.doJSONResponse(response, map);
    }

}
