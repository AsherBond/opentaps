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
