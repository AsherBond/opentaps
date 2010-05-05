/* Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

package org.opentaps.funambol.sync;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityListIterator;

import com.funambol.common.pim.common.TypifiedProperty;
import com.funambol.common.pim.contact.Contact;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;

/**
 * ONE instance of this class should be used per sync - it is NOT thread-safe.
 *
 * @author Cameron Smith, Eurico da Silva - Database, Lda - www.database.co.mz
 *
 */
public final class EntitySyncSourceHelper {

    private static FunambolLogger LOGGER = FunambolLoggerFactory.getLogger("funambol.server");

    //variables representing other data or structures needed to perform the sync
    private static String createdStamp = "createdStamp";
    private static String lastUpdatedStamp = "lastUpdatedStamp";
    private static Map<String, String> purposeAndContactMechType = initPurposeTypeAndContactMechType();
    private static Map<String, String> purposeTypePropertyTypeRelation = initPurposeTypePropertyTypeRelation();

    private EntitySyncSourceHelper() { }

    /**
     * Get all the items from the given EntityListIterator and build up a list of the related Partys - then close the iterator.
     *
     * @param it must contain entities which have the field partyId
     * @return the list of Party entities
     * @throws GenericEntityException if any of the relations cannot be found, or are not of type ONE
     */
    public static List<GenericValue> getPartyListFromIterator(EntityListIterator it) throws GenericEntityException {
        List<GenericValue> gvs = new ArrayList<GenericValue>();

        try {
            GenericValue entity = null;
            // we cannot use getRelatedOne because the entities may be view entities without relationships
            while ((entity = it.next()) != null) {
                gvs.add(entity.getDelegator().findByPrimaryKey("Party", UtilMisc.toMap("partyId", entity.get("partyId"))));
            }
        } finally {
            it.close();
        }

        return gvs;
    }

    /**
     * Gets a subset of all[Parties|task|any other entity] which were created > since.
     * @param allEntities a List of entities to test for
     * @param since the Timestamp to check against
     * @return a subset of all[Parties|task|any other entity] which were created > since
     *
     * TODO: we should be able to calculate this once only, if since is the same for all data subsets (NEW, UPDATED, etc).
     */
    public static List<GenericValue> getNewEntities(List<GenericValue> allEntities, Timestamp since) {
        List<GenericValue> created = new LinkedList<GenericValue>();
        for (GenericValue gValue : allEntities) {
            if (gValue.getTimestamp(createdStamp).after(since)) {
                created.add(gValue);
            }
        }

        return created;
    }

    /**
     * Gets a List of Party entities that were updated since the given time.
     * Only works for Party because of subcriteria: this will test related entities like Person, ContactMech, etc ..
     * @param allParties a List of Party entities to test for
     * @param since the Timestamp to check against
     * @return a List of Party entities that were updated since the given time
     * @throws GenericEntityException if an error occurs
     */
    public static List<GenericValue> getUpdatedParties(List<GenericValue> allParties, Timestamp since) throws GenericEntityException {
        List<GenericValue> updatedParties = new LinkedList<GenericValue>();
        for (GenericValue party : allParties) {
            if (wasPartyUpdated(party, since)) {
                updatedParties.add(party);
            }
        }
        return updatedParties;
    }

    /**
     * Test if the given Party Entity was updated since the given time.
     * @param party a Party entity to test for
     * @param since the Timestamp to check against
     * @return true if the Party was updated since the given time
     * @throws GenericEntityException if an error occurs
     */
    public static boolean wasPartyUpdated(GenericValue party, Timestamp since) throws GenericEntityException {
        // 2.2.1 the party itself may have been updated (but not created) since last sync
        if (wasUpdated(party, since)) {
            return true;
        }

        // 2.2.2 failing that, the Person or PartyGroup may have been updated (e.g. surname)
        if (wasUpdated(party.getRelatedOne("Person"), since)) {
            return true;
        }
        if (wasUpdated(party.getRelatedOne("PartyGroup"), since)) {
            return true;
        }
        if (wasUpdated(party.getRelatedOne("PartySupplementalData"), since)) {
            return true;
        }

        // 2.2.3 failing that too, only a new PCM would count as an update
        return wereAnyUpdated(party.getRelated("PartyContactMech"), since);
    }

