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
package org.opentaps.installer.service;

/**
 *
 *
 */
public interface OSSInstaller {

    public static final String STEP_ID_PROP = "step.id";
    public static final String SEQUENCE_PROP = "sequence";

    public String nextUri(String clazz);

    public String prevUri(String clazz);

    public void registerStepHandler(String clazz);

    public void unregisterStepHandler(String clazz);

    public void run();
}
