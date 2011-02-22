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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.webapp.Shortcut;
import org.opentaps.domain.webapp.ShortcutGroup;
import org.opentaps.domain.webapp.Tab;
import org.opentaps.domain.webapp.WebAppDomainInterface;
import org.opentaps.domain.webapp.WebAppRepositoryInterface;
import org.opentaps.domain.webapp.Webapp;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.tests.OpentapsTestCase;

/**
 * Webapp related tests.
 * See WebappTestData.xml seed.
 */
public class WebappTests extends OpentapsTestCase {

    private static final String MODULE = WebappTests.class.getName();

    private static final List<String> EMPTY_SHORTCUTS_LIST = (List<String>) new ArrayList<String>();

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // Utility methods

    /**
     * Comparison of actual and expected shortcuts group.
     *
     * @param groups List of <code>ShortcutGroup</code>
     * @param expectedGroups List of the expected Groups Id
     * @param tabId an <code>Tab</code> Id
     */
    private void checkGroups(List<ShortcutGroup> groups, List<String> expectedGroups) {
        if (expectedGroups == null) {
            expectedGroups = new ArrayList<String>();
        }
        List<String> actualGroups = new ArrayList<String>();
        for (ShortcutGroup group : groups) {
            actualGroups.add(group.getGroupId());
        }
        assertEquals("Actual groups do not match the expected", actualGroups, expectedGroups, false);
    }

    /**
     * Comparison of actual and expected tabs.
     *
     * @param tabs List of <code>Tab</code>
     * @param expectedTabs List of the expected Tabs Id
     * @param appId an <code>Webapp</code> Id
     */
    private void checkTabs(List<? extends Tab> tabs, List<String> expectedTabs) {
        if (expectedTabs == null) {
            expectedTabs = new ArrayList<String>();
        }
        List<String> actualTabs = new ArrayList<String>();
        for (Tab tab : tabs) {
            actualTabs.add(tab.getTabId());
        }
        assertEquals("Actual tabs do not match the expected", actualTabs, expectedTabs, false);
    }

    /**
     * Comparison of actual and expected shortcuts.
     *
     * @param groups List of <code>ShortcutGroup</code>
     * @param expectedShortcuts Map of the <code>ShortcutGroup</code> Id and List of the expected Shortcuts Id
     * @param appId an <code>Webapp</code> Id
     */
    private void checkShortcuts(List<ShortcutGroup> groups, Map<String, List<String>> expectedShortcuts) {
        if (expectedShortcuts == null) {
            expectedShortcuts = new HashMap<String, List<String>>();
        }

        List<String> expectedShortcutsIds;
        List<String> actualShortcutsIds = new ArrayList<String>();

        for (ShortcutGroup group : groups) {
            expectedShortcutsIds =  expectedShortcuts.get(group.getGroupId());
            if (expectedShortcutsIds == null) {
                expectedShortcutsIds = new ArrayList<String>();
            }
            actualShortcutsIds.clear();
            List<? extends Shortcut> actualShortcuts = group.getAllowedShortcuts();

            for (Shortcut shortcut : actualShortcuts) {
                actualShortcutsIds.add(shortcut.getShortcutId());
            }
            assertEquals("Actual shortcuts do not match the expected in group [" + group.getGroupId() + "]", actualShortcutsIds, expectedShortcutsIds, false);
        }
    }