    /**
     * Gets the subset of the given list of entities that was updated since the given time.
     * @param allTasks a List of entities to test for
     * @param since the Timestamp to check against
     * @return a List of entities that were updated since the given time
     * @throws GenericEntityException if an error occurs
     */
    public static List<GenericValue> getUpdatedTasks(List<GenericValue> allTasks, Timestamp since) throws GenericEntityException {
        List<GenericValue> updatedTasks = new LinkedList<GenericValue>();
        for (GenericValue task : allTasks) {
            if (wasUpdated(task, since)) {
                updatedTasks.add(task);
            }
        }
        return updatedTasks;
    }

    /**
     * Checks if any entity in the given list of entities was updated or created since the given time.
     * Warning - this method is designed for subrecords, so also counts as new, anything which has just been created.
     * @param genericValues the list of entities to check
     * @param since the Timestamp to check against
     * @return true if any entity was updated or created since the given time
     * @throws GenericEntityException if an error occurs
     */
    private static boolean wereAnyUpdated(List<GenericValue> genericValues, Timestamp since) {
        for (GenericValue gValue : genericValues) {
            if (wasUpdatedOrCreated(gValue, since)) {
                return true;
            }
        }

        //if we got here, nothing has changed
        return false;
    }

    /**
     * Checks if the given Entity was created since the given time.
     * @param gValue the Entity to check
     * @param since the Timestamp to check against
     * @return a <code>boolean</code> value
     */
    public static boolean wasCreated(GenericValue gValue, Timestamp since) {
        if (since == null) {
            return true;
        }  //TODO: DSS gives null here on first sync, but is returning true the correct behaviour

        return gValue != null && gValue.getTimestamp(createdStamp).after(since);
    }
    /**
     * Checks if the given Entity was created or updated since the given time.
     * @param gValue the Entity to check
     * @param since the Timestamp to check against
     * @return a <code>boolean</code> value
     */

    public static boolean wasUpdatedOrCreated(GenericValue gValue, Timestamp since) {
        if (since == null) {
            return true;
        }  //TODO: DSS gives null here on first sync, but is returning true the correct behaviour

        return gValue != null
            && gValue.getTimestamp(createdStamp).after(since)
            || gValue.getTimestamp(lastUpdatedStamp).after(since);
    }

    /**
     * Checks if the given Entity was updated (but not created) since the given time.
     * @param gValue the Entity to check
     * @param since the Timestamp to check against
     * @return a <code>boolean</code> value
     */
    protected static boolean wasUpdated(GenericValue gValue, Timestamp since) {
        if (since == null) {
            return true;
        }  //TODO: DSS gives null here on first sync, but is returning true the correct behaviour

        return gValue != null
            && gValue.getTimestamp(createdStamp).before(since)
            && gValue.getTimestamp(lastUpdatedStamp).after(since);
    }

    /**
     * Perpares the input Map for the create relationship service from a given Contact.
     * Copies the contact firstName / lastName and either one of groupName or companyName.
     * @param contact a <code>Contact</code> value
     * @param params the Map of parameters to preate
     */
    public static void prepateServiceCreateRelationshipMap(Contact contact, Map<String, String> params) {
        boolean isContact = ContactUtils.isContact(contact);
        boolean isLead = ContactUtils.isLead(contact);
        boolean isAccount = ContactUtils.isAccount(contact);

        if (isContact || isLead) {
            params.put("firstName", contact.getName().getFirstName().getPropertyValueAsString());
            params.put("lastName", contact.getName().getLastName().getPropertyValueAsString());
        }

        if (isContact) {
            String firstName = contact.getName().getFirstName().getPropertyValueAsString();
            String lastName = contact.getName().getLastName().getPropertyValueAsString();
            if (UtilValidate.isEmpty(lastName)) {
                lastName = contact.getName().getDisplayName().getPropertyValueAsString();
            }
            if (UtilValidate.isEmpty(lastName) && UtilValidate.isNotEmpty(firstName)) {
                lastName = "Unknown";
            }
            params.put("firstName", firstName);
            params.put("lastName", lastName);
        }

        if (isAccount || isLead) {
            params.put(isAccount ? "groupName" : "companyName", contact.getBusinessDetail().getCompany().getPropertyValueAsString());
        }
    }

    /**
     * Return the given String, normalized for ease of comparison.
     * Currently the normalization comprises the following:
     *  <ol>
     *   <li>Trim leading and trailing whitespace</li>
     *   <li>Uppercase all letters</li>
     *   <li>Remove all non alphanumeric characters</li>
     *  </ol>
     * @param string a <code>String</code> value to normalize
     * @return the normalized <code>String</code> value
     */
    public static String normalize(String string) {
        return normalize(string, "[^0-9A-Z]");
    }

