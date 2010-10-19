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

/**
 * This class holds common state for preparation and cleanup of JNDI tweaks
 * 
 *  @author Cameron Smith, www.database.co.mz
 */
public abstract class DataSourceFakeJndiBase
{
    protected String _oldInitialContextFactory;

    /**
     * Set the given system property - or remove it if <tt>value</tt> is null
     */
    protected void setSysPropIfNotNull(String prop, String value)
    {
        if(value == null) { System.clearProperty(prop); } 
        else { System.setProperty(prop, value); }
    }
}
