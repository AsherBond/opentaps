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

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;

import javax.servlet.ServletContext;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Arrays;

public final class FrameworkService {

    private final ServletContext context;
    private Felix felix;

    public FrameworkService(ServletContext context) {
        this.context = context;
    }

    public void start() {
        try {
            doStart();
        } catch (Exception e) {
            log("Failed to start framework", e);
        }
    }

    public void stop() {
        try {
            doStop();
        } catch (Exception e) {
            log("Error stopping framework", e);
        }
    }

    private void doStart() throws Exception
    {
        Felix tmp = new Felix(createConfig());
        tmp.start();
        felix = tmp;
        log("OSGi framework started", null);
    }

    private void doStop() throws Exception
    {
        if (felix != null) {
            felix.stop();
        }
        log("OSGi framework stopped", null);
    }

    private Map<String, Object> createConfig() throws Exception {
        Properties props = new Properties();
        props.load(context.getResourceAsStream("/WEB-INF/osgi-framework.properties"));

        HashMap<String, Object> map = new HashMap<String, Object>();
        for (Object key : props.keySet()) {
            map.put(key.toString(), props.get(key));
        }

        map.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, Arrays.asList(new ProvisionActivator(context)));
        return map;
    }

    private void log(String message, Throwable cause) {
        context.log(message, cause);
    }
}