    /**
     * Return the given String, normalized for ease of comparison.
     * Currently the normalization comprises the following:
     *  <ol>
     *   <li>Trim leading and trailing whitespace</li>
     *   <li>Uppercase all letters</li>
     *   <li>Remove all characters which match<code>removeChars</code></li>
     *  </ol>
     * @param string a <code>String</code> value to normalize
     * @param removeChars chars to remove
     * @return the normalized <code>String</code> value
     */
    public static String normalize(String string, String removeChars) {
        if (string == null) {
            return null;
        }   // guard against bad parameters
        return StringUtils.trim(string).toUpperCase().replaceAll(removeChars, "");
    }

    /**
     * Gets the contact mech type from a given purpose.
     * eg: purpose "PHONE_MOBILE", returns the type "TELECOM_NUMBER".
     * @param purpose the contact mech purpose to lookup
     * @return the corresponding contact mech type
     */
    public static String getPurposeAndContactMechType(String purpose) {
        return purposeAndContactMechType.get(purpose);
    }

    public static String getPropertyFromPurpose(String purpose, List<TypifiedProperty> properties) {
        if (properties != null) {
            for (TypifiedProperty prop : properties) {
                if (purposeTypePropertyTypeRelation.get(purpose).equals(prop.getPropertyType())) {
                    return prop.getPropertyValueAsString();
                }
            }
        } else {
            LOGGER.warn("contact email list is null");
        }
        return "";
    }

    private static Map<String, Set<String>> initContactMechTypeAndPurposeType() {
        Map<String, Set<String>> pm = new HashMap<String, Set<String>>();

        Set<String> purposes = new HashSet<String>();
        purposes.add("PRIMARY_EMAIL");
        purposes.add("OTHER_EMAIL");
        purposes.add("OTHER_EMAIL_SEC");
        pm.put("EMAIL_ADDRESS", new HashSet<String>(purposes));
        purposes.clear();

        purposes.add("WEB_ADDRESS");
        pm.put("PRIMARY_WEB_URL", new HashSet<String>(purposes));
        purposes.clear();

        purposes.add("PRIMARY_PHONE");
        purposes.add("PHONE_MOBILE");
        purposes.add("PHONE_CAR");
        purposes.add("PHONE_WORK");
        purposes.add("PHONE_HOME");
        purposes.add("PHONE_HOME_SEC");
        purposes.add("FAX_NUMBER");
        purposes.add("FAX_NUMBER_SEC");
        pm.put("TELECOM_NUMBER", new HashSet<String>(purposes));
        purposes.clear();

        purposes.add("GENERAL_LOCATION");
        purposes.add("HOME_LOCATION");
        purposes.add("OTHER_LOCATION");
        pm.put("POSTAL_ADDRESS", new HashSet<String>(purposes));
        purposes.clear();
        return pm;
    }

    private static Map<String, String> initPurposeTypeAndContactMechType() {
        Map<String, String> pm = new HashMap<String, String>();

        pm.put("PRIMARY_EMAIL", "EMAIL_ADDRESS");
        pm.put("OTHER_EMAIL", "EMAIL_ADDRESS");
        pm.put("OTHER_EMAIL_SEC", "EMAIL_ADDRESS");

        pm.put("PRIMARY_WEB_URL", "WEB_ADDRESS");

        pm.put("PRIMARY_PHONE", "TELECOM_NUMBER");
        pm.put("PHONE_MOBILE", "TELECOM_NUMBER");
        pm.put("PHONE_CAR", "TELECOM_NUMBER");
        pm.put("PHONE_WORK", "TELECOM_NUMBER");
        pm.put("PHONE_HOME", "TELECOM_NUMBER");
        pm.put("PHONE_HOME_SEC", "TELECOM_NUMBER");
        pm.put("FAX_NUMBER", "TELECOM_NUMBER");
        pm.put("FAX_NUMBER_SEC", "TELECOM_NUMBER");

        pm.put("GENERAL_LOCATION", "POSTAL_ADDRESS");
        pm.put("HOME_LOCATION", "POSTAL_ADDRESS");
        pm.put("OTHER_LOCATION", "POSTAL_ADDRESS");

        return pm;
    }

    private static Map<String, String> initPurposeTypePropertyTypeRelation() {
        Map<String, String> pm = new HashMap<String, String>();
        pm.put("PRIMARY_EMAIL", "Email1Address");
        pm.put("OTHER_EMAIL", "OtherEmail2Address");
        //pm.put("OTHER_EMAIL_SEC", "Email3Address");
        pm.put("PRIMARY_WEB_URL", "WebPage");
        return pm;
    }
}
