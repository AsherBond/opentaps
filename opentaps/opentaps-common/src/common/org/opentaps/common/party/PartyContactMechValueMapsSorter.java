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

package org.opentaps.common.party;

import java.util.Comparator;
import java.util.Map;

import org.ofbiz.entity.GenericValue;

/**
 * This class is just a Comparator used to sort the List returned by
 *   org.ofbiz.party.contact.ContactMechWorker getPartyContactMechValueMaps
 * 
 */
public class PartyContactMechValueMapsSorter implements Comparator {

    public int compare(Object o1, Object o2) {

        // we are sorting a list of Map
        Map map1 = (Map)o1;
        Map map2 = (Map)o2;
        
        // if the party are different, then compare the partyId 
        GenericValue partyContactMech1 = (GenericValue)map1.get("partyContactMech");
        GenericValue partyContactMech2 = (GenericValue)map2.get("partyContactMech");

        String partyId1 = partyContactMech1.getString("partyId");
        String partyId2 = partyContactMech2.getString("partyId");

        if (!partyId1.equals(partyId2)) {
            return partyId1.compareTo(partyId2);
        }

        // else get the contact mech and compare their type
        GenericValue contactMech1 = (GenericValue)map1.get("contactMech");
        GenericValue contactMech2 = (GenericValue)map2.get("contactMech");
        
        String typeId1 = contactMech1.getString("contactMechTypeId");
        String typeId2 = contactMech2.getString("contactMechTypeId");

        if (!typeId1.equals(typeId2)) {
            return typeId1.compareTo(typeId2);
        }

        // else for the same type we can sort by contact mech id
        String id1 = contactMech1.getString("contactMechId");
        String id2 = contactMech2.getString("contactMechId");

        return id1.compareTo(id2);
    }

}