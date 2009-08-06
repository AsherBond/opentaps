/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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
