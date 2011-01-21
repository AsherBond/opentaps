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
package org.opentaps.webapp;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.component.ComponentConfig.WebappInfo;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.opentaps.base.entities.OpentapsShortcut;
import org.opentaps.base.entities.OpentapsWebAppTab;
import org.opentaps.base.entities.OpentapsWebApps;
import org.opentaps.domain.webapp.OpentapsShortcutGroup;
import org.opentaps.domain.webapp.WebAppRepositoryInterface;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

/**
 * the web app repository class.
 */
public class WebAppRepository  extends Repository implements WebAppRepositoryInterface {

    private static final String MODULE = WebAppRepository.class.getName();

    /** {@inheritDoc} */
    public OpentapsWebApps getWebAppById(String applicationId) throws RepositoryException {
        return findOneCache(OpentapsWebApps.class, map(OpentapsWebApps.Fields.applicationId, applicationId));
    }

    /** {@inheritDoc} */
    public OpentapsWebAppTab getTabById(String applicationId, String tabId) throws RepositoryException {
        return findOneCache(OpentapsWebAppTab.class, map(OpentapsWebAppTab.Fields.applicationId, applicationId,
                                                         OpentapsWebAppTab.Fields.tabId, tabId));
    }

    /** {@inheritDoc} */
    public List<? extends OpentapsWebApps> getWebApps() throws RepositoryException {
        return getWebApps(getUser());
    }

    /** {@inheritDoc} */
    public List<? extends OpentapsWebApps> getWebApps(User user) throws RepositoryException {
        List<OpentapsWebApps> opentapsWebapps = findAllCache(OpentapsWebApps.class, Arrays.asList(OpentapsWebApps.Fields.sequenceNum.asc()));
        //get all webapps defined in all the ofbiz-components
        List<WebappInfo> webapps = ComponentConfig.getAllWebappResourceInfos();
        Debug.logVerbose("number of webapps found = " + webapps.size(), MODULE);
        Map<String, String[]> webappsMap = FastMap.newInstance();
        //create a map entry (name , permissions[]) for every webapp
        for (WebappInfo webapp : webapps) {
            webappsMap.put(webapp.getName() , webapp.getBasePermission());
        }
        List<OpentapsWebApps> apps = FastList.newInstance();
        if (UtilValidate.isNotEmpty(opentapsWebapps)) {
            for (OpentapsWebApps webapp : opentapsWebapps) {
                String[] permissions = webappsMap.get(webapp.getApplicationId());
                if (user != null) {
                    Debug.logVerbose("Checking permissions for user [" + user + "]", MODULE);
                    boolean permitted = true;
                    if (permissions != null) {
                        //  if there are permissions for this application, then check if the user can view it
                        for (int i = 0; i < permissions.length; i++) {
                            // if the application has basePermissions and user doesn't has VIEW/ADMIN permissions on them, don't get the app
                            try {
                                if (!"NONE".equals(permissions[i]) && !user.hasPermission(permissions[i], "VIEW") && !user.hasAdminPermissionsForModule(permissions[i])) {
                                    Debug.logVerbose("User [" + user + "] does not have permission for webapp [" + webapp.getApplicationId() + "]", MODULE);
                                    permitted = false;
                                    break;
                                }
                            } catch (InfrastructureException e) {
                                Debug.logError(e, MODULE);
                            }
                        }
                    }
                    if (permitted) {
                        Debug.logVerbose("Webapp [" + webapp.getApplicationId() + "] is enabled for user [" + user + "]", MODULE);
                        apps.add(webapp);
                    }
                } else {
                    // if user is not authenticated
                    if (permissions == null) {
                        // if there are no permissions required for the application, or if it is an external link,
                        apps.add(webapp);
                    } else if (permissions.length > 0) {
                        //  or, if the application is defined with permission of "NONE",  such as the ofbiz e-commerce store
                        if ("NONE".equals(permissions[0])) {
                            //permissions[0] will always exists
                            apps.add(webapp);
                        }
                    }
                }
            }

        }
        return apps;
    }

    private boolean hasPermission(User user, String secModule, String secAction) throws RepositoryException {
        // if a module permission is given
        if (UtilValidate.isNotEmpty(secModule)) {
            // if no action is given defaults to "VIEW"
            if (UtilValidate.isEmpty(secAction)) {
                secAction = "VIEW";
            }
            // if the permission check fails
            try {
                if (user == null || !user.hasPermission(secModule, secAction) && !user.hasAdminPermissionsForModule(secModule)) {
                    Debug.logWarning("Permission [" + secModule + " / " + secAction + "] denied for user [" + user.getUserId() + "]", MODULE);
                    return false;
                }
            } catch (InfrastructureException e) {
                Debug.logError(e, MODULE);
                return false;
            }
        }
        return true;
    }

