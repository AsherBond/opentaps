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

package org.opentaps.funambol.sync;

import java.io.Serializable;
import java.util.List;

/**
 * Holds configuration information for an EntitySyncSource
 *
 * TODO: put one-off heavy stuff here for efficiency?
 *
 * @author Cameron Smith - Database, Lda - www.database.co.mz
 */
public class EntitySyncSourceConfig implements Serializable
{
    private String _handlerSuffix = "";  //suffix which should be appended to name of a SyncSource, to find handler for its record type
    
    public String getHandlerSuffix() { return _handlerSuffix; }
    public void setHandlerSuffix(String suffix) { _handlerSuffix = suffix; }
    
    /**
     * This method does nothing, but gives us a hook for forcing startup config validation of handlers
     * @see opentaps-sync-config
     */
    public void setHandlers(List<EntitySyncHandler> handlers) {}
}
