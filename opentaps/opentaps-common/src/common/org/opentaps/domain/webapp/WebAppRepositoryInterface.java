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
package org.opentaps.domain.webapp;

import java.util.List;
import java.util.Map;

import org.opentaps.base.entities.OpentapsWebAppTab;
import org.opentaps.base.entities.OpentapsWebApps;
import org.opentaps.domain.webapp.OpentapsShortcutGroup;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Repository for OpentapsWebApps to handle interaction of OpentapsWebApps-related domain with the entity engine (database) and the service engine.
 */
public interface WebAppRepositoryInterface extends RepositoryInterface {

    /**
     * Gets the get a <code>OpentapsWebApps</code> by ID.
     * @param applicationId the webapp ID
     * @return the <code>OpentapsWebApps</code> instance, or null if not found
     * @throws RepositoryException if an error occurs
     */
    public OpentapsWebApps getWebAppById(String applicationId) throws RepositoryException;

    /**
     * Gets the get a <code>OpentapsWebAppTab</code> by ID.
     * @param applicationId the webapp ID
     * @param tabId the tab ID
     * @return the <code>OpentapsWebAppTab</code> instance, or null if not found
     * @throws RepositoryException if an error occurs
     */
    public OpentapsWebAppTab getTabById(String applicationId, String tabId) throws RepositoryException;

    /**
     * Gets the get the list of available webapps for the current user.
     * @return a List of <code>OpentapsWebApps</code> instance.
     * @throws RepositoryException if an error occurs
     */
    public List<? extends OpentapsWebApps> getWebApps() throws RepositoryException;

    /**
     * Gets the get the list of available webapps for given user.
     * @param user a <code>User</code> instance
     * @return a List of <code>OpentapsWebApps</code> instance.
     * @throws RepositoryException if an error occurs
     */
    public List<? extends OpentapsWebApps> getWebApps(User user) throws RepositoryException;

    /**
     * Gets the get the list of available tabs in the given webapp for the current user.
     * @param webapp an <code>OpentapsWebApps</code> instance
     * @param context the context Map
     * @return a List of <code>OpentapsWebApps</code> instance.
     * @throws RepositoryException if an error occurs
     */
    public List<? extends OpentapsWebAppTab> getWebAppTabs(OpentapsWebApps webapp, Map<String, Object> context) throws RepositoryException;

    /**
     * Gets the get the list of available tabs in the given webapp for given user.
     * @param webapp an <code>OpentapsWebApps</code> instance
     * @param user a <code>User</code> instance, null for anonymous
     * @param context the context Map
     * @return a List of <code>OpentapsWebApps</code> instance.
     * @throws RepositoryException if an error occurs
     */
    public List<? extends OpentapsWebAppTab> getWebAppTabs(OpentapsWebApps webapp, User user, Map<String, Object> context) throws RepositoryException;

    /**
     * Gets the get the list of available shortcut groups in the given tab for the current user.
     * @param tab an <code>OpentapsWebAppTab</code> instance
     * @param context the context Map
     * @return a List of <code>OpentapsShortcutGroup</code> instance.
     * @throws RepositoryException if an error occurs
     */
    public List<OpentapsShortcutGroup> getShortcutGroups(OpentapsWebAppTab tab, Map<String, Object> context) throws RepositoryException;

    /**
     * Gets the get the list of available shortcut groups in the given tab for given user.
     * @param tab an <code>OpentapsWebAppTab</code> instance
     * @param user a <code>User</code> instance, null for anonymous
     * @param context the context Map
     * @return a List of <code>OpentapsShortcutGroup</code> instance.
     * @throws RepositoryException if an error occurs
     */
    public List<OpentapsShortcutGroup> getShortcutGroups(OpentapsWebAppTab tab, User user, Map<String, Object> context) throws RepositoryException;

}
