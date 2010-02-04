/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (c) 2006 - 2010 Open Source Strategies, Inc.
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
package org.opentaps.http.bridge;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import javax.servlet.ServletContext;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class ProvisionActivator implements BundleActivator {

    List<String> BASIC_BUNDLES = Arrays.asList(
            "org.apache.felix.log",
            "org.apache.felix.http"
    );

    private final ServletContext servletContext;

    public ProvisionActivator(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void start(BundleContext context) throws Exception {
        servletContext.setAttribute(BundleContext.class.getName(), context);

        ArrayList<Bundle> installed = new ArrayList<Bundle>();
        for (URL url : findBundles()) {
            this.servletContext.log("Installing bundle [" + url + "]");
            Bundle bundle = context.installBundle(url.toExternalForm());
            installed.add(bundle);
        }

        for (Bundle bundle : installed) {
            bundle.start();
        }
    }

    public void stop(BundleContext context) throws Exception {}

    @SuppressWarnings("unchecked")
    private List<URL> findBundles() throws Exception {
        ArrayList<URL> list = new ArrayList<URL>();
        Set<Object> resourcePaths = this.servletContext.getResourcePaths("/WEB-INF/bundles/");

        // find very basic bundles that must be loaded first
        for (String basicBundle : BASIC_BUNDLES) {
            Iterator<Object> iter = resourcePaths.iterator();
            while (iter.hasNext()) {
                String name = (String) iter.next();
                if (name.endsWith(".jar") && name.indexOf(basicBundle) != -1) {
                    URL url = this.servletContext.getResource(name);
                    if (url != null) {
                        list.add(url);
                        iter.remove();
                    }
                }
            }
        }

        // add all other bundles
        for (Object path : resourcePaths) {
            String name = (String) path;
            if (name.endsWith(".jar")) {
                URL url = this.servletContext.getResource(name);
                if (url != null) {
                    list.add(url);
                }
            }
        }

        return list;
    }
}