    /**
     * Loads a <code>WebAppRepositoryInterface</code> for a given user.
     *
     * @param userLoginId a <code>String</code> value
     * @return a <code>WebAppRepositoryInterface</code> value
     * @exception Exception if an error occurs
     */
    private WebAppRepositoryInterface getWebappRepository(String userLoginId) throws Exception {
        GenericValue testuser1 = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(testuser1));
        WebAppDomainInterface webAppDomain = domainLoader.getDomainsDirectory().getWebAppDomain();
        return webAppDomain.getWebAppRepository();
    }

    // Tests

    /**
     * Test basic functionality.
     *
     * test that the webapp repository gives the correct tab list in each application :
     * Webapp webapp1 = webappRepository.getWebAppById("testapp1")
     * tabs = webappRepository.getWebAppTabs(webapp1, map{})
     * -> check tabs includes testtab11, testtab12, testtab13 ...
     * test that tabs include only the expected for this application tabs (and no extra one)
     * tab = webappRepository.getTabById(opentapsApplicationName, sectionName);
     * groups = webappRepository.getShortcutGroups(tab, user, context);
     * -> test groups includes the corresponding groups
     * same for testapp2
     *
     * @throws Exception if an error occurs
     */
    public void testBasicGetTabList() throws Exception {
        // 1. testapp1
        WebAppDomainInterface webAppDomain = domainsDirectory.getWebAppDomain();
        WebAppRepositoryInterface webappRepository = webAppDomain.getWebAppRepository();
        Map<String, Object> context = FastMap.newInstance();

        Webapp webapp1 = webappRepository.getWebAppById("testapp1");
        List<? extends Tab> tabs = webappRepository.getWebAppTabs(webapp1, context);

        // check that the tabs for testapp1 are: testtab11, testtab12 and testtab13
        checkTabs(tabs, Arrays.asList("testtab11", "testtab12", "testtab13"));

        // check the shortcut groups of each tab, here they all have no shortcut group
        // testtab11
        Tab tab = webappRepository.getTabById("testapp1", "testtab11");
        List<ShortcutGroup> groups = webappRepository.getShortcutGroups(tab, context);
        assertEquals("The [testtab11] in the [testapp1] should not have any shortcut group", groups.size(), 0);

        // testtab12
        tab = webappRepository.getTabById("testapp1", "testtab12");
        groups = webappRepository.getShortcutGroups(tab, context);
        assertEquals("The [testtab12] in the [testapp1] should not have any shortcut group", groups.size(), 0);

        // testtab13
        tab = webappRepository.getTabById("testapp1", "testtab13");
        groups = webappRepository.getShortcutGroups(tab, context);
        assertEquals("The [testtab13] in the [testapp1] should not have any shortcut group", groups.size(), 0);

        // 2. testapp2
        Webapp webapp2 = webappRepository.getWebAppById("testapp2");
        tabs = webappRepository.getWebAppTabs(webapp2, context);

        // check that the tabs for testapp2 are: testtab21 and testtab22
        checkTabs(tabs, Arrays.asList("testtab21", "testtab22"));

        tab = webappRepository.getTabById("testapp2", "testtab22");
        groups = webappRepository.getShortcutGroups(tab, context);

        // check that the groups for testtab22 are: group1 and group2, and check the shortcuts of each group
        checkGroups(groups, Arrays.asList("group2-22-1", "group2-22-2"));
        checkShortcuts(groups, UtilMisc.toMap("group2-22-1", Arrays.asList("shortcut1", "shortcut2"),
                                              "group2-22-2", Arrays.asList("shortcut1", "shortcut2", "shortcut3")));
    }

    /**
     * Test permission checking with testapp2.
     *
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
     * one having TESTAPP3T1G2_VIEW, TESTAPP3T3_VIEW - testuser1
     * one having TESTAPP3T2_VIEW, TESTAPP3T2G2_VIEW - testuser2
     * one having no specific permission - testuser3
     *
     * @throws Exception if an error occurs
     */
    public void testPermissionCheckOnGetTabList() throws Exception {
        Map<String, Object> context = FastMap.newInstance();

        // 1. testuser1
        WebAppRepositoryInterface webappRepository = getWebappRepository("testuser1");
        Webapp webapp3 = webappRepository.getWebAppById("testapp3");
        List<? extends Tab> tabs = webappRepository.getWebAppTabs(webapp3, context);

        // check that the tabs for testapp3 are: testtab31 and testtab33, this user does not have permission for testtab32
        checkTabs(tabs, Arrays.asList("testtab31", "testtab33"));

        // test groups for testuser1 tab1
        Tab tab1 = webappRepository.getTabById("testapp3", "testtab31");
        List<ShortcutGroup> groups = webappRepository.getShortcutGroups(tab1, context);
        checkGroups(groups, Arrays.asList("group3-31-1", "group3-31-2"));
        checkShortcuts(groups, UtilMisc.toMap("group3-31-1", Arrays.asList("shortcut1"),
                                              "group3-31-2", Arrays.asList("shortcut1")));

        // test groups for testuser1 tab2, should be empty since this user does not have permission for testtab32
        Tab tab2 = webappRepository.getTabById("testapp3", "testtab32");
        groups = webappRepository.getShortcutGroups(tab2, context);
        checkGroups(groups, null);

        // test groups for testuser1 tab3, only group 1 as this user does not have permission for the group 2
        Tab tab3 = webappRepository.getTabById("testapp3", "testtab33");
        groups = webappRepository.getShortcutGroups(tab3, context);
        checkGroups(groups, Arrays.asList("group3-33-1"));
        checkShortcuts(groups, UtilMisc.toMap("group3-33-1", Arrays.asList("shortcut1")));

        // 2. testuser2
        webappRepository = getWebappRepository("testuser2");

        // test tab for testuser2
        webapp3 = webappRepository.getWebAppById("testapp3");
        tabs = webappRepository.getWebAppTabs(webapp3, context);

        // check that the tabs for testapp3 are: testtab31 and testtab32, this user does not have permission for testtab33
        checkTabs(tabs, Arrays.asList("testtab31", "testtab32"));

        // test groups for testuser2 tab1, only group 1 has this user does not have permission for the group 2
        tab1 = webappRepository.getTabById("testapp3", "testtab31");
        groups = webappRepository.getShortcutGroups(tab1, context);
        checkGroups(groups, Arrays.asList("group3-31-1"));
        checkShortcuts(groups, UtilMisc.toMap("group3-31-1", Arrays.asList("shortcut1")));

        // test groups for testuser2 tab2, all groups are visible
        tab2 = webappRepository.getTabById("testapp3", "testtab32");
        groups = webappRepository.getShortcutGroups(tab2, context);
        checkGroups(groups, Arrays.asList("group3-32-1", "group3-32-2"));
        checkShortcuts(groups, UtilMisc.toMap("group3-32-1", Arrays.asList("shortcut1"),
                                              "group3-32-2", Arrays.asList("shortcut1")));

        // test groups for testuser2 tab3, empty since this user does not have permission
        tab3 = webappRepository.getTabById("testapp3", "testtab33");
        groups = webappRepository.getShortcutGroups(tab3, context);
        checkGroups(groups, null);

        // 3. testuser3
        webappRepository = getWebappRepository("testuser3");

        // test tab for testuser3
        webapp3 = webappRepository.getWebAppById("testapp3");
        tabs = webappRepository.getWebAppTabs(webapp3, context);

        // check that the tabs for testapp3 are: only testtab31, this user does not have permission for testtab32 and testtab33
        checkTabs(tabs, Arrays.asList("testtab31"));

        // test groups for testuser3 tab1, includes only group 1 since the user has not permission for the group 2
        tab1 = webappRepository.getTabById("testapp3", "testtab31");
        groups = webappRepository.getShortcutGroups(tab1, context);
        checkGroups(groups, Arrays.asList("group3-31-1"));
        checkShortcuts(groups, UtilMisc.toMap("group3-31-1", Arrays.asList("shortcut1")));

        // test groups for testuser3 tab2, empty since the user has not permission for this tab
        tab2 = webappRepository.getTabById("testapp3", "testtab32");
        groups = webappRepository.getShortcutGroups(tab2, context);
        checkGroups(groups, null);

        // test groups for testuser3 tab3, empty since the user has not permission for this tab
        tab3 = webappRepository.getTabById("testapp3", "testtab33");
        groups = webappRepository.getShortcutGroups(tab3, context);
        checkGroups(groups, null);
    }

    /**
     * Test handler methods with testapp4.
     *
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
     * @throws Exception if an error occurs
     */
    public void testHandlerMethods() throws Exception {
        // test with testapp4
        WebAppDomainInterface webAppDomain = domainsDirectory.getWebAppDomain();
        WebAppRepositoryInterface webappRepository = webAppDomain.getWebAppRepository();

        // 1. Empty context map:

        Map<String, Object> context = FastMap.newInstance();

        // testtab41
        Tab tab = webappRepository.getTabById("testapp4", "testtab41");
        List<ShortcutGroup> groups = webappRepository.getShortcutGroups(tab, context);
        // includes group1 and group2
        checkGroups(groups, Arrays.asList("group4-41-1", "group4-41-2"));
        // in group 2, shortcut 1 should not appear (needs a cart)
        checkShortcuts(groups, UtilMisc.toMap("group4-41-1", Arrays.asList("shortcut1"),
                                              "group4-41-2", Arrays.asList("shortcut2")));

        // testtab42
        tab = webappRepository.getTabById("testapp4", "testtab42");
        groups = webappRepository.getShortcutGroups(tab, context);
        // group1 should not appear (needs a hasGroup1 context value)
        checkGroups(groups, Arrays.asList("group4-42-2"));
        // in group2 / shortcut3 should not appear as it needs a view pref value, and shortcut2 needs a UserLogin in context
        checkShortcuts(groups, UtilMisc.toMap("group4-42-2", Arrays.asList("shortcut1")));

        // 2. Not empty context map:

        // setting the context shoppingCart to a non null value
        FakeHttpSession session = new FakeHttpSession();
        session.setAttribute("shoppingCart", "myCart");
        context.put("session", session);

        tab = webappRepository.getTabById("testapp4", "testtab41");
        groups = webappRepository.getShortcutGroups(tab, context);

        // this changed the group 2, now shortcut 1 is visible and shortcut 2 is hidden
        checkShortcuts(groups, UtilMisc.toMap("group4-41-1", Arrays.asList("shortcut1"),
                                              "group4-41-2", Arrays.asList("shortcut1")));

        // setting the context hasGroup1 to "Y"
        context.clear();
        context.put("hasGroup1", "Y");

        // testtab42 / group1 should appear, but without shortcuts inside
        tab = webappRepository.getTabById("testapp4", "testtab42");
        groups = webappRepository.getShortcutGroups(tab, context);

        // now group 1 and 2 are present
        checkGroups(groups, Arrays.asList("group4-42-1", "group4-42-2"));
        // the shortcuts list for group4-42-1 should be empty, group 2 contains shortcuts 1
        checkShortcuts(groups, UtilMisc.toMap("group4-42-1", EMPTY_SHORTCUTS_LIST,
                                              "group4-42-2", Arrays.asList("shortcut1")));

        // setting the context hasGroup1 to "Y" and hasShortcut2 to Boolean.TRUE
        context.put("hasShortcut2", new Boolean(true));
        // setting the userLogin,  to allow the view preference to take effect, it will then use the default value (since the section name is missing from context)
        context.put("userLogin", admin);

        // same as before but now group 1 shortcut 2 and group 2 shortcut 2 are present
        tab = webappRepository.getTabById("testapp4", "testtab42");
        groups = webappRepository.getShortcutGroups(tab, context);
        checkGroups(groups, Arrays.asList("group4-42-1", "group4-42-2"));
        checkShortcuts(groups,  UtilMisc.toMap("group4-42-1", Arrays.asList("shortcut2"),
                                               "group4-42-2", Arrays.asList("shortcut1", "shortcut2")));

        // setting the context opentapsApplicationName "test" sectionName to "test" to allow the view preference to take effect with the test value
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
        checkGroups(groups, Arrays.asList("group4-42-2"));

        // the shortcuts list for group4-42-2 includes shortcut1 and shortcut3
        checkShortcuts(groups,  UtilMisc.toMap("group4-42-2", Arrays.asList("shortcut1", "shortcut3")));
    }

    /**
     * get for the app testapp5 set context values for foo51, bar51, etc ...
     * some set to ""
     * some set to some arbitrary strings or numbers
     * check that the values in the tab labels, links, group labels, shortcuts labels and links ... have the values
     * in the return of getLinkUrl() / getUiLabel()
     *
     * @throws Exception if an error occurs
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

        Webapp webapp5 = webappRepository.getWebAppById("testapp5");
        List<? extends Tab> tabs = webappRepository.getWebAppTabs(webapp5, context);

        // check the correct expansion of labels and links
        for (Tab tab : tabs) {
            if (tab.getTabId().equals("testtab51")) {
                assertEquals("UiLabels for [testtab51] is not equal to the expected value ", tab.getUiLabel(), "testtab51 foo51_label");
                assertEquals("LinkUrl for [testtab51] is not equal to the expected value ", tab.getLinkUrl(), "foobar51_linkUrl");

                List<ShortcutGroup> groups = webappRepository.getShortcutGroups(tab, context);

                for (ShortcutGroup group : groups) {
                    if (group.getGroupId().equalsIgnoreCase("group5-51-1")) {
                        hasGroup1 = true;

                        assertEquals("UiLabels for [testtab51] [group5-51-1] is not equal to the expected value ", group.getUiLabel(), "group1foo511_label");

                        List<? extends Shortcut> shortcuts = group.getAllowedShortcuts();

                        for (Shortcut shortcut : shortcuts) {
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

    /**
     * Tests the controller injection.
     * @exception Exception if an error occurs
     */
    public void testControllerInjection() throws Exception {
        // assuming we have a test injection seed date, for example
        //  <ControllerInjection injectUrl="component://opentaps-tests/webapp/data/test-injected-controller.xml" injectIntoUrl="component://crmsfa/webapp/crmsfa/WEB-INF/controller.xml" />

        // 1. get the ControllerConfig object from ConfigXMLReader.getControllerConfig("component://crmsfa/webapp/crmsfa/WEB-INF/controller.xml");
        // 2. call getRequestMapMap
        //  a. you should find the new injected requests (for uri defined in the injected controller that did not exist in the target controller)
        //  b. you should get the overridden requests (for uri defined in the injected controller that also existed in the target controller)
        // 3. same for the view map from getViewMapMap
        // 4. same for getPreprocessorEventList (<preprocessor>..</preprocessor> in the controller)
        // 5. same for getPostprocessorEventList
        // 6. same for getAfterLoginEventList
        // 7. same for getFirstVisitEventList


        // second test for a more complicated scenario, with the seed data to inject 3 different controllers into a target controller
        // do the same test but also make sure that the controller injection sequence is respected (with sequenceNum), so the last injected controller has its requests / views / etc... that overrides those of the first 2
    }
}
