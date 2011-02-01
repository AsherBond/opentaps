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
        // 1. testapp1
        WebAppDomainInterface webAppDomain = domainsDirectory.getWebAppDomain();
        WebAppRepositoryInterface webappRepository = webAppDomain.getWebAppRepository();
        Map<String, Object> context = FastMap.newInstance();

        OpentapsWebApps webapp1 = webappRepository.getWebAppById("testapp1");
        List<? extends OpentapsWebAppTab> tabs = webappRepository.getWebAppTabs(webapp1, context);

        // the tabs list for testapp1 includes testtab11, testtab12 and testtab13
        checkTabs(tabs, Arrays.asList("testtab11", "testtab12", "testtab13"), "testapp1");

        // testtab11 not have a group
        OpentapsWebAppTab tab = webappRepository.getTabById("testapp1", "testtab11");
        List<OpentapsShortcutGroup> groups = webappRepository.getShortcutGroups(tab, context);

        assertEquals("The [testtab11] in the [testapp1] not have a group", groups.size(), 0);

        // testtab12 not have a group
        tab = webappRepository.getTabById("testapp1", "testtab12");
        groups = webappRepository.getShortcutGroups(tab, context);

        assertEquals("The [testtab12] in the [testapp1] not have a group", groups.size(), 0);

        // testtab13 not have a group
        tab = webappRepository.getTabById("testapp1", "testtab13");
        groups = webappRepository.getShortcutGroups(tab, context);

        assertEquals("The [testtab13] in the [testapp1] not have a group", groups.size(), 0);

        // 2. testapp2
        OpentapsWebApps webapp2 = webappRepository.getWebAppById("testapp2");
        tabs = webappRepository.getWebAppTabs(webapp2, context);

        // the tabs list for testapp2 includes testtab21 and testtab22
        checkTabs(tabs, Arrays.asList("testtab21", "testtab22"), "testapp2");

        tab = webappRepository.getTabById("testapp2", "testtab22");
        groups = webappRepository.getShortcutGroups(tab, context);

        // the groups list for testtab22 appears group1 and group2
        checkGroups(groups, Arrays.asList("group2-22-1", "group2-22-2"), "testtab22");
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
        // 1. testuser1
        GenericValue testuser1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "testuser1"));

        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(testuser1));
        WebAppDomainInterface webAppDomain = domainLoader.loadDomainsDirectory().getWebAppDomain();
        WebAppRepositoryInterface webappRepository = webAppDomain.getWebAppRepository();
        Map<String, Object> context = FastMap.newInstance();

        // test tab for testuser1
        OpentapsWebApps webapp3 = webappRepository.getWebAppById("testapp3");
        List<? extends OpentapsWebAppTab> tabs = webappRepository.getWebAppTabs(webapp3, context);

        // the tabs list for testapp3 includes testtab31 and testtab33
        checkTabs(tabs, Arrays.asList("testtab31", "testtab33"), "testapp3");

        // test groups for testuser1 tab1
        OpentapsWebAppTab tab1 = webappRepository.getTabById("testapp3", "testtab31");
        List<OpentapsShortcutGroup> groups = webappRepository.getShortcutGroups(tab1, context);

        // the groups list for testtab31 appears group1 and group2
        checkGroups(groups, Arrays.asList("group3-31-1", "group3-31-2"), "testtab31");

        // test groups for testuser1 tab2
        OpentapsWebAppTab tab2 = webappRepository.getTabById("testapp3", "testtab32");
        groups = webappRepository.getShortcutGroups(tab2, context);

        // the groups list for testtab32 appears only group1
        checkGroups(groups, Arrays.asList("group3-32-1"), "testtab32");

        // test groups for testuser1 tab3
        OpentapsWebAppTab tab3 = webappRepository.getTabById("testapp3", "testtab33");
        groups = webappRepository.getShortcutGroups(tab3, context);

        // the groups list for testtab33 appears group1 and group2
        checkGroups(groups, Arrays.asList("group3-33-1", "group3-33-2"), "testtab33");

        // 2. testuser2
        GenericValue testuser2 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "testuser2"));

        domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(testuser2));
        webAppDomain = domainLoader.loadDomainsDirectory().getWebAppDomain();
        webappRepository = webAppDomain.getWebAppRepository();

        // test tab for testuser2
        webapp3 = webappRepository.getWebAppById("testapp3");
        tabs = webappRepository.getWebAppTabs(webapp3, context);

        // the tabs list for testapp3 includes testtab31 and testtab32
        checkTabs(tabs, Arrays.asList("testtab31", "testtab32"), "testapp3");

        // test groups for testuser2 tab1
        tab1 = webappRepository.getTabById("testapp3", "testtab31");
        groups = webappRepository.getShortcutGroups(tab1, context);

        // the groups list for testtab31 appears only group1
        checkGroups(groups, Arrays.asList("group3-31-1"), "testtab31");

        // test groups for testuser2 tab2
        tab2 = webappRepository.getTabById("testapp3", "testtab32");
        groups = webappRepository.getShortcutGroups(tab2, context);

        // the groups list for testtab32 appears group1 and group2
        checkGroups(groups, Arrays.asList("group3-32-1", "group3-32-2"), "testtab32");

        // test groups for testuser2 tab3
        tab3 = webappRepository.getTabById("testapp3", "testtab33");
        groups = webappRepository.getShortcutGroups(tab3, context);

        // the groups list for testtab33 appears only group1
        checkGroups(groups, Arrays.asList("group3-33-1"), "testtab33");

        // 3. testuser3
        GenericValue testuser3 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "testuser3"));

        domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(testuser3));
        webAppDomain = domainLoader.loadDomainsDirectory().getWebAppDomain();
        webappRepository = webAppDomain.getWebAppRepository();

        // test tab for testuser3
        webapp3 = webappRepository.getWebAppById("testapp3");
        tabs = webappRepository.getWebAppTabs(webapp3, context);

        // the tabs list for testapp3 includes only testtab31
        checkTabs(tabs, Arrays.asList("testtab31"), "testapp3");

        // test groups for testuser3 tab1
        tab1 = webappRepository.getTabById("testapp3", "testtab31");
        groups = webappRepository.getShortcutGroups(tab1, context);

        // the groups list for testtab31 appears only group1
        checkGroups(groups, Arrays.asList("group3-31-1"), "testtab31");

        // test groups for testuser3 tab2
        tab2 = webappRepository.getTabById("testapp3", "testtab32");
        groups = webappRepository.getShortcutGroups(tab2, context);

        // the groups list for testtab32 appears only group1
        checkGroups(groups, Arrays.asList("group3-32-1"), "testtab32");

        // test groups for testuser3 tab3
        tab3 = webappRepository.getTabById("testapp3", "testtab33");
        groups = webappRepository.getShortcutGroups(tab3, context);

        // the groups list for testtab33 appears only group1
        checkGroups(groups, Arrays.asList("group3-33-1"), "testtab33");
    }

    /**
     * test with testapp4
     * will an empty context map:
     *    testtab41 / group2 / shortcut1 should not appear
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

        // 1. Empty context map:

        Map<String, Object> context = FastMap.newInstance();

        // testtab41 / group2 / shortcut1 should not appear
        OpentapsWebAppTab tab = webappRepository.getTabById("testapp4", "testtab41");
        List<OpentapsShortcutGroup> groups = webappRepository.getShortcutGroups(tab, context);

        // the shortcuts list for group4-41-2 should be empty
        checkShortcuts(groups,  UtilMisc.toMap("group4-41-2", (List<String>) new ArrayList<String>()));

        // testtab42 / group1 should not appear
        tab = webappRepository.getTabById("testapp4", "testtab42");
        groups = webappRepository.getShortcutGroups(tab, context);

        // the groups list for testtab42 appears only group2
        checkGroups(groups, Arrays.asList("group4-42-2"), "testtab42");

        // testtab42 / group2 / shortcut3 should not appear
        tab = webappRepository.getTabById("testapp4", "testtab42");
        groups = webappRepository.getShortcutGroups(tab, context);

        // the shortcuts list for group4-42-2 includes only shortcut1
        checkShortcuts(groups,  UtilMisc.toMap("group4-42-2", Arrays.asList("shortcut1")));

        // 2. Not empty context map:

        // setting the context shoppingCart to a non null value
        FakeHttpSession session = new FakeHttpSession();
        session.setAttribute("shoppingCart", "myCart");
        context.put("session", session);

        // testtab41 / group2 / shortcut1 should appear
        // testtab41 / group2 / shortcut2 should not appear
        tab = webappRepository.getTabById("testapp4", "testtab41");
        groups = webappRepository.getShortcutGroups(tab, context);

        // the shortcuts list for group4-41-2 includes only shortcut1
        checkShortcuts(groups,  UtilMisc.toMap("group4-41-2", Arrays.asList("shortcut1")));

        // setting the context hasGroup1 to "Y"
        context.clear();
        context.put("hasGroup1", "Y");

        // testtab42 / group1 should appear, but without shortcuts inside
        tab = webappRepository.getTabById("testapp4", "testtab42");
        groups = webappRepository.getShortcutGroups(tab, context);

        // the groups list for testtab42 appears group1 and group2
        checkGroups(groups, Arrays.asList("group4-42-1", "group4-42-2"), "testtab42");

        // the shortcuts list for group4-42-1 should be empty
        checkShortcuts(groups,  UtilMisc.toMap("group4-42-1", (List<String>) new ArrayList<String>()));

        // setting the context hasGroup1 to "Y" and hasShortcut2 to Boolean.TRUE
        context.put("hasShortcut2", new Boolean(true));

        //   testtab42 / group1 should appear
        //   testtab42 / group1 / shortcut1 should not appear
        //   testtab42 / group1 / shortcut2 should appear
        tab = webappRepository.getTabById("testapp4", "testtab42");
        groups = webappRepository.getShortcutGroups(tab, context);

        // the groups list for testtab42 appears group1 and group2
        checkGroups(groups, Arrays.asList("group4-42-1", "group4-42-2"), "testtab42");

        // the shortcuts list for group4-42-1 includes only shortcut2
        checkShortcuts(groups,  UtilMisc.toMap("group4-42-1", Arrays.asList("shortcut2")));

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

        // the groups list for testtab42 appears only group2
        checkGroups(groups, Arrays.asList("group4-42-2"), "testtab42");

        // the shortcuts list for group4-42-2 includes shortcut1 and shortcut3
        checkShortcuts(groups,  UtilMisc.toMap("group4-42-2", Arrays.asList("shortcut1", "shortcut3")));
    }

    /**
     * Comparison of actual and expected shortcuts
     *
     * @param groups List of the <code>OpentapsShortcutGroup</code>
     * @param expectedShortcuts Map of the <code>OpentapsShortcutGroup</code> Id and List of the expected Shortcuts Id
     */
    private void checkShortcuts(List<OpentapsShortcutGroup> groups, Map<String, List<String>> expectedShortcuts) {
        List<String> expectedShortcutsIds;
        List<String> actualShortcutsIds = new ArrayList<String>();

        for (OpentapsShortcutGroup group : groups) {
            expectedShortcutsIds =  expectedShortcuts.get(group.getGroupId());
            if (expectedShortcutsIds != null) {
                actualShortcutsIds.clear();
                List<? extends OpentapsShortcut> actualShortcuts = group.getAllowedShortcuts();

                for (OpentapsShortcut shortcut : actualShortcuts) {
                    actualShortcutsIds.add(shortcut.getShortcutId());
                }
                assertEquals("Actual shortcuts do not match the expected in the ["+group.getGroupId()+"] ",actualShortcutsIds, expectedShortcutsIds, false);
            }
        }
    }

    /**
     * Comparison of actual and expected shortcuts group
     *
     * @param groups List of the <code>OpentapsShortcutGroup</code>
     * @param expectedGroups List of the expected Groups Id
     * @param tabId an <code>OpentapsWebAppTab</code> Id
     */
    private void checkGroups(List<OpentapsShortcutGroup> groups, List<String> expectedGroups, String tabId) {
        List<String> actualGroups = new ArrayList<String>();

        for (OpentapsShortcutGroup group : groups) {
            actualGroups.add(group.getGroupId());
        }

        assertEquals("Actual groups do not match the expected in the ["+tabId+"] ",actualGroups, expectedGroups, false);
    }

    /**
     * Comparison of actual and expected tabs
     *
     * @param tabs List of the <code>OpentapsWebAppTab</code>
     * @param expectedTabs List of the expected Tabs Id
     * @param appId an <code>OpentapsWebApps</code> Id
     */
    private void checkTabs(List<? extends OpentapsWebAppTab> tabs, List<String> expectedTabs, String appId) {
        List<String> actualTabs = new ArrayList<String>();

        for (OpentapsWebAppTab tab : tabs) {
            actualTabs.add(tab.getTabId());
        }

        assertEquals("Actual tabs do not match the expected in the ["+appId+"] ",actualTabs, expectedTabs, false);
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
