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
package org.opentaps.common.util;

import java.util.LinkedHashSet;

/**
 * <p>This interface we use together with class <code>ConvertMapToString</code> (or in similar cases)
 * if user code need to know which fields are used to create resulting string.</p>
 *
 */
public interface ICompositeValue {

    /**
     * <p>Returns collection of names. Their meaning depend on context 
     * where <code>ICompositeValue</code> is used</p>
     */
    public LinkedHashSet<String> getFields();

}
