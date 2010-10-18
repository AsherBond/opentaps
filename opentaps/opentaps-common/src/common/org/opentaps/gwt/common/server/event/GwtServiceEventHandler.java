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

package org.opentaps.gwt.common.server.event;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.webapp.control.ConfigXMLReader.Event;
import org.ofbiz.webapp.control.ConfigXMLReader.RequestMap;
import org.ofbiz.webapp.event.EventHandler;
import org.ofbiz.webapp.event.EventHandlerException;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;
import org.opentaps.gwt.common.server.form.JsonResponse;

/**
 * GwtServiceEventHandler - OFBiz controller-style JSON Event Handler which calls the GwtServiceEventHandler to run a service
 * and returns either the results or the error message to our GWT client-side widgets.
 */
public class GwtServiceEventHandler implements EventHandler {

    /** {@inheritDoc} */
    public void init(ServletContext context) throws EventHandlerException {
    }

    /** @{inheritDoc} */
    public String invoke(Event event, RequestMap requestMap, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {

        String serviceName = null;

        // make sure we have a defined service to call
        serviceName = event.invoke;
        if (serviceName == null) {
            throw new EventHandlerException("Service name (eventMethod) cannot be null");
        }

        JsonResponse json = new JsonResponse(response);
        try {
            InputProviderInterface provider = new HttpInputProvider(request);
            GwtServiceEventHandlerWrapper service = new GwtServiceEventHandlerWrapper(provider, serviceName);
            return json.makeResponse(service.call());
        } catch (Throwable e) {
            return json.makeResponse(e);
        }
    }
}
