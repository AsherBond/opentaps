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
package org.opentaps.domain.voip;

import org.opentaps.base.entities.ExternalUser;
import org.opentaps.base.entities.TelecomNumber;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;
/**
 * The Voip service interface used to support voip call in/out works.
 */
public interface VoipRepositoryInterface  extends RepositoryInterface {
    /**
     * Gets the ExternalUser which relate with the User.
     * @param user a <code>User</code> instance
     * @return ExternalUser instance, return null if not found
     * @throws RepositoryException if an error occurs
     */
    public ExternalUser getVoipExtensionForUser(User user) throws RepositoryException;
    
    /**
     * Gets the link for the Account that matching the current call in.
     * @param user a <code>User</code> instance
     * @return the party view url
     * @throws RepositoryException if an error occurs
     */
    public String getCallInPartyLink(User user) throws RepositoryException;

    /**
     * Make a outgoing call.
     * @param user a <code>User</code> instance
     * @param countryCode a <code>String</code> value
     * @param areaCode a <code>String</code> value
     * @param phoneNumber a <code>String</code> value
     * @throws RepositoryException if an error occurs
     */
    public void makeOutgoingCall(User user, String countryCode, String areaCode, String phoneNumber) throws RepositoryException;
    
    /**
     * Make a outgoing call.
     * @param user a <code>User</code> instance
     * @param telecomNumber a <code>TelecomNumber</code> value
     */
    public void makeOutgoingCall(User user, TelecomNumber telecomNumber) throws RepositoryException;
}
