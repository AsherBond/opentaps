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

import java.util.Hashtable;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionEvent;

import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;
import com.funambol.framework.logging.LogContext;

/**
 * It's a HttpSessionListener used to close the SyncHolder when a session expires
 * (caused by session timeout) or when a session is invalidated by the Sync4jServlet
 * (when the SyncML session is completed)
 * @version $Id: SessionListener.java,v 1.4 2007/02/04 11:09:05 nichele Exp $
 */
public class SessionListener implements HttpSessionListener, Constants {

    // ------------------------------------------------------------ Private data

    private FunambolLogger log = FunambolLoggerFactory.getLogger(LOG_NAME);

    // ---------------------------------------------------------- Public Methods

    /**
     * Called when a session is created.
     */
    public void sessionCreated(HttpSessionEvent evt) {
    }

    /**
     * Called when a session is destroyed(invalidated).<br/>
     * It closes the SyncHolder (if not null). It uses the LogContext in the session
     * to provide useful information about the destroyed session. Note that the
     * log context is not stored in session at the end of the method because
     * the session is destroyed.
     */
    public void sessionDestroyed(HttpSessionEvent evt) {
        HttpSession session = evt.getSession();

        LogContext.clear();

        String             sessionId       = session.getId();

        Hashtable          previousContext =
            (Hashtable)session.getAttribute(SESSION_ATTRIBUTE_LOG_CONTEXT);

        if (previousContext != null) {
            //
            // Sets in the current log context the previous values
            //
            LogContext.setValues(previousContext);
        } else {
            LogContext.setSessionId(sessionId);
        }
        //
        // The thread id is always set
        //
        LogContext.setThreadId(String.valueOf(Thread.currentThread().getId()));

        SyncHolder holder = (SyncHolder)session.getAttribute(SESSION_ATTRIBUTE_SYNC_HOLDER);
        try {
            if (holder != null) {
                if (log.isTraceEnabled()) {
                    log.trace("Closing holder for " + sessionId);
                }
                holder.close();
            }
        } catch (Exception e) {
            log.error("Error closing the holder", e);
        }

        LogContext.clear();
    }

}
