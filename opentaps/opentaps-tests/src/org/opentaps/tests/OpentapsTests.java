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
package org.opentaps.tests;

import org.ofbiz.base.util.UtilMisc;

import java.util.Map;
import java.math.BigDecimal;

/**
 * Global test cases for opentaps, and meta tests.  Test things like
 * the unit test methods themselves here.
 */
public class OpentapsTests extends OpentapsTestCase {

    public void testAssertFieldDifference() {
        Map initialMap = UtilMisc.toMap("one", new Double(1.0), "two", new Double(1.1), "three", new BigDecimal("-0.1"));
        Map finalMap = UtilMisc.toMap("one", new Integer(5), "two", null, "three", new BigDecimal("10.00000"));
        Map expectedMap = UtilMisc.toMap("one", new BigDecimal("4"), "two", "-1.1", "three", new Double(10.1));
        assertMapDifferenceCorrect(initialMap, finalMap, expectedMap);
    }

}
