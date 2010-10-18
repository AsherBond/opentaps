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

import org.ofbiz.entity.GenericValue;

import com.funambol.framework.server.Sync4jUser;

/**
 * Stores additional ofbiz-specific authentication information
 * 
 * @author Cameron Smith - Database, Lda - www.database.co.mz 
 */
public class OFBizSync4jUser extends Sync4jUser
{
    private GenericValue _userLogin;
    
    public OFBizSync4jUser(GenericValue userLogin, String[] roles)
    {
        super(userLogin.getString("userLoginId"), 
              userLogin.getString("currentPassword"), null, "", "", roles);
        
        _userLogin = userLogin;
    }

    public GenericValue getUserLogin() { return _userLogin; }
}
