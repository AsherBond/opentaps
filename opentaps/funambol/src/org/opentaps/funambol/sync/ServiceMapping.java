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

package org.opentaps.funambol.sync;

import java.io.Serializable;
import java.util.Map;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;

/**
 * Meta-Info about a particular way to call an OFBiz service, with certain params.
 * Optionally may also record entity handled by the service
 * 
 * Immutable once created.
 * 
 * @author Cameron Smith - Database, Lda - www.database.co.mz
 *
 */
public class ServiceMapping implements Serializable
{
    private String _serviceName;
    private String _param;  //so far we only support one param
    private String _entity;  //entity name, ex. TelecomNumber
    
    public ServiceMapping(String service)
    {
    	this(service,null);
    }
    
    public ServiceMapping(String service, String param)
    {
        this(service, param, null);
    }
    
    public ServiceMapping(String service, String param, String entity)
    {
        _serviceName = service;
        _param = param;
        _entity = entity;
    }
    
    public String getServiceName() { return _serviceName; }
   
    /**
     * @return null if no entity is defined for this mapping
     */
    public String getEntity() { return _entity; }
    
    /**
     * Return a Map which can be used as params to the service call, using the given param value
     */
    public Map paramsMap(String value)
    {
    	if(UtilValidate.isEmpty(_param)){return null;}
    	
    	return UtilMisc.toMap(_param, value); 
    }
}
