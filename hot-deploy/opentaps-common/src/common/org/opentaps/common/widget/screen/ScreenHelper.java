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

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.widget.screen.ScreenRenderer;
import org.ofbiz.widget.screen.ScreenStringRenderer;
import org.ofbiz.widget.screen.ModelScreen;
import org.ofbiz.widget.screen.ScreenFactory;
import org.ofbiz.widget.html.HtmlScreenRenderer;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.entity.GenericValue;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.util.Map;
import java.util.Locale;
import java.io.IOException;
import java.io.Writer;
import java.io.StringWriter;

/**
 * Utility methods for working with screen widgets
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev$
 */
public class ScreenHelper {

    /**
     * Renders a screen widget as text
     * @param screenLocation location in component:// notation
     * @param dctx 
     * @param screenContext 
     * @return Rendered contents of the screen as text
     * @throws GeneralException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static String renderScreenLocationAsText(String screenLocation, DispatchContext dctx, Map screenContext, Map screenParameters) throws GeneralException, IOException, SAXException, ParserConfigurationException {

         
        // Construct a new writer and use it to construct a new ScreenRenderer instead of using
        //  any ScreenRenderer that might exist in the context, so that output from any nested calls to
        //  ${screens.render(...)} is captured
        Writer writer = new StringWriter();
        ScreenRenderer screens = new ScreenRenderer(writer, MapStack.create(screenContext), new HtmlScreenRenderer());
        ScreenRenderer.populateBasicContext(MapStack.create(screenContext), screens, screenParameters, dctx.getDelegator(), dctx.getDispatcher(), dctx.getAuthorization(), dctx.getSecurity(), (Locale) screenContext.get("locale"), (GenericValue) screenContext.get("userLogin"));

        // Get the screen and render it
        ScreenStringRenderer renderer = screens.getScreenStringRenderer();
        ModelScreen modelScreen = ScreenFactory.getScreenFromLocation(screenLocation);
        modelScreen.renderScreenString(writer, screenContext, renderer);

        // Return the rendered text
        return writer.toString();
    }
}
