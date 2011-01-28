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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.opentaps.base.entities.OpentapsShortcut;
import org.opentaps.base.entities.OpentapsWebAppTab;
import org.opentaps.base.entities.OpentapsWebApps;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.webapp.OpentapsShortcutGroup;
import org.opentaps.domain.webapp.WebAppDomainInterface;
import org.opentaps.domain.webapp.WebAppRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
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
              shortcut2 label = "shortcut2${s2}"; linkUrl = "foo${s22}"
          testtab52 label = "testtab52 ${foo52}"; linkUrl = "foo${bar52}"

     */

    /**
     * test that the webapp repository gives the correct tab list in each application :
     * OpentapsWebApps webapp1 = webappRepository.getWebAppById("testapp1")
     * tabs = webappRepository.getWebAppTabs(webapp1, map{})
     * -> check tabs includes testtab11, testtab12, testtab13 ...
     * test that tabs include only the expected for this application tabs (and no extra one)
     * tab = webappRepository.getTabById(opentapsApplicationName, sectionName);
     * groups = webappRepository.getShortcutGroups(tab, user, context);
     * -> test groups includes the corresponding groups
     * same for testapp2
     *
     * @throws Exception
     */
    public void testBasicGetTabList() throws Exception {
        // testapp1
        WebAppDomainInterface webAppDomain = domainsDirectory.getWebAppDomain();
        WebAppRepositoryInterface webappRepository = webAppDomain.getWebAppRepository();
        Map<String, Object> context = FastMap.newInstance();

        OpentapsWebApps webapp1 = webappRepository.getWebAppById("testapp1");
        List<? extends OpentapsWebAppTab> tabs = webappRepository.getWebAppTabs(webapp1, context);

        boolean hasTab1 = false;
        boolean hasTab2 = false;
        boolean hasTab3 = false;

        List<String> expectedList = Arrays.asList("testtab11", "testtab12", "testtab13");
        List<String> actualList = new ArrayList<String>();

        for (OpentapsWebAppTab tab : tabs) {
            if (tab.getTabId().equals("testtab11")) {
                hasTab1 = true;
            } else if (tab.getTabId().equals("testtab12")) {
                hasTab2 = true;
            } else if (tab.getTabId().equals("testtab13")) {
                hasTab3 = true;
            }
            actualList.add(tab.getTabId());
        }

        assertTrue("Could not find tab [testtab11] in the [testapp1] ", hasTab1);
        assertTrue("Could not find tab [testtab12] in the [testapp1] ", hasTab2);
        assertTrue("Could not find tab [testtab13] in the [testapp1] ", hasTab3);

        assertEquals("Tabs includes not only the expected tabs of the [testapp1] ",actualList, expectedList, false);

        OpentapsWebAppTab tab = webappRepository.getTabById("testapp1", "testtab11");
        List<OpentapsShortcutGroup> groups = webappRepository.getShortcutGroups(tab, context);

        assertEquals("The [testtab11] in the [testapp1] not have a group", groups.size(), 0);

        tab = webappRepository.getTabById("testapp1", "testtab12");
        groups = webappRepository.getShortcutGroups(tab, context);

        assertEquals("The [testtab12] in the [testapp1] not have a group", groups.size(), 0);

        tab = webappRepository.getTabById("testapp1", "testtab13");
        groups = webappRepository.getShortcutGroups(tab, context);

        assertEquals("The [testtab13] in the [testapp1] not have a group", groups.size(), 0);

        // testapp2
        OpentapsWebApps webapp2 = webappRepository.getWebAppById("testapp2");
        tabs = webappRepository.getWebAppTabs(webapp2, context);

        hasTab1 = false;
        hasTab2 = false;

        expectedList = Arrays.asList("testtab21", "testtab22");
        actualList.clear();

        for (OpentapsWebAppTab tab2 : tabs) {
            if (tab2.getTabId().equals("testtab21")) {
                hasTab1 = true;
            } else if (tab2.getTabId().equals("testtab22")) {
                hasTab2 = true;
            }
            actualList.add(tab2.getTabId());
        }

        assertTrue("Could not find tab [testtab21] in the [testapp2] ", hasTab1);
        assertTrue("Could not find tab [testtab22] in the [testapp2] ", hasTab2);

        assertEquals("Tabs includes not only the expected tabs of the [testapp2] ",actualList, expectedList, false);

        tab = webappRepository.getTabById("testapp2", "testtab21");
        groups = webappRepository.getShortcutGroups(tab, context);

        boolean hasGroup1 = false;
        boolean hasGroup2 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group2-21-1")) {
                hasGroup1 = true;
            } else if (group.getGroupId().equalsIgnoreCase("group2-21-2")) {
                hasGroup2 = true;
            }
        }

        assertTrue("Could not find group [group2-21-1] in the [testapp2] [testtab21]", hasGroup1);
        assertTrue("Could not find group [group2-21-2] in the [testapp2] [testtab21]", hasGroup2);

        tab = webappRepository.getTabById("testapp2", "testtab22");
        groups = webappRepository.getShortcutGroups(tab, context);

        hasGroup1 = false;
        hasGroup2 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group2-22-1")) {
                hasGroup1 = true;
            } else if (group.getGroupId().equalsIgnoreCase("group2-22-2")) {
                hasGroup2 = true;
            }
        }

        assertTrue("Could not find group [group2-22-1] in the [testapp2] [testtab22]", hasGroup1);
        assertTrue("Could not find group [group2-22-2] in the [testapp2] [testtab22]", hasGroup2);
    }

    /**
     * same as above but test for testapp3
     * the tab 1 requires no permission so it should always be listed
     *   group 1 requires no permission so it should always be listed
     *   group 2 requires TESTAPP3T1G2_VIEW
     * the tab 2 requires TESTAPP3T2_VIEW
     *   group 1 requires no permission so it should always be listed
     *   group 2 requires TESTAPP3T2G2_VIEW
     * the tab 3 requires TESTAPP3T3_VIEW
     *   group 1 requires no permission so it should always be listed
     *   group 2 requires TESTAPP3T3G2_VIEW
     *
     * test with various users:
     * one having TESTAPP3T1G2_VIEW TESTAPP3T3G2_VIEW - testuser1
     * one having TESTAPP3T2G2_VIEW                   - testuser2
     * one having no specific permission              - testuser3
     *
     * @throws Exception
     */
    public void testPermissionCheckOnGetTabList() throws Exception {
        // testuser1
        GenericValue testuser1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "testuser1"));

        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(testuser1));
        WebAppDomainInterface webAppDomain = domainLoader.loadDomainsDirectory().getWebAppDomain();
        WebAppRepositoryInterface webappRepository = webAppDomain.getWebAppRepository();
        Map<String, Object> context = FastMap.newInstance();

        // test tab for testuser1
        OpentapsWebApps webapp3 = webappRepository.getWebAppById("testapp3");
        List<? extends OpentapsWebAppTab> tabs = webappRepository.getWebAppTabs(webapp3, context);

        boolean hasTab1 = false;
        boolean hasTab2 = false;
        boolean hasTab3 = false;

        List<String> expectedList = Arrays.asList("testtab31", "testtab33");
        List<String> actualList = new ArrayList<String>();

        for (OpentapsWebAppTab tab : tabs) {
            if (tab.getTabId().equals("testtab31")) {
                hasTab1 = true;
            } else if (tab.getTabId().equals("testtab32")) {
                hasTab2 = true;
            } else if (tab.getTabId().equals("testtab33")) {
                hasTab3 = true;
            }
            actualList.add(tab.getTabId());
        }

        assertTrue("Could not find tab [testtab31] in the [testapp3] for [testuser1]", hasTab1);
        assertFalse("Found tab [testtab32] in the [testapp3] for [testuser1]", hasTab2);
        assertTrue("Could not find tab [testtab33] in the [testapp3] for [testuser1]", hasTab3);

        assertEquals("Tabs includes not only the expected tabs of the [testapp3] for [testuser1]",actualList, expectedList, false);

        // test groups for testuser1 tab1
        OpentapsWebAppTab tab1 = webappRepository.getTabById("testapp3", "testtab31");
        List<OpentapsShortcutGroup> groups = webappRepository.getShortcutGroups(tab1, context);

        boolean hasGroup1 = false;
        boolean hasGroup2 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group3-31-1")) {
                hasGroup1 = true;
            } else if (group.getGroupId().equalsIgnoreCase("group3-31-2")) {
                hasGroup2 = true;
            }
        }

        assertTrue("Could not find group [group3-31-1] in the [testapp3] [testtab31] for [testuser1]", hasGroup1);
        assertTrue("Could not find group [group3-31-2] in the [testapp3] [testtab31] for [testuser1]", hasGroup2);

        // test groups for testuser1 tab2
        OpentapsWebAppTab tab2 = webappRepository.getTabById("testapp3", "testtab32");
        groups = webappRepository.getShortcutGroups(tab2, context);

        hasGroup1 = false;
        hasGroup2 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group3-32-1")) {
                hasGroup1 = true;
            } else if (group.getGroupId().equalsIgnoreCase("group3-32-2")) {
                hasGroup2 = true;
            }
        }

        assertTrue("Could not find group [group3-32-1] in the [testapp3] [testtab32] for [testuser1]", hasGroup1);
        assertFalse("Found group [group3-32-2] in the [testapp3] [testtab32] for [testuser1]", hasGroup2);

        // test groups for testuser1 tab3
        OpentapsWebAppTab tab3 = webappRepository.getTabById("testapp3", "testtab33");
        groups = webappRepository.getShortcutGroups(tab3, context);

        hasGroup1 = false;
        hasGroup2 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group3-33-1")) {
                hasGroup1 = true;
            } else if (group.getGroupId().equalsIgnoreCase("group3-33-2")) {
                hasGroup2 = true;
            }
        }

        assertTrue("Could not find group [group3-33-1] in the [testapp3] [testtab33] for [testuser1]", hasGroup1);
        assertTrue("Could not find group [group3-33-2] in the [testapp3] [testtab33] for [testuser1]", hasGroup2);

        // test tab for testuser2
        GenericValue testuser2 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "testuser2"));

        domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(testuser2));
        webAppDomain = domainLoader.loadDomainsDirectory().getWebAppDomain();
        webappRepository = webAppDomain.getWebAppRepository();

        webapp3 = webappRepository.getWebAppById("testapp3");
        tabs = webappRepository.getWebAppTabs(webapp3, context);

        hasTab1 = false;
        hasTab2 = false;
        hasTab3 = false;

        expectedList = Arrays.asList("testtab31", "testtab32");
        actualList.clear();

        for (OpentapsWebAppTab tab : tabs) {
            if (tab.getTabId().equals("testtab31")) {
                hasTab1 = true;
            } else if (tab.getTabId().equals("testtab32")) {
                hasTab2 = true;
            } else if (tab.getTabId().equals("testtab33")) {
                hasTab3 = true;
            }
            actualList.add(tab.getTabId());
        }

        assertTrue("Could not find tab [testtab31] in the [testapp3] for [testuser2]", hasTab1);
        assertTrue("Could not find tab [testtab32] in the [testapp3] for [testuser2]", hasTab2);
        assertFalse("Found tab [testtab33] in the [testapp3] for [testuser2]", hasTab3);

        assertEquals("Tabs includes not only the expected tabs of the [testapp3] for [testuser2]",actualList, expectedList, false);

        // test groups for testuser2 tab1
        tab1 = webappRepository.getTabById("testapp3", "testtab31");
        groups = webappRepository.getShortcutGroups(tab1, context);

        hasGroup1 = false;
        hasGroup2 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group3-31-1")) {
                hasGroup1 = true;
            } else if (group.getGroupId().equalsIgnoreCase("group3-31-2")) {
                hasGroup2 = true;
            }
        }

        assertTrue("Could not find group [group3-31-1] in the [testapp3] [testtab31] for [testuser2]", hasGroup1);
        assertFalse("Found group [group3-31-2] in the [testapp3] [testtab31] for [testuser2]", hasGroup2);

        // test groups for testuser2 tab2
        tab2 = webappRepository.getTabById("testapp3", "testtab32");
        groups = webappRepository.getShortcutGroups(tab2, context);

        hasGroup1 = false;
        hasGroup2 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group3-32-1")) {
                hasGroup1 = true;
            } else if (group.getGroupId().equalsIgnoreCase("group3-32-2")) {
                hasGroup2 = true;
            }
        }

        assertTrue("Could not find group [group3-32-1] in the [testapp3] [testtab32] for [testuser2]", hasGroup1);
        assertTrue("Could not find group [group3-32-2] in the [testapp3] [testtab32] for [testuser2]", hasGroup2);

        // test groups for testuser2 tab3
        tab3 = webappRepository.getTabById("testapp3", "testtab33");
        groups = webappRepository.getShortcutGroups(tab3, context);

        hasGroup1 = false;
        hasGroup2 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group3-33-1")) {
                hasGroup1 = true;
            } else if (group.getGroupId().equalsIgnoreCase("group3-33-2")) {
                hasGroup2 = true;
            }
        }

        assertTrue("Could not find group [group3-33-1] in the [testapp3] [testtab33] for [testuser2]", hasGroup1);
        assertFalse("Found group [group3-33-2] in the [testapp3] [testtab33] for [testuser2]", hasGroup2);

        // test tab for testuser3
        GenericValue testuser3 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "testuser3"));

        domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(testuser3));
        webAppDomain = domainLoader.loadDomainsDirectory().getWebAppDomain();
        webappRepository = webAppDomain.getWebAppRepository();

        webapp3 = webappRepository.getWebAppById("testapp3");
        tabs = webappRepository.getWebAppTabs(webapp3, context);

        hasTab1 = false;
        hasTab2 = false;
        hasTab3 = false;

        expectedList = Arrays.asList("testtab31");
        actualList.clear();

        for (OpentapsWebAppTab tab : tabs) {
            if (tab.getTabId().equals("testtab31")) {
                hasTab1 = true;
            } else if (tab.getTabId().equals("testtab32")) {
                hasTab2 = true;
            } else if (tab.getTabId().equals("testtab33")) {
                hasTab3 = true;
            }
            actualList.add(tab.getTabId());
        }

        assertTrue("Could not find tab [testtab31] in the [testapp3] for [testuser3]", hasTab1);
        assertFalse("Found tab [testtab32] in the [testapp3] for [testuser3]", hasTab2);
        assertFalse("Found tab [testtab33] in the [testapp3] for [testuser3]", hasTab3);

        assertEquals("Tabs includes not only the expected tabs of the [testapp3] for [testuser3]",actualList, expectedList, false);

        // test groups for testuser3 tab1
        tab1 = webappRepository.getTabById("testapp3", "testtab31");
        groups = webappRepository.getShortcutGroups(tab1, context);

        hasGroup1 = false;
        hasGroup2 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group3-31-1")) {
                hasGroup1 = true;
            } else if (group.getGroupId().equalsIgnoreCase("group3-31-2")) {
                hasGroup2 = true;
            }
        }

        assertTrue("Could not find group [group3-31-1] in the [testapp3] [testtab31] for [testuser3]", hasGroup1);
        assertFalse("Found group [group3-31-2] in the [testapp3] [testtab31] for [testuser3]", hasGroup2);

        // test groups for testuser3 tab2
        tab2 = webappRepository.getTabById("testapp3", "testtab32");
        groups = webappRepository.getShortcutGroups(tab2, context);

        hasGroup1 = false;
        hasGroup2 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group3-32-1")) {
                hasGroup1 = true;
            } else if (group.getGroupId().equalsIgnoreCase("group3-32-2")) {
                hasGroup2 = true;
            }
        }

        assertTrue("Could not find group [group3-32-1] in the [testapp3] [testtab32] for [testuser3]", hasGroup1);
        assertFalse("Found group [group3-32-2] in the [testapp3] [testtab32] for [testuser3]", hasGroup2);

        // test groups for testuser3 tab3
        tab3 = webappRepository.getTabById("testapp3", "testtab33");
        groups = webappRepository.getShortcutGroups(tab3, context);

        hasGroup1 = false;
        hasGroup2 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group3-33-1")) {
                hasGroup1 = true;
            } else if (group.getGroupId().equalsIgnoreCase("group3-33-2")) {
                hasGroup2 = true;
            }
        }

        assertTrue("Could not find group [group3-33-1] in the [testapp3] [testtab33] for [testuser3]", hasGroup1);
        assertFalse("Found group [group3-33-2] in the [testapp3] [testtab33] for [testuser3]", hasGroup2);

    }

    /**
     * test with testapp4
     * will an empty context map:
     * testtab41 / group2 / shortcut1 should not appear
     *    testtab42 / group1 should not appear
     *    testtab42 / group2 / shortcut3 should not appear
     *
     *  setting the context shoppingCart to a non null value
     *     testtab41 / group2 / shortcut1 should appear
     *     testtab41 / group2 / shortcut2 should not appear
     *   rest unchanged
     *
     *   setting the context hasGroup1 to "Y"
     *     testtab42 / group1 should appear, but without shortcuts inside
     *   rest unchanged
     *
     *   setting the context hasGroup1 to "Y" and hasShortcut2 to Boolean.TRUE
     *     testtab42 / group1 should appear
     *     testtab42 / group1 / shortcut1 should not appear
     *     testtab42 / group1 / shortcut2 should appear
     *   rest unchanged
     *
     *  setting the context opentapsApplicationName "test" sectionName to "test" and setting a view preference (ViewPrefAndLocation entity)
     *   for user = admin, application = test, applicationSection = test, viewPrefTypeId = TEST_HANDLER, viewPrefString = VALUE_2
     *     testtab42 / group1 should not appear
     *     testtab42 / group2 / shortcut2 should not appear
     *     testtab42 / group2 / shortcut3 should appear
     *   rest unchanged
     *
     * @throws Exception
     */
    public void testHandlerMethods() throws Exception {
        // test with testapp4
        WebAppDomainInterface webAppDomain = domainsDirectory.getWebAppDomain();
        WebAppRepositoryInterface webappRepository = webAppDomain.getWebAppRepository();
        Map<String, Object> context = FastMap.newInstance();

        // will an empty context map:

        // testtab41 / group2 / shortcut1 should not appear
        OpentapsWebAppTab tab = webappRepository.getTabById("testapp4", "testtab41");
        List<OpentapsShortcutGroup> groups = webappRepository.getShortcutGroups(tab, context);

        boolean hasGroup1 = false;
        boolean hasGroup2 = false;

        boolean hasShortcut1 = false;
        boolean hasShortcut2 = false;
        boolean hasShortcut3 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group4-41-2")) {
                hasGroup2 = true;

                List<? extends OpentapsShortcut> shortcuts = group.getAllowedShortcuts();

                for (OpentapsShortcut shortcut : shortcuts) {
                    if (shortcut.getShortcutId().equalsIgnoreCase("shortcut1")) {
                        hasShortcut1 = true;
                    }
                }

                assertFalse("Found shortcut [shortcut1] in the [testapp4] [testtab41] [group4-41-2]", hasShortcut1);
            }
        }

        assertTrue("Could not find group [group4-41-2] in the [testapp4] [testtab41]", hasGroup2);

        // testtab42 / group1 should not appear
        tab = webappRepository.getTabById("testapp4", "testtab42");
        groups = webappRepository.getShortcutGroups(tab, context);

        hasGroup1 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group4-42-1")) {
                hasGroup1 = true;
            }
        }

        assertFalse("Found group [group4-42-1] in the [testapp4] [testtab42]", hasGroup1);

        // testtab42 / group2 / shortcut3 should not appear
        tab = webappRepository.getTabById("testapp4", "testtab42");
        groups = webappRepository.getShortcutGroups(tab, context);
                                ;
        hasGroup2 = false;
        hasShortcut3 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group4-42-2")) {
                hasGroup2 = true;

                List<? extends OpentapsShortcut> shortcuts = group.getAllowedShortcuts();

                for (OpentapsShortcut shortcut : shortcuts) {
                    if (shortcut.getShortcutId().equalsIgnoreCase("shortcut3")) {
                        hasShortcut3 = true;
                    }
                }

                assertFalse("Found shortcut [shortcut3] in the [testapp4] [testtab42] [group4-42-2]", hasShortcut3);
            }
        }

        assertTrue("Could not find group [group4-42-2] in the [testapp4] [testtab42]", hasGroup2);

        // setting the context shoppingCart to a non null value
        FakeHttpSession session = new FakeHttpSession();
        session.setAttribute("shoppingCart", "myCart");
        context.put("session", session);

        // testtab41 / group2 / shortcut1 should appear
        // testtab41 / group2 / shortcut2 should not appear
        tab = webappRepository.getTabById("testapp4", "testtab41");
        groups = webappRepository.getShortcutGroups(tab, context);

        hasGroup2 = false;
        hasShortcut1 = false;
        hasShortcut2 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group4-41-2")) {
                hasGroup2 = true;

                List<? extends OpentapsShortcut> shortcuts = group.getAllowedShortcuts();

                for (OpentapsShortcut shortcut : shortcuts) {
                    if (shortcut.getShortcutId().equalsIgnoreCase("shortcut1")) {
                        hasShortcut1 = true;
                    } else if (shortcut.getShortcutId().equalsIgnoreCase("shortcut2")) {
                        hasShortcut2 = true;
                    }
                }

                assertTrue("Could not find shortcut [shortcut1] in the [testapp4] [testtab41] [group4-41-2]", hasShortcut1);
                assertFalse("Found shortcut [shortcut2] in the [testapp4] [testtab41] [group4-41-2]", hasShortcut2);
            }
        }

        assertTrue("Could not find group [group4-41-2] in the [testapp4] [testtab41]", hasGroup2);

        // setting the context hasGroup1 to "Y"
        context.clear();
        context.put("hasGroup1", "Y");

        // testtab42 / group1 should appear, but without shortcuts inside
        tab = webappRepository.getTabById("testapp4", "testtab42");
        groups = webappRepository.getShortcutGroups(tab, context);

        hasGroup1 = false;
        hasShortcut1 = false;
        hasShortcut2 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group4-42-1")) {
                hasGroup1 = true;

                List<? extends OpentapsShortcut> shortcuts = group.getAllowedShortcuts();

                for (OpentapsShortcut shortcut : shortcuts) {
                    if (shortcut.getShortcutId().equalsIgnoreCase("shortcut1")) {
                        hasShortcut1 = true;
                    } else if (shortcut.getShortcutId().equalsIgnoreCase("shortcut2")) {
                        hasShortcut2 = true;
                    }
                }

                assertFalse("Found shortcut [shortcut1] in the [testapp4] [testtab42] [group4-42-1]", hasShortcut1);
                assertFalse("Found shortcut [shortcut2] in the [testapp4] [testtab42] [group4-42-1]", hasShortcut2);
            }
        }

        assertTrue("Could not find group [group4-42-1] in the [testapp4] [testtab42]", hasGroup1);

        // setting the context hasGroup1 to "Y" and hasShortcut2 to Boolean.TRUE
        context.put("hasShortcut2", new Boolean(true));

        //   testtab42 / group1 should appear
        //   testtab42 / group1 / shortcut1 should not appear
        //   testtab42 / group1 / shortcut2 should appear
        tab = webappRepository.getTabById("testapp4", "testtab42");
        groups = webappRepository.getShortcutGroups(tab, context);

        hasGroup1 = false;
        hasShortcut1 = false;
        hasShortcut2 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group4-42-1")) {
                hasGroup1 = true;

                List<? extends OpentapsShortcut> shortcuts = group.getAllowedShortcuts();

                for (OpentapsShortcut shortcut : shortcuts) {
                    if (shortcut.getShortcutId().equalsIgnoreCase("shortcut1")) {
                        hasShortcut1 = true;
                    } else if (shortcut.getShortcutId().equalsIgnoreCase("shortcut2")) {
                        hasShortcut2 = true;
                    }
                }

                assertFalse("Found shortcut [shortcut1] in the [testapp4] [testtab42] [group4-42-1]", hasShortcut1);
                assertTrue("Could not find shortcut [shortcut2] in the [testapp4] [testtab42] [group4-42-1]", hasShortcut2);
            }
        }

        assertTrue("Could not find group [group4-42-1] in the [testapp4] [testtab42]", hasGroup1);

        // setting the context opentapsApplicationName "test" sectionName to "test"
        context.clear();
        context.put("opentapsApplicationName", "test");
        context.put("sectionName", "test");
        context.put("userLogin", admin);

        // testtab42 / group1 should not appear
        // testtab42 / group2 / shortcut2 should not appear
        // testtab42 / group2 / shortcut3 should appear
        tab = webappRepository.getTabById("testapp4", "testtab42");
        groups = webappRepository.getShortcutGroups(tab, context);

        hasGroup1 = false;
        hasGroup2 = false;
        hasShortcut2 = false;
        hasShortcut3 = false;

        for (OpentapsShortcutGroup group : groups) {
            if (group.getGroupId().equalsIgnoreCase("group4-42-1")) {
                hasGroup1 = true;
            } else if (group.getGroupId().equalsIgnoreCase("group4-42-2")) {
                hasGroup2 = true;

                List<? extends OpentapsShortcut> shortcuts = group.getAllowedShortcuts();

                for (OpentapsShortcut shortcut : shortcuts) {
                    if (shortcut.getShortcutId().equalsIgnoreCase("shortcut2")) {
                        hasShortcut2 = true;
                    } else if (shortcut.getShortcutId().equalsIgnoreCase("shortcut3")) {
                        hasShortcut3 = true;
                    }
                }

                assertFalse("Found shortcut [shortcut2] in the [testapp4] [testtab42] [group4-42-2]", hasShortcut2);
                assertTrue("Could not find shortcut [shortcut3] in the [testapp4] [testtab42] [group4-42-2]", hasShortcut3);
            }
        }

        assertFalse("Found group [group4-42-1] in the [testapp4] [testtab42]", hasGroup1);
        assertTrue("Could not find group [group4-42-2] in the [testapp4] [testtab42]", hasGroup2);
    }

    /**
     * get for the app testapp5 set context values for foo51, bar51, etc ...
     * some set to ""
     * some set to some arbitrary strings or numbers
     * check that the values in the tab labels, links, group labels, shortcuts labels and links ... have the values
     * in the return of getLinkUrl() / getUiLabel()
     *
     * @throws Exception
     */
    public void testTabAndShortcutsExpansions() throws Exception {
        // testtab51
        WebAppDomainInterface webAppDomain = domainsDirectory.getWebAppDomain();
        WebAppRepositoryInterface webappRepository = webAppDomain.getWebAppRepository();
        Map<String, Object> context = FastMap.newInstance();

        context.put("foo51", "foo51_label");
        context.put("bar51", "bar51_linkUrl");
        context.put("foo52", "foo52_label");
        context.put("bar52", "");
        context.put("foo511", "foo511_label");
        context.put("s1", "s1_label");
        context.put("s11", "s11_linkUrl");
        context.put("s2", "s2_label");
        context.put("s22", "s22_linkUrl");

        boolean hasGroup1 = false;
        boolean hasShortcut1 = false;
        boolean hasShortcut2 = false;

        OpentapsWebApps webapp5 = webappRepository.getWebAppById("testapp5");
        List<? extends OpentapsWebAppTab> tabs = webappRepository.getWebAppTabs(webapp5, context);

        for (OpentapsWebAppTab tab : tabs) {
            if (tab.getTabId().equals("testtab51")) {
                assertEquals("UiLabels for [testtab51] is not equal to the expected value ", tab.getUiLabel(), "testtab51 foo51_label");
                assertEquals("LinkUrl for [testtab51] is not equal to the expected value ", tab.getLinkUrl(), "foobar51_linkUrl");

                List<OpentapsShortcutGroup> groups = webappRepository.getShortcutGroups(tab, context);

                for (OpentapsShortcutGroup group : groups) {
                    if (group.getGroupId().equalsIgnoreCase("group5-51-1")) {
                        hasGroup1 = true;

                        assertEquals("UiLabels for [testtab51] [group5-51-1] is not equal to the expected value ", group.getUiLabel(), "group1foo511_label");

                        List<? extends OpentapsShortcut> shortcuts = group.getAllowedShortcuts();

                        for (OpentapsShortcut shortcut : shortcuts) {
                            if (shortcut.getShortcutId().equalsIgnoreCase("shortcut1")) {
                                hasShortcut1 = true;

                                assertEquals("UiLabels for [testtab51] [group5-51-1] [shortcut1] is not equal to the expected value ", shortcut.getUiLabel(), "shortcut1s1_label");
                                assertEquals("LinkUrl for [testtab51] [group5-51-1] [shortcut1] is not equal to the expected value ", shortcut.getLinkUrl(), "foos11_linkUrl");

                            } else if (shortcut.getShortcutId().equalsIgnoreCase("shortcut2")) {
                                hasShortcut2 = true;

                                assertEquals("UiLabels for [testtab51] [group5-51-1] [shortcut2] is not equal to the expected value ", shortcut.getUiLabel(), "shortcut2s2_label");
                                assertEquals("LinkUrl for [testtab51] [group5-51-1] [shortcut2] is not equal to the expected value ", shortcut.getLinkUrl(), "foos22_linkUrl");
                            }
                        }

                        assertTrue("Could not find shortcut [shortcut1] in the [testapp5] [testtab51] [group5-51-1]", hasShortcut1);
                        assertTrue("Could not find shortcut [shortcut2] in the [testapp5] [testtab51] [group5-51-1]", hasShortcut2);
                    }
                }

                assertTrue("Could not find group [group5-51-1] in the [testapp5] [testtab51]", hasGroup1);

            } else if (tab.getTabId().equals("testtab52")) {
                assertEquals("UiLabels for [testtab52] is not equal to the expected value ", tab.getUiLabel(), "testtab52 foo52_label");
                assertEquals("LinkUrl for [testtab52] is not equal to the expected value ", tab.getLinkUrl(), "foo");
            }
        }
    }
}