    private <T extends EntityInterface> T callHandler(T obj, String handlerMethod, Map<String, Object> context) {
        // if there is a handler, pass the obj to it
        if (UtilValidate.isNotEmpty(handlerMethod)) {
            // resolve the handler method
            String className = handlerMethod.substring(0, handlerMethod.lastIndexOf("."));
            String methodName = handlerMethod.substring(handlerMethod.lastIndexOf(".") + 1);
            try {
                Class<?> c = this.getClass().getClassLoader().loadClass(className);
                Method m = c.getMethod(methodName, Map.class, EntityInterface.class);
                if (Modifier.isStatic(m.getModifiers())) {
                    obj = (T) m.invoke(null, context, obj);
                } else {
                    obj = (T) m.invoke(c.newInstance(), context, obj);
                }
            } catch (Exception e) {
                Debug.logError(e, MODULE);
                // skip this object on error
                return null;
            }
        }
        return obj;
    }

    private <T extends EntityInterface> void expandFields(T obj, Map<String, Object> context) {
        // expand the uiLabel
        String s = obj.getString("uiLabel");
        if (UtilValidate.isNotEmpty(s)) {
            obj.set("uiLabel", FlexibleStringExpander.expandString(s, context, Locale.getDefault()));
        }
        // expand the link
        s = obj.getString("linkUrl");
        if (UtilValidate.isNotEmpty(s)) {
            obj.set("linkUrl", FlexibleStringExpander.expandString(s, context, Locale.getDefault()));
        }
    }

    /** {@inheritDoc} */
    public List<? extends OpentapsWebAppTab> getWebAppTabs(OpentapsWebApps webapp, Map<String, Object> context) throws RepositoryException {
        return getWebAppTabs(webapp, getUser(), context);
    }

    /** {@inheritDoc} */
    public List<? extends OpentapsWebAppTab> getWebAppTabs(OpentapsWebApps webapp, User user, Map<String, Object> context) throws RepositoryException {
        List<OpentapsWebAppTab> tabs = findListCache(OpentapsWebAppTab.class, map(OpentapsWebAppTab.Fields.applicationId, webapp.getApplicationId()), Arrays.asList(OpentapsWebAppTab.Fields.sequenceNum.asc()));
        List<OpentapsWebAppTab> allowedTabs = new ArrayList<OpentapsWebAppTab>();
        // check permission on each tab
        for (OpentapsWebAppTab tab : tabs) {
            String secModule = tab.getSecurityModule();
            String secAction = tab.getSecurityAction();
            // check permission or skip this tab
            if (!hasPermission(user, secModule, secAction)) {
                continue;
            }
            // if there is a handler, pass the OpentapsWebAppTab to it
            tab = callHandler(tab, tab.getHandlerMethod(), context);

            // handler may have set the tab to null
            if (tab != null) {
                expandFields(tab, context);
                allowedTabs.add(tab);
            }
        }

        return allowedTabs;
    }

    /** {@inheritDoc} */
    public List<OpentapsShortcutGroup> getShortcutGroups(OpentapsWebAppTab tab, Map<String, Object> context) throws RepositoryException {
        return getShortcutGroups(tab, getUser(), context);
    }

    /** {@inheritDoc} */
    public List<OpentapsShortcutGroup> getShortcutGroups(OpentapsWebAppTab tab, User user, Map<String, Object> context) throws RepositoryException {
        List<OpentapsShortcutGroup> groups = findListCache(OpentapsShortcutGroup.class, map(OpentapsShortcutGroup.Fields.applicationId, tab.getApplicationId(), OpentapsShortcutGroup.Fields.tabId, tab.getTabId()), Arrays.asList(OpentapsShortcutGroup.Fields.sequenceNum.asc()));
        Debug.logInfo("Found group candidates " + groups, MODULE);
        List<OpentapsShortcutGroup> allowedGroups = new ArrayList<OpentapsShortcutGroup>();
        // check permission on each group
        for (OpentapsShortcutGroup group : groups) {
            // check permission or skip this group
            if (!hasPermission(user, group.getSecurityModule(), group.getSecurityAction())) {
                continue;
            }
            // if there is a handler, pass the OpentapsShortcutGroup to it
            group = callHandler(group, group.getHandlerMethod(), context);

            // handler may have set the tab to null
            if (group != null) {
                // load the allowed shortcuts in each group
                List<OpentapsShortcut> allowedShortcuts = new ArrayList<OpentapsShortcut>();
                for (OpentapsShortcut shortcut : findListCache(OpentapsShortcut.class, map(OpentapsShortcut.Fields.groupId, group.getGroupId()), Arrays.asList(OpentapsShortcut.Fields.sequenceNum.asc()))) {
                    // check permission or skip this group
                    if (!hasPermission(user, shortcut.getSecurityModule(), shortcut.getSecurityAction())) {
                        continue;
                    }
                    // if there is a handler, pass the OpentapsShortcutGroup to it
                    shortcut = callHandler(shortcut, shortcut.getHandlerMethod(), context);
                    if (shortcut != null) {
                        expandFields(shortcut, context);
                        allowedShortcuts.add(shortcut);
                    }
                }
                group.setAllowedShortcuts(allowedShortcuts);
                expandFields(group, context);
                allowedGroups.add(group);
            }
        }

        return allowedGroups;
    }
}
