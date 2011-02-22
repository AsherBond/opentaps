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
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.component.ComponentConfig.WebappInfo;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.domain.webapp.Shortcut;
import org.opentaps.domain.webapp.ShortcutGroup;
import org.opentaps.domain.webapp.Tab;
import org.opentaps.domain.webapp.WebAppRepositoryInterface;
import org.opentaps.domain.webapp.Webapp;
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
    public Webapp getWebAppById(String applicationId) throws RepositoryException {
        return findOneCache(Webapp.class, map(Webapp.Fields.applicationId, applicationId));
    }

    /** {@inheritDoc} */
    public Tab getTabById(String applicationId, String tabId) throws RepositoryException {
        return findOneCache(Tab.class, map(Tab.Fields.applicationId, applicationId,
                                                         Tab.Fields.tabId, tabId));
    }

    /** {@inheritDoc} */
    public List<? extends Webapp> getWebApps() throws RepositoryException {
        return getWebApps(getUser());
    }

    /** {@inheritDoc} */
    public List<? extends Webapp> getWebApps(User user) throws RepositoryException {
        List<Webapp> opentapsWebapps = findAllCache(Webapp.class, Arrays.asList(Webapp.Fields.sequenceNum.asc()));
        //get all webapps defined in all the ofbiz-components
        List<WebappInfo> webapps = ComponentConfig.getAllWebappResourceInfos();
        Debug.logVerbose("number of webapps found = " + webapps.size(), MODULE);
        Map<String, String[]> webappsMap = FastMap.newInstance();
        //create a map entry (name , permissions[]) for every webapp
        for (WebappInfo webapp : webapps) {
            webappsMap.put(webapp.getName() , webapp.getBasePermission());
        }
        List<Webapp> apps = FastList.newInstance();
        if (UtilValidate.isNotEmpty(opentapsWebapps)) {
            for (Webapp webapp : opentapsWebapps) {
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
                    Debug.logWarning("Permission [" + secModule + " / " + secAction + "] denied for user [" + ((user != null) ? user.getUserId() : user) + "]", MODULE);
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

    /** {@inheritDoc} */
    public List<? extends Tab> getWebAppTabs(Webapp webapp, Map<String, Object> context) throws RepositoryException {
        return getWebAppTabs(webapp, getUser(), context);
    }

    /** {@inheritDoc} */
    public List<? extends Tab> getWebAppTabs(Webapp webapp, User user, Map<String, Object> context) throws RepositoryException {
        List<Tab> tabs = findListCache(Tab.class, map(Tab.Fields.applicationId, webapp.getApplicationId()), Arrays.asList(Tab.Fields.sequenceNum.asc()));
        List<Tab> allowedTabs = new ArrayList<Tab>();
        // check permission on each tab
        for (Tab tab : tabs) {
            String secModule = tab.getSecurityModule();
            String secAction = tab.getSecurityAction();
            // check permission or skip this tab
            if (!hasPermission(user, secModule, secAction)) {
                tab.setDisabled(true);
                if (tab.isHidden()) {
                    continue;
                }
            }
            // if there is a handler, pass the Tab to it
            tab = callHandler(tab, tab.getHandlerMethod(), context);

            // handler may have set the tab to null, or set is as hidden
            if (tab == null || tab.isHidden()) {
                continue;
            }
            tab.expandFields(context);
            allowedTabs.add(tab);
        }

        return allowedTabs;
    }

    /** {@inheritDoc} */
    public List<ShortcutGroup> getShortcutGroups(Tab tab, Map<String, Object> context) throws RepositoryException {
        return getShortcutGroups(tab, getUser(), context);
    }

    /** {@inheritDoc} */
    public List<ShortcutGroup> getShortcutGroups(Tab tab, User user, Map<String, Object> context) throws RepositoryException {
        List<ShortcutGroup> allowedGroups = new ArrayList<ShortcutGroup>();
        // check if the user has permission on the tab, else do not return any group
        if (tab == null || tab.isHidden() || !hasPermission(user, tab.getSecurityModule(), tab.getSecurityAction())) {
            return allowedGroups;
        }
        // if there is a handler, pass the Tab to it
        tab = callHandler(tab, tab.getHandlerMethod(), context);
        if (tab == null || tab.isHidden()) {
            return allowedGroups;
        }

        List<ShortcutGroup> groups = findListCache(ShortcutGroup.class, map(ShortcutGroup.Fields.applicationId, tab.getApplicationId(), ShortcutGroup.Fields.tabId, tab.getTabId()), Arrays.asList(ShortcutGroup.Fields.sequenceNum.asc()));
        Debug.logInfo("Found group candidates " + groups, MODULE);
        // check permission on each group
        for (ShortcutGroup group : groups) {
            // check permission
            if (!hasPermission(user, group.getSecurityModule(), group.getSecurityAction())) {
                group.setDisabled(true);
                if (group.isHidden()) {
                    continue;
                }
            }
            // if there is a handler, pass the ShortcutGroup to it
            group = callHandler(group, group.getHandlerMethod(), context);

            // handler may have set the group to null or hidden
            if (group == null || group.isHidden()) {
                continue;
            }

            // load the allowed shortcuts in each group
            List<Shortcut> allowedShortcuts = new ArrayList<Shortcut>();
            for (Shortcut shortcut : findListCache(Shortcut.class, map(Shortcut.Fields.groupId, group.getGroupId()), Arrays.asList(Shortcut.Fields.sequenceNum.asc()))) {
                // check permission
                if (group.isDisabled() || !hasPermission(user, shortcut.getSecurityModule(), shortcut.getSecurityAction())) {
                    shortcut.setDisabled(true);
                    if (shortcut.isHidden()) {
                        continue;
                    }
                }
                // if there is a handler, pass the ShortcutGroup to it
                shortcut = callHandler(shortcut, shortcut.getHandlerMethod(), context);
                if (shortcut == null || shortcut.isHidden()) {
                    continue;
                }
                shortcut.expandFields(context);
                allowedShortcuts.add(shortcut);
            }
            group.setAllowedShortcuts(allowedShortcuts);
            group.expandFields(context);
            allowedGroups.add(group);
        }

        return allowedGroups;
    }
}
