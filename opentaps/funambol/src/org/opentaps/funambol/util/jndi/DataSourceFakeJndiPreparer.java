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

package org.opentaps.funambol.util.jndi;

import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import mz.co.dbl.siga.framework.workflow.MapBasedJndiInitialContextFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ofbiz.base.util.UtilMisc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.funambol.server.db.RoutingDataSource;

/**
 * Should be configured in opentaps-sync-config.xml BEFORE anything which depends on its services.
 * TODO: simplify even further and point straight at EE datasources?
 * 
 * @author Cameron Smith, www.database.co.mz
 */
public class DataSourceFakeJndiPreparer extends DataSourceFakeJndiBase implements ApplicationContextAware
{
    private static Log log = LogFactory.getLog(DataSourceFakeJndiPreparer.class.getName());
    
    private DataSource _ds;  //datasource we should put into Fake JNDI
    private RoutingDataSource _dsUser;  //datasource we should put into Fake JNDI
    private String _dsJndiName;   //name with which this datasource should be stored in Fake JNDI
    private String _dsUserJndiName;   //name with which this datasource should be stored in Fake JNDI
    
    private ApplicationContext _spring;
    private String _springJndiName;  //name with which Spring ApplicationContext should be stored in JNDI
   
    //=== initialization via IOC ===
    
    public void setDataSource(DataSource ds) { _ds = ds; }
    public void setUserDataSource(RoutingDataSource ds) { _dsUser = ds; }
    public void setDsJndiName(String name) { _dsJndiName = name; }
    public void setDsUserJndiName(String name) { _dsUserJndiName = name; }
    
    /**
     * Set the name by which the preparer will store the Spring ApplicationContext in the fake JNDI.
     * If this property is not set, or is set to null, Spring will NOT be put into JNDI.
     */
    public void setSpringJndiName(String springName) { _springJndiName = springName; }
    
    public void setApplicationContext(ApplicationContext spring) { _spring = spring; }
    
    /**
     * Create a datasource and put it in JNDI
     */
    public void init()
    {
        log.info("-> initialization starting");
          
        try
        {    
            if(_ds != null)
            {
                log.debug(" Using this DataSource: " + _ds);
                
                //substitute JVM default JNDI initial context factory with ours - that way all config is done in jndiservers.xml
                _oldInitialContextFactory = System.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY, MapBasedJndiInitialContextFactory.class.getName());

                //now do the JNDI "storing"
                MapBasedJndiInitialContextFactory fakeCtx = new MapBasedJndiInitialContextFactory();
                Map jndiObjects = UtilMisc.toMap(_dsJndiName, _ds, _dsUserJndiName, _dsUser);
                fakeCtx.setObjects(jndiObjects);
                log.debug(" ...and stored it in JNDI as: " + _dsJndiName  + "," + _dsUserJndiName);
                
                //finally store Spring if our configuration was set to this
                if(_springJndiName != null)
                {
                    jndiObjects.put(_springJndiName, _spring);
                }
            }
            else
            {
                log.error("No datasource defined");
            }
        } 
        catch(NamingException jndiBindX)
        {
           log.error("Could not store in JNDI", jndiBindX);  //should never actually happen because we control the context
        }

        log.info("<- initialization finished");
    }
    
    /**
     * This method does not need to do anything.
     * 
     * @see DataSourceFakeJndiCleaner for cleanup actions
     */
    public void destroy() { }
}
