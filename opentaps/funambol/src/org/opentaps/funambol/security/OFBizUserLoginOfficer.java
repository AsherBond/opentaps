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

package org.opentaps.funambol.security;

import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.LocalDispatcher;

import com.funambol.framework.core.Authentication;
import com.funambol.framework.core.Cred;
import com.funambol.framework.server.Sync4jUser;
import com.funambol.framework.server.store.PersistentStoreException;
import com.funambol.framework.tools.Base64;
import com.funambol.server.security.DBOfficer;
import com.funambol.server.security.UserProvisioningOfficer;

/**
 * Perform authorization based on whether the username/password correspond to an OFBiz UserLogin entity
 * 
 * @author Cameron Smith, www.database.co.mz
 */
public class OFBizUserLoginOfficer extends UserProvisioningOfficer
{
    protected GenericDelegator _delegator = GenericDelegator.getGenericDelegator("default");
    protected LocalDispatcher _dispatcher;
  
    public OFBizUserLoginOfficer()
    {
        _dispatcher = GenericDispatcher.getLocalDispatcher("funambol", _delegator);
        log.info("CREATED OULO with this ps: " + ps);
    }
    
    /**
     * @see DBOfficer.authenticateBasicCredential
     * 
     * @return null if login not found
     * @throws WHAT if could not access backend?
     */
    public Sync4jUser authenticateUser(Cred credentials)
    {
        log.warn("authenticateUser called with " + credentials + " when PS is: " + ps);
        
        Authentication auth = credentials.getAuthentication();
        
        //1. Decipher username and pwd from SyncML data
        String username = null, password = null;
        String userpwd = new String(Base64.decode(auth.getData()));

        int p = userpwd.indexOf(':');
        if (p == -1)  //TODO: check if this is necessary - better just to bail out
        {
            username = userpwd;
            password = "";
        }
        else  //TODO: tidy up and comment this weird mess from FBol
        {
            username = p>0  ? userpwd.substring(0, p) : "";
            password = p == (userpwd.length()-1) ? "" : userpwd.substring(p+1);
        }
        log.warn("authenticateUser detected " + username + ", " + password);
        
        try
        {
            Map userData = _dispatcher.runSync("userLogin", UtilMisc.<String, Object>toMap("login.username", username, "login.password", password, "isServiceAuth", true));
            GenericValue userLogin = (GenericValue)userData.get("userLogin");
            if(userLogin == null)
            {
                log.warn("Failed login attempt by " + username);
                return null;  //indicates failure
            }
            else
            {
                //detect the security groups - for now FULLADMIN means FBol admin, everything is normal user
                boolean isAdmin = false;
                for(GenericValue group : (List<GenericValue>)userLogin.getRelated("UserLoginSecurityGroup"))
                {
                    if("FULLADMIN".equals(group.get("groupId")))
                    {
                        isAdmin = true;
                        break;
                    }       
                }
                String[] roles = { isAdmin ? "sync_administrator" :  "sync_user" };  //TODO: find fbol constants for these?
                
                //simple create a Principal to map username to device, if doesn't already exist
                try
                {
                    log.warn("-> handlePrincipal");
                    handlePrincipal(username, auth.getDeviceId());
                    log.warn("<- handlePrincipal");
                }
                catch(PersistentStoreException principalX)
                {
                    log.error("Error handling the principal", principalX);
                    return null;
                }
                
                //TODO: set all fields here
                log.warn("authenticateUser: success");
                return new OFBizSync4jUser(userLogin, roles);
            }
        }
        catch(GeneralException couldNotLoginX)  //TODO: throw RTX here?
        {
           log.error("Could not call access UserLogin or related info for " + username, couldNotLoginX);
           return null;
        }
    }
}
