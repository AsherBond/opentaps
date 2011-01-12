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
/* Copyright (c) Open Source Strategies, Inc. */

package org.opentaps.tests.webapp;

import org.opentaps.tests.OpentapsTestCase;

/**
 * Webapp related tests.
 */
public class WebappTests extends OpentapsTestCase {

    private static final String MODULE = WebappTests.class.getName();

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /*
      Those test assume some test seed data has been added:
        testapp1
          testtab11
          testtab12
          testtab13
        testapp2
          testtab21
            group1
              shortcut 1
            group2
              shortcut 1
              shortcut 2
          testtab22
            group1
              shortcut 1
              shortcut 2
            group2
              shortcut 1
              shortcut 2
              shortcut 3
        testapp3 (see permission test)
          testtab31
            group1
              shortcut 1
            group2
              shortcut 1
          testtab32
            group1
              shortcut 1
            group2
              shortcut 1
          testtab33
            group1
              shortcut 1
            group2
              shortcut 1
        testapp4 (see handler test)
          testtab41
            group1
              shortcut 1
            group2
              shortcut 1 org.opentaps.common.handlers.CommonHandlers.checkHasCart
              shortcut 2 org.opentaps.common.handlers.CommonHandlers.checkHasNoCart
          testtab42
            group1 org.opentaps.common.handlers.CommonHandlers.checkBoolean hasGroup1
              shortcut 1 org.opentaps.common.handlers.CommonHandlers.checkBoolean hasShortcut1
              shortcut 2 org.opentaps.common.handlers.CommonHandlers.checkBoolean hasShortcut2
            group2
              shortcut 1
              shortcut 2 org.opentaps.common.handlers.CommonHandlers.checkViewPreferenceForTab TEST_HANDLER:VALUE_1:VALUE_1
              shortcut 3 org.opentaps.common.handlers.CommonHandlers.checkViewPreferenceForTab TEST_HANDLER:VALUE_2:VALUE_1
        testapp5 (See expansion test)
          testtab51 label = "testtab51 ${foo51}"; linkUrl = "foo${bar51}"
            group1 label = "group1${foo511}";
              shortcut1 label = "shortcut1${s1}"; linkUrl = "foo${s11}"
              shortcut2 label = "shortcut1${s2}"; linkUrl = "foo${s22}"
          testtab52 label = "testtab51 ${foo52}"; linkUrl = "foo${bar52}"
          
     */

    public void testBasicGetTabList() {
        // test that the webapp repository gives the correct tab list in each application :
        //  OpentapsWebApps webapp1 = webappRepository.getWebAppById("testapp1")
        //  tabs = webappRepository.getWebAppTabs(webapp1, admin, map{})
        //  -> check tabs includes testtab11, testtab12, testtab13 ...
        //  tab = webappRepository.getTabById(opentapsApplicationName, sectionName);
        //  groups = webappRepository.getShortcutGroups(tab, user, context);
        //  -> test groups includes the corresponding groups

        // same for testapp2
    }

    public void testPermissionCheckOnGetTabList() {
        // same as above but test for testapp3
        //   the tab 1 requires no permission so it should always be listed
        //     group 1 requires no permission so it should always be listed
        //     group 2 requires TESTAPP3T1G2_VIEW
        //   the tab 2 requires TESTAPP3T2_VIEW
        //     group 1 requires no permission so it should always be listed
        //     group 2 requires TESTAPP3T2G2_VIEW
        //   the tab 3 requires TESTAPP3T3_VIEW
        //     group 1 requires no permission so it should always be listed
        //     group 2 requires TESTAPP3T3G2_VIEW

        // test with various users:
        //  one having TESTAPP3T1G2_VIEW TESTAPP3T3G2_VIEW
        //  one having TESTAPP3T2G2_VIEW
        //  one having no specific permission
    }

    public void testHandlerMethods() {
        // test with testapp4

        // will an empty context map:
        //   testtab41 / group2 / shortcut1 should not appear
        //   testtab42 / group1 should not appear
        //   testtab42 / group2 / shortcut3 should not appear

        // setting the context shoppingCart to a non null value
        //   testtab41 / group2 / shortcut1 should appear
        //   testtab41 / group2 / shortcut2 should not appear
        // rest unchanged

        // setting the context hasGroup1 to "Y"
        //   testtab42 / group1 should appear, but without shortcuts inside
        // rest unchanged

        // setting the context hasGroup1 to "Y" and hasShortcut2 to Boolean.TRUE
        //   testtab42 / group1 should appear
        //   testtab42 / group1 / shortcut1 should not appear
        //   testtab42 / group1 / shortcut2 should appear
        // rest unchanged

        // setting the context opentapsApplicationName "test" sectionName to "test" and setting a view preference (ViewPrefAndLocation entity)
        // for user = admin, application = test, applicationSection = test, viewPrefTypeId = TEST_HANDLER, viewPrefString = VALUE_2
        //   testtab42 / group1 should not appear
        //   testtab42 / group2 / shortcut2 should not appear
        //   testtab42 / group2 / shortcut3 should appear
        // rest unchanged
    }

    public void testTabAndShortcutsExpansions() {
        // get for the app testapp5 set context values for foo51, bar51, etc ...
        //  some set to ""
        //  some set to some arbitrary strings or numbers
        // check that the values in the tab labels, links, group labels, shortcuts labels and links ... have the values
        //  in the return of getLinkUrl() / getUiLabel()
    }
    
}
