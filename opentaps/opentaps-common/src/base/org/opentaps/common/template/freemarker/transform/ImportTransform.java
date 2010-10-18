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

/* This file may contain code which has been modified from that included with the Apache-licensed OFBiz application */
/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.common.template.freemarker.transform;

import freemarker.template.*;

import java.util.Map;
import java.io.*;

import org.ofbiz.base.util.UtilValidate;
import org.opentaps.common.template.freemarker.FreemarkerUtil;

/**
 * Transform class to provide an <@import .../> wrapper for FTL templates in order to replace 
 *  the <#import .../> Freemarker directive
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev$
 */
public class ImportTransform extends IncludeTransform implements TemplateTransformModel {

    public static final String module = ImportTransform.class.getName();

    public Writer getWriter(final Writer out, Map args) {                      

        final String location = getLocation(args);

        return new Writer(out) {

            public void write(char cbuf[], int off, int len) {};

            public void flush() throws IOException {
                out.flush();
            };

            public void close() throws IOException {
                
                if (UtilValidate.isEmpty(location)) {
                    // todo: Import all FTL templates in a configurable directory
                } else {
                    FreemarkerUtil.addFreeMarkerMacrosFromLocation(location);
                }
            }
        };
    }
}
