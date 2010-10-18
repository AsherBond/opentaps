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
package org.opentaps.domain.voip;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericValue;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.base.entities.ExternalUser;
import org.opentaps.foundation.action.ActionContext;
import org.opentaps.foundation.infrastructure.User;

/**
 * The Voip actions used to support voip call feature on screen.
 */
public class VoipScreenActions {
    
    private static final String MODULE = VoipScreenActions.class.getName();
    /**
     * Action for the enable voip feature.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     */
    public static void enableVoip(Map<String, Object> context) throws GeneralException {
        final ActionContext ac = new ActionContext(context);
        HttpServletRequest request = ac.getRequest();
        String enabled = UtilProperties.getPropertyValue("voip.properties", "voip.enabled", "N");
        // check the voip feature if enabled
        if (enabled.equals("Y")) {
         boolean hasVoipUser = false;                                                                                      
         GenericValue userLogin = (GenericValue) ac.get("userLogin");
         // check if existing ExternalUser relate current login
         if (userLogin != null) {
             DomainsLoader domainLoader = new DomainsLoader(request);
             VoipRepositoryInterface voipRepository = domainLoader.loadDomainsDirectory().getVoipDomain().getVoipRepository();
             ExternalUser externalUser = voipRepository.getVoipExtensionForUser(new User(userLogin));
             if (externalUser != null) {                                                                          
               hasVoipUser = true;                                                                                     
             }                                                                                                         
         }
         // enable the voip feature
         if (hasVoipUser) {
             Map globalContext = (Map) ac.get("globalContext");
             UtilCommon.addGwtScript(globalContext, "commongwt/org.opentaps.gwt.common.voip.voip");
             Debug.logInfo("enable commongwt/org.opentaps.gwt.common.voip.voip", MODULE);
         } 
     }
    }
}
