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

package org.opentaps.gwt.common.client.form;

/**
 * Interface for registering objects to a Form.
 * @param <T> the type to return in the notification
 */
public interface FormNotificationInterface<T> {

    /**
     * Callback method called after a Form has been successfully submitted.
     * Notification provider may pass to receiver an object that has no particular meaning and should be handled according to execution context.
     * @param obj An object of type T, may be <code>null</code>.
     */
    public void notifySuccess(T obj);

}
