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
package org.opentaps.webapp;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.component.ComponentConfig.WebappInfo;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.base.entities.OpentapsWebApps;
import org.opentaps.domain.webapp.WebAppRepositoryInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

/**
 * the web app repository class.
 */
public class WebAppRepository  extends Repository implements WebAppRepositoryInterface {

    private static final String MODULE = WebAppRepository.class.getName();
    /** {@inheritDoc}
     * @throws RepositoryException */
    public List<? extends OpentapsWebApps> getWebApps(User user) throws RepositoryException {
        List<OpentapsWebApps> opentapsWebapps =  findAllCache(OpentapsWebApps.class, Arrays.asList(OpentapsWebApps.Fields.sequenceNum.asc()));
        //get all webapps defined in all the ofbiz-components
        List<WebappInfo> webapps = ComponentConfig.getAllWebappResourceInfos();
        Map<String, String[]> webappsMap = FastMap.newInstance();
        //create a map entry (name , permissions[]) for every webapp
        for (WebappInfo webapp : webapps) {
            webappsMap.put(webapp.getName() , webapp.getBasePermission());
        }
        List<OpentapsWebApps> apps = FastList.newInstance();
        if (UtilValidate.isNotEmpty(opentapsWebapps)) {
            for (OpentapsWebApps webapp : opentapsWebapps) {
                String[] permissions = webappsMap.get(webapp.getApplicationId());
                if (user != null) {
                    boolean permitted = true;
                    if (permissions != null) {
                        //  if there are permissions for this application, then check if the user can view it
                        for (int i = 0; i < permissions.length; i++) {
                            // if the application has basePermissions and user doesn't has VIEW/ADMIN permissions on them, don't get the app
                            try {
                                if (!"NONE".equals(permissions[i]) && !user.hasPermission(permissions[i], "VIEW") && !user.hasAdminPermissionsForModule(permissions[i])) {
                                    permitted = false;
                                    break;
                                }
                            } catch (InfrastructureException e) {
                                Debug.logError(e, MODULE);
                            }
                        }
                    }
                    if (permitted){
                        apps.add(webapp);
                    }
                } else {
                    // if user is not authenticated
                    if (permissions == null) {
                        // if there are no permissions required for the application, or if it is an external link,
                        apps.add(webapp);
                    } else if (permissions.length > 0){
                        //  or, if the application is defined with permission of "NONE",  such as the ofbiz e-commerce store
                        if("NONE".equals(permissions[0])){
                            //permissions[0] will always exists
                            apps.add(webapp);
                        }
                    }
                }
            } 

        }
        return apps;
    }
}
