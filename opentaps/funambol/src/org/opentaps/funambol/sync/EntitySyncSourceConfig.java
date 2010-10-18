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
