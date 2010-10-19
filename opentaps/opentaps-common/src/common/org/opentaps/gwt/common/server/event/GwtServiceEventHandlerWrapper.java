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

import java.util.Map;

import org.ofbiz.service.GenericServiceException;
import org.opentaps.gwt.common.server.InputProviderInterface;
import org.opentaps.gwt.common.server.form.GenericService;

/**
 * A wrapper which calls a service with parameters from the InputServiceProviderInterface.   This provides a further level of abstraction
 * between the server-side services and the client-side widgets, by interposing an InputServiceProviderInterface instead of directly working with the servlet request.
 */
public class GwtServiceEventHandlerWrapper extends GenericService {

    protected GwtServiceEventHandlerWrapper(InputProviderInterface provider, String serviceName) {
        super(provider);
        this.serviceName = serviceName;
    }

    private String serviceName;

    @Override
    protected Map<String, Object> callService() throws GenericServiceException {
        return callService(serviceName);
    }
}
