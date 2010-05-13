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

/**
 *
 * @version $Id: Constants.java,v 1.14 2007/03/09 10:18:00 luigiafassina Exp $
 */
public interface Constants {
    public static final String PARAM_SESSION_ID       = "sid"                 ;

    public static final String PARAM_SYNCHOLDER_CLASS = "sync-holder-class"   ;
    public static final String PARAM_HOLDER_CACHE_FACTORY
                                                      = "holder-cache-factory";
    public static final String PARAM_HOLDER_CACHE_CLASS
                                                      = "holder-cache-class"  ;
    public static final String PARAM_GROUP            = "group"               ;
    public static final String PARAM_CHANNEL_PROPERTIES
                                                      = "channel-properties"  ;
    public static final String PARAM_SESSION_TIMEOUT  = "session-timeout";
    public static final String PARAM_JNDI_ADDRESS     = "jndi-address"        ;

    public static final String DEFAULT_GROUP          =
        "funambol.reference_cache";
    public static final long   DEFAULT_TTL            = 600000; // 10 mins
    public static final long   DEFAULT_TIMEOUT        = 10000 ; // 10 secs
    public static final String DEFAULT_JNDI_ADDRESS   = "localhost:1009"      ;

    public static final String LOG_NAME               = "transport.http"      ;

    public static final String PARAM_LOG_MESSAGES     = "log-messages"        ;
    public static final String PARAM_DIRLOG_MESSAGES  = "dirlog-messages"     ;

    public static final String PATH_INFO_STATUS       = "status"              ;

    public static final String PARAM_PREFERRED_ENCODING = "preferred-encoding";
    public static final String PARAM_SUPPORTED_ENCODING = "supported-encoding";
    public static final String PARAM_COMPRESSION_LEVEL  = "compression-level" ;

    public static final String SESSION_ATTRIBUTE_SYNC_HOLDER =
        "funambol.sync-holder";
    public static final String SESSION_ATTRIBUTE_LOG_CONTEXT =
        "funambol.log-context";

    //
    // Supported compression types
    //
    public static final String COMPRESSION_TYPE_GZIP    = "gzip"   ;
    public static final String COMPRESSION_TYPE_DEFLATE = "deflate";

    public static final String HEADER_ACCEPT_ENCODING  = "Accept-Encoding" ;
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
    public static final String HEADER_SIZE_THRESHOLD   = "Size-Threshold"  ;
    public static final String HEADER_UNCOMPRESSED_CONTENT_LENGTH =
        "Uncompressed-Content-Length";

}
