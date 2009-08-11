/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 * 
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
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
