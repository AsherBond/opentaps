/**
 * Copyright (C) 2003-2007 Funambol
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
package com.funambol.transport.http.server;

import java.util.Map;

import javax.servlet.http.*;

import com.funambol.framework.server.SyncResponse;
import com.funambol.framework.core.Sync4jException;
import com.funambol.framework.server.error.ServerException;
import com.funambol.framework.server.error.NotImplementedException;
import com.funambol.framework.protocol.ProtocolException;


/**
 * This is an interface of sync holder. A sync holder hides the implementation
 * of the real service provider. Thus the provider can be developed as a local
 * EJB, a remote EJB or a simple local object.
 *
 *
 * @version $Id: SyncHolder.java,v 1.2 2007/01/11 11:49:26 nichele Exp $
 */
public interface SyncHolder {
    // --------------------------------------------------------------- Constants

    // ---------------------------------------------------------- Public methods

    public void setSessionId(String sessionId) throws Sync4jException;

    public String getSessionId();

    /**
     * Processes an incoming XML message.
     *
     * @param requestURI the uri of the request
     * @param msg the SyncML request as stream of bytes
     * @param parameters SyncML request parameters
     * @param headers SyncML request headers
     *
     * @return the SyncML response as a <i>ISyncResponse</i> object
     *
     * @throws ServerException in case of a server error
     *
     */
    public SyncResponse processXMLMessage(final String requestURI ,
                                          final byte[] msg        ,
                                          final Map    parameters ,
                                          final Map    headers    )
    throws NotImplementedException, ProtocolException, ServerException;

    /**
     * Processes an incoming WBXML message.
     *
     * @param requestURI the uri of the request
     * @param msg the SyncML request as stream of bytes
     * @param parameters SyncML request parameters
     * @param headers SyncML request headers
     *
     * @return the SyncML response as a <i>ISyncResponse</i> object
     *
     * @throws ServerException in case of a server error
     *
     */
    public SyncResponse processWBXMLMessage(final String requestURI ,
                                            final byte[] msg        ,
                                            final Map    parameters ,
                                            final Map    headers    )
    throws NotImplementedException, ProtocolException, ServerException;

    /**
     * Called when the SyncHolder is not required any more. It gives the holder
     * an opportunity to do clean up and releaseing of resources.
     *
     * @throws java.lang.Exception in case of error. The real exception is stored
     * in the cause.
     */
    public void close() throws Exception;

    /**
     * Returns the creation timestamp (in milliseconds since midnight, January
     * 1, 1970 UTC).
     */
    public long getCreationTimestamp();
}
