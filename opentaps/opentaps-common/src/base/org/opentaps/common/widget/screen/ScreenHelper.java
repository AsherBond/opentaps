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

/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/

/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.common.widget.screen;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.authz.Authorization;
import org.ofbiz.security.authz.AuthorizationFactory;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.widget.html.HtmlScreenRenderer;
import org.ofbiz.widget.screen.ModelScreen;
import org.ofbiz.widget.screen.ScreenFactory;
import org.ofbiz.widget.screen.ScreenRenderer;
import org.ofbiz.widget.screen.ScreenStringRenderer;
import org.xml.sax.SAXException;

/**
 * Utility methods for working with screen widgets.
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev$
 */
public class ScreenHelper {

    /**
     * Renders a screen widget as text.
     * @param screenLocation location in component:// notation
     * @param dctx a <code>DispatchContext</code> value
     * @return Rendered contents of the screen as text
     * @exception GeneralException if an error occurs
     * @exception IOException if an error occurs
     * @exception SAXException if an error occurs
     * @exception ParserConfigurationException if an error occurs
     */
    public static String renderScreenLocationAsText(String screenLocation, DispatchContext dctx, Map<String, Object> screenContext, Map<String, Object> screenParameters) throws GeneralException, IOException, SAXException, ParserConfigurationException {
        return renderScreenLocationAsText(screenLocation, dctx.getDelegator(), dctx.getDispatcher(), dctx.getSecurity(), screenContext, screenParameters);
    }

    /**
     * Renders a screen widget as text.
     * @param screenLocation location in component:// notation
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param security a <code>Security</code> value
     * @return Rendered contents of the screen as text
     * @exception GeneralException if an error occurs
     * @exception IOException if an error occurs
     * @exception SAXException if an error occurs
     * @exception ParserConfigurationException if an error occurs
     */
    public static String renderScreenLocationAsText(String screenLocation, Delegator delegator, LocalDispatcher dispatcher, Security security, Map<String, Object> screenContext, Map<String, Object> screenParameters) throws GeneralException, IOException, SAXException, ParserConfigurationException {


        // Construct a new writer and use it to construct a new ScreenRenderer instead of using
        //  any ScreenRenderer that might exist in the context, so that output from any nested calls to
        //  ${screens.render(...)} is captured
        Writer writer = new StringWriter();
        ScreenRenderer screens = new ScreenRenderer(writer, MapStack.<String>create(screenContext), new HtmlScreenRenderer());
        Authorization authz = (Authorization) screenParameters.get("authz");
        if (authz == null) {
            authz = AuthorizationFactory.getInstance(delegator);
        }

        ScreenRenderer.populateBasicContext(MapStack.<String>create(screenContext), screens, screenParameters, delegator, dispatcher, (Authorization) screenParameters.get("authz"), security, (Locale) screenContext.get("locale"), (GenericValue) screenContext.get("userLogin"));

        // Get the screen and render it
        ScreenStringRenderer renderer = screens.getScreenStringRenderer();
        ModelScreen modelScreen = ScreenFactory.getScreenFromLocation(screenLocation);
        modelScreen.renderScreenString(writer, screenContext, renderer);

        // Return the rendered text
        return writer.toString();
    }
}
