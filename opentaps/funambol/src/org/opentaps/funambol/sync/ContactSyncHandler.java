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


package org.opentaps.funambol.sync;

import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mz.co.dbl.siga.framework.base.NotConfiguredException;
import mz.co.dbl.siga.framework.entity.EntityBeanConverter;
import mz.co.dbl.siga.framework.entity.EntityPreparer;
import mz.co.dbl.siga.framework.entity.MapBeanConverter;

import org.apache.commons.lang.StringUtils;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.GenericServiceException;

import com.funambol.common.pim.common.Property;
import com.funambol.common.pim.common.TypifiedProperty;
import com.funambol.common.pim.contact.Address;
import com.funambol.common.pim.contact.Contact;
import com.funambol.common.pim.contact.Email;
import com.funambol.common.pim.contact.Name;
import com.funambol.common.pim.contact.Phone;
import com.funambol.common.pim.contact.WebPage;
import com.funambol.common.pim.converter.ContactToVcard;
import com.funambol.common.pim.converter.ConverterException;
import com.funambol.common.pim.vcard.ParseException;
import com.funambol.common.pim.vcard.VcardParser;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.source.SyncSource;
import com.funambol.framework.engine.source.SyncSourceException;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;
import com.opensourcestrategies.crmsfa.party.PartyHelper;

/**
 * Knows how to perform sync for an OT Contact/Lead/Account, represented remotely as an instance of com.funambol.common.pim.contact.Contact.
 * @author cameron
 */
public class ContactSyncHandler extends AbstractSyncHandler<Contact> implements EntitySyncHandler<Contact> {
    private static FunambolLogger log = FunambolLoggerFactory.getLogger("funambol.server");

    //=== configuration info, should eventually be loaded from spring ===

    private Map<String, ServiceMapping> _deleteServices = new HashMap<String, ServiceMapping>();  //roleTypeId:serviceName
    private Map<String, ServiceMapping> _updateServices = new HashMap<String, ServiceMapping>();  //roleTypeId:serviceName
    private Map<String, String> _busPhoneTypes = new HashMap<String, String>();  //contactMechPurposeType:Funambol Phone Purpose
    private Map<String, String> _persPhoneTypes = new HashMap<String, String>();  //contactMechPurposeType:Funambol Phone Purpose
    private Map<String, String> _emailTypes = new HashMap<String, String>();  //contactMechPurposeType:Funambol Email Purpose

    private String _defaultCountryGeoId;  //country ID to use if Outlook country is not recognized or empty
    private String _defaultStateGeoId_US;  //state ID to use if Outlook country is US and state is not recognized or empty
    private String _emptyFieldString = "N/A";  //what string should we set in a field which cannot be blank in OT, but Outlook has no value for it?

    //=== sync-specific data ===

    private List<GenericValue> _allParties;  //all A/C/L accessible to syncing user - for efficiency we load this list only once
    private Set<String> _newKeys;  //new partyIds - we need them to separate from updateds

    //=== initialization of unchanging data ===

    public ContactSyncHandler() {
        //TODO: the setup below could be static for efficiency

        //prep delete services
        _deleteServices.put("ACCOUNT", new ServiceMapping("crmsfa.deactivateAccount", "partyId"));
        _deleteServices.put("CONTACT", new ServiceMapping("crmsfa.deactivateContact", "partyId"));
        _deleteServices.put("PROSPECT", new ServiceMapping("crmsfa.deleteLead", "leadPartyId"));

        //prep update services
        _updateServices.put("ACCOUNT", new ServiceMapping("crmsfa.updateAccount", "partyId"));
        _updateServices.put("CONTACT", new ServiceMapping("crmsfa.updateContact", "partyId"));
        _updateServices.put("PROSPECT", new ServiceMapping("crmsfa.updateLead", "partyId"));

         //TODO: should this go in this map or somewhere else?
        ServiceMapping contactMechService = new ServiceMapping("updatePartyContactMech", "contactMechId");
        _updateServices.put("EMAIL_ADDRESS", contactMechService);
        _updateServices.put("WEB_ADDRESS", contactMechService);
        _updateServices.put("TELECOM_NUMBER", new ServiceMapping("updatePartyTelecomNumber", "contactMechId", "TelecomNumber"));
        _updateServices.put("POSTAL_ADDRESS", new ServiceMapping("updatePartyPostalAddress", "contactMechId", "PostalAddress"));

        //prep phone mappings
        _busPhoneTypes.put("PRIMARY_PHONE", "BusinessTelephoneNumber");
        _busPhoneTypes.put("FAX_NUMBER", "BusinessFaxNumber");
        _persPhoneTypes.put("PHONE_HOME", "HomeTelephoneNumber");
        _persPhoneTypes.put("PHONE_MOBILE", "MobileTelephoneNumber");

        //prep email mappings
        _emailTypes.put("PRIMARY_EMAIL", "Email1Address");
        _emailTypes.put("OTHER_EMAIL", "OtherEmail2Address");
        _emailTypes.put("OTHER_EMAIL_SEC", "OtherEmail3Address");

        mechPurposeAndContactFieldType = initMechPurposeContactFieldType();
    }

    //=== initialization via IOC ===

    private Map<String, String> mechPurposeAndContactFieldType=null;

    /**
     * Default (if this method is not called): server_wins.
     */
    public void setMergeStrategy(String strategy) {
        try {
            _mergeStrategy = Enum.valueOf(MergeStrategy.class, strategy);
        } catch (IllegalArgumentException noSuchStrategyX) {
            String message = "Merge Strategy setting not recognized: [" + strategy + "]. Must be one of {" + StringUtils.join(MergeStrategy.values(), ",") + "}";
            log.fatal(message);
            throw new NotConfiguredException(message, noSuchStrategyX);
        }
    }

    //  === implement EntitySyncHandler ===

    /**
     *
     * @param a instance of <code>Contact</code>
     * @return The partyId that will be used as SyncItemKey
     * @throws GeneralException if an error occurs
     */
    public String addItem(Contact source) throws GeneralException {
    	addressCompatibilization(source);

        //2.2 create the Party representing new client
        Map relParams = new HashMap();

        //TODO: Cameron, should this method return the map? or it's good void!
        EntitySyncSourceHelper.prepateServiceCreateRelationshipMap(source, relParams);
        Map contactInfo = _syncSource.runSync("crmsfa.create" + ContactUtils.contactType(source), relParams);
        String partyId = (String) contactInfo.get("partyId");
        GenericValue contactParty = _syncSource._delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
        log.warn("created contactParty " + contactParty);

        //2.2 create the ContactMech and related details representing tel number
        insertContactMech(partyId,source, "PRIMARY_PHONE");  //TODO: this mapping needs to be more intelligent
        insertContactMech(partyId,source, "PHONE_HOME");
        insertContactMech(partyId,source, "FAX_NUMBER");
        insertContactMech(partyId,source, "PHONE_MOBILE");

        updateOrInsertPostalAddress(contactParty,source, "GENERAL_LOCATION");
        updateOrInsertPostalAddress(contactParty,source, "HOME_LOCATION");
        updateOrInsertPostalAddress(contactParty,source, "OTHER_LOCATION");

        insertContactMech(partyId,source, "PRIMARY_EMAIL");
        insertContactMech(partyId,source, "OTHER_EMAIL");
        insertContactMech(partyId,source, "OTHER_EMAIL_SEC");

        insertContactMech(partyId,source, "PRIMARY_WEB_URL");

        return partyId;
    }

    public Contact getItemFromId(String key) throws GeneralException {
        log.info("Will look for this partyId: " + key);
        GenericValue contactPartyGV = _syncSource.findByPrimaryKey("Party", UtilMisc.toMap("partyId", key));
        EntityPreparer contactParty = new EntityPreparer(contactPartyGV);
        Contact contact = new Contact();   //TODO: use EBConverter in this direction too, but first need a bean preparer

        //2. basic data
        partyBasicDetailsToContact(contactParty, contact);

        //3. now deal with the contact numbers for this party
        partyPhoneDetailsToContact(contactPartyGV, contact);

        //address
        partyAddressDetailsToContact(contactPartyGV, contact);

        //email
        prepareContactTypifiedProperty(getActivePartyContactMechPurpose(contactPartyGV, "PRIMARY_EMAIL"), contact, "Email1Address");
        prepareContactTypifiedProperty(getActivePartyContactMechPurpose(contactPartyGV, "OTHER_EMAIL"), contact, "OtherEmail2Address");
        prepareContactTypifiedProperty(getActivePartyContactMechPurpose(contactPartyGV, "OTHER_EMAIL_SEC"), contact, "OtherEmail3Address");

        //web page
        prepareContactTypifiedProperty(getActivePartyContactMechPurpose(contactPartyGV, "PRIMARY_WEB_URL"), contact, "WebPage");

        return contact;
    }

    public Iterable<String> getAllKeys() {
        Set<String> keys = new HashSet<String>();
        for (GenericValue party : _allParties) {
            keys.add(party.getString("partyId"));
        }

        return keys;
    }

    public Iterable<String> getNewKeys(Timestamp since) {
        for (GenericValue party : EntitySyncSourceHelper.getNewEntities(_allParties, since)) {
            _newKeys.add(party.getString("partyId"));
        }

        _since = since;  //this line is essential for merge to work, otherwise it has no basis for comparison of times

        return _newKeys;
    }

    public Iterable<String> getUpdatedKeys(Timestamp since) throws GenericEntityException {
        Set<String> keys = new HashSet<String>();
        for (GenericValue party : EntitySyncSourceHelper.getUpdatedParties(_allParties, since)) {
            keys.add(party.getString("partyId"));
        }

        //because of the way contactmechs are updated in OFBiz, new parties can sometimes be reported as updated so we have to filter them out
        keys.removeAll(_newKeys);  //TODO: can we guarantee that getUpdated always called after getNew

        return keys;
    }

    /**
     * @return Iterable<partyId> of the Account/Contact/Leads which have been deleted in OT
     */
    public Iterable<String> getDeletedKeys(Timestamp since) throws GenericEntityException {
        Set<String> deletedKeys = new HashSet<String>();  //use a set in case any Id comes up twice
        for(GenericValue deleted : _syncSource.findByAnd("PartyDeactivation", EntityCondition.makeCondition("deactivationTimestamp", EntityOperator.GREATER_THAN, since))) {
            deletedKeys.add(deleted.getString("partyId"));
        }

        return deletedKeys;
    }

    /**
     * TODO: this method will be quite SLOW so far, as ALL existing contacts much be searched TWICE for each potential add
     */
    public Iterable<String> getKeysFromTwin(Contact twin) throws GeneralException {
        Collection<String> twinKeys = new HashSet<String>();  //use set to avoid duplicates

        //1.1 try looking by person's full name
        String fullName = twin.getName().getFirstName().getPropertyValueAsString() + twin.getName().getLastName().getPropertyValueAsString();

        //1.2 normalize the name
        fullName = EntitySyncSourceHelper.normalize(fullName, _normalizeRemoveRegex);

        //1.3 now perform the search
        if (!StringUtils.isBlank(fullName)) { //if we have a name, it could be an CONTACT or LEAD
            for (GenericValue candidate : _allParties) {
                String roleType = findFirstRoleForParty(candidate.getString("partyId"));
                if ("CONTACT".equals(roleType) || "PROSPECT".equals(roleType)) {
                    GenericValue person = candidate.getRelatedOne("Person");
                    String candidateFullName = EntitySyncSourceHelper.normalize(person.getString("firstName") + person.getString("lastName"), _normalizeRemoveRegex);
                    if (fullName.equals(candidateFullName)) {
                        twinKeys.add(candidate.getString("partyId"));
                    }
                }
            }
        } else { //2. failing that, look by company - in this case only ACCOUNTS are interesting
            String groupName = EntitySyncSourceHelper.normalize(twin.getBusinessDetail().getCompany().getPropertyValueAsString(), _normalizeRemoveRegex);

            for (GenericValue candidate : _allParties) {
                String roleType = findFirstRoleForParty(candidate.getString("partyId"));
                if ("ACCOUNT".equals(roleType)) {
                    GenericValue group = candidate.getRelatedOne("PartyGroup");
                    String candidateGroupName = EntitySyncSourceHelper.normalize(group.getString("groupName"), _normalizeRemoveRegex);
                    if ((groupName != null) && groupName.equals(candidateGroupName)) {
                        twinKeys.add(candidate.getString("partyId"));
                    }
                }
            }
        }
        return twinKeys;
    }

    /**
     * @see SyncSource.removeSyncItem
     */
    public void removeItem(String partyId) throws GeneralException {
        //1. security: are we actually allowed to delete this guy - crmsfa services should do this automatically

        //2. call the appropriate service for deletion, based on first role which matches
        for (GenericValue role : findRolesForParty(partyId)) { //unfortunately Java does not have a lambda/block mechanism!!
            ServiceMapping service = _deleteServices.get(role.getString("roleTypeId"));
            if (service != null) {
                _syncSource.runSync(service.getServiceName(), service.paramsMap(partyId));
                log.info("REMOVE " + partyId + " via " + service);
                return;  //nothing more to do
            }
        }

        //3. if we got to here there is something wrong
        throw new GenericServiceException("Could not find any service to REMOVE party " + partyId);
    }

    public void updateItem(String partyId, Contact contact) throws GenericEntityException, GenericServiceException {
        //0. swap address strings with geoIds
        addressCompatibilization(contact);

        //1. Update Party + Person/PartyGroup with the appropriate service
        GenericValue contactGV = _syncSource.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));

        //2. call the appropriate converter + update service, based on first role which matches
        String roleType = "";
        for (GenericValue role : findRolesForParty(partyId)) { //unfortunately Java does not have a lambda/block mechanism!!
            if (role.getString("roleTypeId").equals("ACCOUNT")
                || role.getString("roleTypeId").equals("CONTACT")
                || role.getString("roleTypeId").equals("PROSPECT")) {
                roleType = role.getString("roleTypeId");
                break;
            }
        }
        if (UtilValidate.isEmpty(roleType)) {
            throw new GenericServiceException("Could not find any service to UPDATE party " + partyId);
        }

        //2.0 update SupplementalData
        if ("CONTACT".equals(roleType)) {
            GenericValue pSupp = contactGV.getRelatedOne("PartySupplementalData");
            _converters.toEntityAndStore(contact, pSupp);
        }

        ServiceMapping service = _updateServices.get(roleType);
        if (service != null) {
            //2.1 convert bean data to service params
            Map serviceParams = service.paramsMap(partyId);  //set PK param
            MapBeanConverter converter = _converters.getConverter("Party", contact, roleType);
            converter.toMap(contact, serviceParams);  //set other params

            _syncSource.runSync(service.getServiceName(), serviceParams);  //2.2 actually call the service
            log.info("UPDATE " + partyId + " via " + service);
        }

        //3 postal address
        updateOrInsertPostalAddress(contactGV, contact, "GENERAL_LOCATION");
        updateOrInsertPostalAddress(contactGV, contact, "HOME_LOCATION");
        updateOrInsertPostalAddress(contactGV, contact, "OTHER_LOCATION");

        //4. emails
        updateNorInsertEntity(contactGV, contact, "PRIMARY_EMAIL");
        updateNorInsertEntity(contactGV, contact, "OTHER_EMAIL");
        updateNorInsertEntity(contactGV, contact, "OTHER_EMAIL_SEC");

        //5. update TelecomNumber
        updateNorInsertEntity(contactGV, contact, "PRIMARY_PHONE");
        updateNorInsertEntity(contactGV, contact, "FAX_NUMBER");
        updateNorInsertEntity(contactGV, contact, "PHONE_HOME");
        updateNorInsertEntity(contactGV, contact, "PHONE_MOBILE");

        //6. Web page
        updateOrInsertWebPage(contactGV, contact);
    }

    //TODO: it actually seems like return true, is enough to cause DSS to recall getItemFromId so as to update remote contact: would simplify implementation of this method
    //TODO: need to detect when remote subrecord is EMPTY, in that case we need to keep our own
    public boolean mergeItem(String partyId, Contact source) throws GeneralException {
        boolean sourceChanged = false;  //did we in fact make any changes to the source bean?

        //0.1 swap address strings with geoIds
        addressCompatibilization(source);

        //0.2  get the related entity
        GenericValue mergeParty = _syncSource.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));

        //1.1 top-level record
        if (serverWinsOnMerge() && EntitySyncSourceHelper.wasPartyUpdated(mergeParty, _since)) {
            partyBasicDetailsToContact(new EntityPreparer(mergeParty), source);
            sourceChanged = true;  //we have changed at least one piece of data
        } else { //client wins, or we assume it does as nothing on server has changed
            for (GenericValue role : findRolesForParty(partyId)) { //unfortunately Java does not have a lambda/block mechanism!!
                String roleType = role.getString("roleTypeId");
                ServiceMapping service = _updateServices.get(roleType);
                if (service != null) {
                    //2.1 convert bean data to service params
                    Map serviceParams = service.paramsMap(partyId);  //set PK param
                    MapBeanConverter converter = _converters.getConverter("Party", source, roleType);
                    converter.toMap(source, serviceParams);  //set other params

                    _syncSource.runSync(service.getServiceName(), serviceParams);  //2.2 actually call the service
                    log.info("MERGE " + partyId + " via " + service);
                    break;  //nothing more to do for top-level record
                }
            }
        }

        //2. telecom numbers TODO: perhaps we only need to call this once now?
        sourceChanged = sourceChanged | mergePhonesOrEmails(mergeParty, source, source.getBusinessDetail().getPhones(), _busPhoneTypes, _since);
        sourceChanged = sourceChanged | mergePhonesOrEmails(mergeParty, source, source.getPersonalDetail().getPhones(), _persPhoneTypes, _since);

        //3. postal address
        sourceChanged = sourceChanged | mergeAddress(mergeParty, source, "GENERAL_LOCATION");
        sourceChanged = sourceChanged | mergeAddress(mergeParty, source, "HOME_LOCATION");
        sourceChanged = sourceChanged | mergeAddress(mergeParty, source, "GENERAL_LOCATION");

        //4. emails
        sourceChanged = sourceChanged | mergePhonesOrEmails(mergeParty, source, source.getPersonalDetail().getEmails(), _emailTypes, _since);

        //5. web address
        sourceChanged = sourceChanged | mergeWebAddress(mergeParty, source, _since);

        //finally inform our caller of what happened
        return sourceChanged;
    }

    //=== override superclass ===

    protected void prepareHandler() throws GeneralException {
        _allParties = prepareAllParties();
        _newKeys = new HashSet<String>();  //always clear

        //detect default values for missing data
        _defaultCountryGeoId = UtilProperties.getPropertyValue("opentaps", "defaultCountryGeoId");
        _defaultStateGeoId_US = UtilProperties.getPropertyValue("opentaps", "state.geo.id.default.us");
    }

    //=== private behaviour ===

    private void insertContactMech(String partyId, Contact contact, String purpose) throws GenericEntityException {
        if (isContactFieldEmpty(contact, purpose)) {
            return;
        }

        String contactMechType = EntitySyncSourceHelper.getPurposeAndContactMechType(purpose);

        //create the basic ContactMech which holds everything together
        String contactMechId = _syncSource._delegator.getNextSeqId("ContactMech");
        Map<String, String> createCMParams = UtilMisc.toMap("contactMechId", contactMechId, "contactMechTypeId", contactMechType);

        if (contactMechType.equals("WEB_ADDRESS")) {
            createCMParams.put("infoString",
                               EntitySyncSourceHelper.getPropertyFromPurpose(purpose, contact.getPersonalDetail().getWebPages()));
        }
        if (contactMechType.equals("EMAIL_ADDRESS")) {
            createCMParams.put("infoString",
                               EntitySyncSourceHelper.getPropertyFromPurpose(purpose, contact.getPersonalDetail().getEmails()));
        }
        GenericValue contactMech = _syncSource._delegator.create("ContactMech", createCMParams);

        //create the PartyContactMech which relates person to mech
        _syncSource._delegator.create("PartyContactMech",
                UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId, "fromDate", _syncSource.now()));

        //finally the purpose
        _syncSource._delegator.create("PartyContactMechPurpose",
                UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", purpose, "fromDate", _syncSource.now()));
        log.info("created PartyContactMech");

        EntityPreparer contactPreparer = new EntityPreparer(contactMech, true);
        if (contactMechType.equals("TELECOM_NUMBER")) {
            _converters.toEntityAndStore(contact, contactPreparer.getRelated("TelecomNumber"), purpose);
        } else if (contactMechType.equals("POSTAL_ADDRESS")) {
            _converters.toEntityAndStore(contact, contactPreparer.getRelated("PostalAddress"), purpose);
        } else if (contactMechType.equals("WEB_ADDRESS")) {
            //To not fall in Xception
        } else if (contactMechType.equals("EMAIL_ADDRESS")) {
            //To not fall in Xception
        } else {
            throw new IllegalArgumentException("ContactMechType " + contactMechType + " not implemented.");
        }
    }

    private boolean mergeAddress(GenericValue mergeParty, Contact source, String purpose) throws GenericEntityException, GenericServiceException
    {
        GenericValue addressPurpose = getActivePartyContactMechPurpose(mergeParty, purpose); //if address was updated then PartyContactMech must have been
        if (serverWinsOnMerge() && EntitySyncSourceHelper.wasUpdated(addressPurpose, _since)) {
            partyAddressDetailsToContact(mergeParty, source);
            return true;
        } else { //update address on server
            updateOrInsertPostalAddress(mergeParty, source, purpose);
            return false;
        }
    }

    /**
     * Merge the web address between the given server and client records.
     *
     * @return true if at least one phone was changed
     */
    private boolean mergeWebAddress(GenericValue mergeParty, Contact source, Timestamp since) throws GenericEntityException, GenericServiceException {
        //if the contact was updated in any way then PartyContactMechPurpose must have been
        GenericValue contactPurpose = getActivePartyContactMechPurpose(mergeParty, "PRIMARY_WEB_URL");
        if (serverWinsOnMerge() && EntitySyncSourceHelper.wasUpdated(contactPurpose, since)) {
            prepareContactTypifiedProperty(getActivePartyContactMechPurpose(mergeParty, "PRIMARY_WEB_URL"), source, "WebPage");
            return true;
        } else {//client wins, update the server
            updateOrInsertWebPage(mergeParty, source);
            return false;
        }
    }

    /**
     * Merge all telephone numbers/email addresses from the given list, according to the given mappings.
     *
     * @return true if at least one phone was changed
     */
    private boolean mergePhonesOrEmails(GenericValue mergeParty, Contact source, List<TypifiedProperty> records, Map<String, String> phoneTypes, Timestamp since)
        throws GenericEntityException, GenericServiceException {
        boolean recordChanged = false;
        if (records.size() == 0) {
            return recordChanged;
        }

        for (String purposeType : phoneTypes.keySet()) {
            //if the contact was updated in any way then PartyContactMechPurpose must have been
            GenericValue contactPurpose = getActivePartyContactMechPurpose(mergeParty, purposeType);
            String phoneType = phoneTypes.get(purposeType);
            if (serverWinsOnMerge() && contactPurpose != null && EntitySyncSourceHelper.wasUpdatedOrCreated(contactPurpose, since)) {
                if (records.get(0) instanceof Phone) { //call the appropriate update for phone or email
                    buildPhone(records, mergeParty, purposeType, phoneType);
                } else { //must be email TODO: is this a safe assumption to make?
                    prepareContactTypifiedProperty(contactPurpose, source, phoneType);
                }
                recordChanged = true;
            } else {//client wins, update the server
                updatePhoneOrEmailDetailsFromContact(mergeParty, source, records, purposeType, phoneType);
            }
        }

        return recordChanged;
    }

    /**
     * Prepare _allParties only if it has not been prepared
     *
     * We do this only once for efficiency,
     *  TODO: do it in constructor? beginSync?
     *  TODO: put tx mgt where?
     */
    private List<GenericValue> prepareAllParties() throws GeneralException {
        List<GenericValue> allParties = new ArrayList<GenericValue>();

        allParties.addAll(EntitySyncSourceHelper.getPartyListFromIterator(PartyHelper.findActiveAccounts(_syncSource._delegator, _syncSource._dispatcher, _syncPartyId)));
        log.info(allParties.size() + " accounts");
        allParties.addAll(EntitySyncSourceHelper.getPartyListFromIterator(PartyHelper.findActiveLeads(_syncSource._delegator, _syncSource._dispatcher, _syncPartyId)));
        log.info(allParties.size() + " + leads");
        allParties.addAll(EntitySyncSourceHelper.getPartyListFromIterator(PartyHelper.findActiveContacts(_syncSource._delegator, _syncSource._dispatcher, _syncPartyId)));
        log.info(allParties.size() + " + contacts");

        return allParties;
    }

    /**
     * Find all the PartyRole for the given party.  Useful for deciding if we have an Account/Contact/Lead
     */
    private List<GenericValue> findRolesForParty(String partyId) throws GenericEntityException {
        return _syncSource.findByAnd("PartyRole", UtilMisc.toMap("partyId", partyId));
    }

    /**
     * Return the first ACCOUNT, CONTACT or PROSPECT role associated with the given party, or fail.
     *
     * @return roleTypeId detected
     * @throws GenericEntityException if NONE of the above roles are detected
     */
    private String findFirstRoleForParty(String partyId) throws GenericEntityException {
        for (GenericValue role : findRolesForParty(partyId)) {
            String roleTypeId = role.getString("roleTypeId");
            if ("ACCOUNT".equals(roleTypeId) || "CONTACT".equals(roleTypeId) || "PROSPECT".equals(roleTypeId)) {
                return roleTypeId;
            }
        }

        //if we got to here, then nothing was found so bail out!
        throw new GenericEntityException("This party does not have a supported PartyRole: " + partyId);
    }

    private void partyPhoneDetailsToContact(GenericValue contactPartyGV, Contact contact) throws GenericEntityException {
        partyPhoneDetailsToContact(contactPartyGV, contact.getBusinessDetail().getPhones(), _busPhoneTypes);
        partyPhoneDetailsToContact(contactPartyGV, contact.getPersonalDetail().getPhones(), _persPhoneTypes);
    }

    /**
     * Look for all supported address records owned by contactPartyGV and set them into their correct place within the given contact's object hierarchy.
     */
    private void partyAddressDetailsToContact(GenericValue contactPartyGV, Contact contact) throws GenericEntityException {

        //go through all supported PostalAddress:Contact converters and see if we have something in OT for each one
        for (EntityBeanConverter addressConverter : _converters.listConverters("PostalAddress", contact)) {
            String purposeType = addressConverter.getQualifier();
            Address address = ContactUtils.getAddressForPurpose(contact, purposeType);
            String city = "", country = "", postal = "", street = "", street2 = "", state = "";  //default values - cause address to be emptied

            GenericValue purpose = getActivePartyContactMechPurpose(contactPartyGV, purposeType); //according to spec should be general corresp. address
            if (purpose != null) { //override default empty values with actual data
                GenericValue postalAddress = purpose.getRelatedOne("PostalAddress");
                if (postalAddress != null) {
                    GenericValue countryGeo = postalAddress.getRelatedOne("CountryGeo");

                    city = postalAddress.getString("city");
                    country = countryGeo == null ? "" : countryGeo.getString("geoName");
                    postal = postalAddress.getString("postalCode");
                    street2 = postalAddress.getString("address2");
                    street = postalAddress.getString("address1");
                    state = postalAddress.getRelatedOne("StateProvinceGeo") != null ? postalAddress.getRelatedOne("StateProvinceGeo").getString("geoName"):"";

                    if (!StringUtils.isEmpty(street2)) {
                        street += "\r\n" + street2;
                    }
                }
            }

            //finally set the data into the address
            //TODO: this should be done via a converter
            address.getCity().setPropertyValue(city);
            address.getCountry().setPropertyValue(country);
            address.getPostalCode().setPropertyValue(postal);
            address.getStreet().setPropertyValue(street);
            address.getState().setPropertyValue(state);
        }
    }

    private void updateOrInsertWebPage(GenericValue contactGV, Contact contact) throws GenericEntityException, GenericServiceException {
        ServiceMapping service = _updateServices.get(EntitySyncSourceHelper.getPurposeAndContactMechType("PRIMARY_WEB_URL"));
        GenericValue purpose = getActivePartyContactMechPurpose(contactGV, "PRIMARY_WEB_URL");
        if (purpose != null) { //we actually have something to update!
            GenericValue mech = purpose.getRelatedOne("ContactMech");
            Map serviceParams = service.paramsMap(mech.getString("contactMechId"));  //set PK param
            serviceParams.put("contactMechTypeId", mech.getString("contactMechTypeId"));
            serviceParams.put("partyId", purpose.getString("partyId"));
            _converters.getConverter("PRIMARY_WEB_URL", contact).toMap(contact, serviceParams);  //set other params

            _syncSource.runSync(service.getServiceName(), serviceParams);  //actually call the service
        } else {
            insertContactMech(contactGV.getString("partyId"), contact, "PRIMARY_WEB_URL");
        }
    }

    private void updatePhoneOrEmailDetailsFromContact(GenericValue contactGV, Contact contact, List<TypifiedProperty> records, String phonePurpose, String phoneType)
        throws GenericEntityException, GenericServiceException {
        updateNorInsertEntity(contactGV, contact, phonePurpose);
    }

    /**
     * Copy all the phone types from the given GV to the given set of phones, according to the given mapping set.
     */
    private void partyPhoneDetailsToContact(GenericValue contactPartyGV, List phones, Map<String, String> phoneTypes) throws GenericEntityException {
        for (String purposeType : phoneTypes.keySet()) {
            buildPhone(phones, contactPartyGV, purposeType, phoneTypes.get(purposeType));
        }
    }

    private void prepareContactTypifiedProperty(GenericValue purpose, Contact contact, String type) throws GenericEntityException {
        boolean isEmail = !"WebPage".equals(type);  //TODO: could not use UtilValidate.isEmail

        //prepare info
        String infoString = "";  //by default we have nothing - will cause the remote record to be "deleted"
        if (purpose != null) { //we actually have something to update
            String infoStr = ((GenericValue) purpose.getRelatedOne("ContactMech")).getString("infoString");
            if (infoStr != null) {
                infoString = infoStr;
            }
        }

        TypifiedProperty tp = isEmail ? new Email() : new WebPage();
        tp.setPropertyType(type);
        tp.setPropertyValue(infoString);

        if (isEmail) {
            contact.getPersonalDetail().getEmails().add(tp);
        } else {
            contact.getPersonalDetail().getWebPages().add(tp);
        }
    }

    /**
     * Copy all the basic details for the given Account/Contact/Lead to the given contact.
     */
    private void partyBasicDetailsToContact(EntityPreparer contactParty, Contact contact) throws GenericEntityException {
        //1. detect role
        String roleType = findFirstRoleForParty((String) contactParty.get("partyId"));

        //2. set data based on role
        if ("CONTACT".equals(roleType) || "PROSPECT".equals(roleType)) {
            //there must be a related person
            GenericValue person = contactParty.getRelated("Person");

            //2.1 convert Entity -> Contact
            Name name = new Name();
            name.getFirstName().setPropertyValue(person.get("firstName"));
            name.getLastName().setPropertyValue(person.get("lastName"));
            contact.setName(name);

            GenericValue supplData = contactParty.getRelated("PartySupplementalData");
            if (supplData != null) {
                String company = supplData.getString("companyName");
                company = company == null ? "" : company;
                contact.getBusinessDetail().getCompany().setPropertyValue(company);
            }

        } else if ("ACCOUNT".equals(roleType)) { //must be an ACCOUNT
            GenericValue company = contactParty.getRelated("PartyGroup");
            contact.getBusinessDetail().getCompany().setPropertyValue(company.getString("groupName"));
        } else {
            throw new GenericEntityException("Trying to sync party with unknown role type:" + roleType);
        }
    }

    /**
     *  Insert (or updated if already exists) a PostalAddress for given contactParty and contactMechPurposeType.
     *
     * @param contactParty
     * @param contact
     * @param purpose - A ContactMechPurposeType value
     * @throws GenericEntityException
     * @throws SyncSourceException
     */
    private void updateOrInsertPostalAddress(GenericValue contactParty, Contact contact, String purpose) throws GenericEntityException, GenericServiceException {
        GenericValue mech = null;
        Address address = ContactUtils.getAddressForPurpose(contact, purpose);
        ServiceMapping service = _updateServices.get(EntitySyncSourceHelper.getPurposeAndContactMechType(purpose));
        GenericValue partyContactMech = getActivePartyContactMechPurpose(contactParty, purpose);
        MapBeanConverter converter = _converters.getConverter(purpose, contact);

        if (partyContactMech != null) { //we actually have something to update!
            //now we have to check if is a real update, or effectively a delete
            if (ContactUtils.isEmpty(address, _defaultCountryGeoId, _defaultStateGeoId_US, _emptyFieldString)) {
                expirePartyContactMech(partyContactMech);
            } else { //it is a genuine update
                mech = partyContactMech.getRelatedOne("ContactMech");
                Map serviceParams = service.paramsMap(mech.getString("contactMechId"));  //set PK param
                serviceParams.put("partyId", partyContactMech.getString("partyId"));
                converter.toMap(contact, serviceParams);  //set other params
                _syncSource.runSync(service.getServiceName(), serviceParams);  //actually call the service
            }
        } else { //could be a new insertion
            //if the incoming address is empty, then we just want to ignore it
            if (ContactUtils.isEmpty(address, _defaultCountryGeoId, _defaultStateGeoId_US, _emptyFieldString)) {
                return;
            } else { //definitely something to insert
                insertContactMech(contactParty.getString("partyId"), contact, purpose);
            }
        }
    }

    /**
     * Expire the given PartyContactMech at the moment this method is called.
     */
    private void expirePartyContactMech(GenericValue partyContactMech) throws GenericEntityException {
        log.info("Empty record received from client so expiring PartyContactMechPurpose with contactMechId " + partyContactMech.get("contactMechId"));
        partyContactMech.set("thruDate", SyncUtilities.now());
        partyContactMech.store();
    }

    private GenericValue getActivePartyContactMechPurpose(GenericValue contactParty, String purpose) throws GenericEntityException {
        //1. first check that we have a unique record
        List<GenericValue> activePCMPs = contactParty.getRelatedByAnd("PartyContactMechPurpose", UtilMisc.toMap("contactMechPurposeTypeId", purpose, "thruDate", null));
        if (UtilValidate.isEmpty(activePCMPs)) {
            return null;
        }

        //2. now get the related PartyContactMech which is not expired - should only be one
        List<GenericValue> activePCMs = EntityUtil.getRelatedByAnd("PartyContactMech", UtilMisc.toMap("thruDate", null), activePCMPs);
        if (activePCMs.size() == 0) {
            return null;  //there is nothing
        } else if (activePCMs.size() > 1) {
            log.warn("Could not find a unique active PCMP for " + purpose + " and " + contactParty);
            throw new GenericEntityException("Could not find a unique active PCM for purpose " + purpose + " and party id " + contactParty.get("partyId"));
        } else {
            return activePCMs.get(0);
        }
    }

    /**
     * Insert (or updated if already exists) a TelecomNumber/Email/WebAddress for given contactParty and contactMechPurposeType.
     *
     * TODO: pull phone out via a query string/jxpath
     *
     * @param phoneType - FBol type string - this method does NOTHING if <code>phone</code> does not match this type
     * @throws SyncSourceException
     */
    private void updateNorInsertEntity(GenericValue contactParty, Contact contact, String purpose)
        throws GenericEntityException, GenericServiceException {
        //first detect if the contactmech of this type already exists or not
        GenericValue partyContactMech = getActivePartyContactMechPurpose(contactParty, purpose);
        if (partyContactMech != null) { //we actually have something to update!
            GenericValue mech = partyContactMech.getRelatedOne("ContactMech");
            String mechType = mech.getString("contactMechTypeId");
            ServiceMapping service = _updateServices.get(EntitySyncSourceHelper.getPurposeAndContactMechType(purpose));
            Map serviceParams = service.paramsMap(mech.getString("contactMechId"));  //set PK param

            if (isEmailPurpose(purpose)) {
                serviceParams.put("contactMechTypeId", mechType);
            }

            serviceParams.put("partyId", partyContactMech.getString("partyId"));

            EntityBeanConverter converter = null;
            if (service.getEntity() == null) {
                converter = _converters.getConverter(purpose, contact);
            } else {
                converter = _converters.getConverter(service.getEntity(), contact, purpose);
            }
            converter.toMap(contact, serviceParams);  //set other params

            //we may now have to set the extension in PartyContactMech, bizarrely, which is a related entity
            //see for explanation: https://issues.apache.org/jira/browse/OFBIZ-1332
            String extension = (String) serviceParams.remove("extension");
            if (extension != null) {
              partyContactMech.set("extension", extension);
              partyContactMech.store();
            }

            //now perform update or expire, depending on whether we really have a value
            if (ContactUtils.isEmpty(serviceParams, mechType)) {
                expirePartyContactMech(partyContactMech);
            } else {
                _syncSource.runSync(service.getServiceName(), serviceParams);  //actually call the service
            }
        } else {
            insertContactMech(contactParty.getString("partyId"), contact, purpose);
        }
    }

    private boolean isEmailPurpose(String purpose) {
        if (purpose.contains("EMAIL")) {
            return true;
        }
        return false;
    }

    /**
     * Build an FBol Phone bean for the given contactParty's data contactMechPurposeType and FBol phone type.
     *
     * TODO: factor out
     * TODO: cc-ac-num parsing
     *
     * @return null if the contactParty has no such data
     */
    private void buildPhone(List<TypifiedProperty> phones, GenericValue contactParty, String phonePurpose, String phoneType) throws GenericEntityException {
        //we must now build a Phone bean
        String phoneString = "";  //by default is empty, which will cause deletion to corresponding record in Outlook
        Phone phone = new Phone();
        phone.setPropertyType(phoneType);

        GenericValue phoneGV = getActivePartyContactMechPurpose(contactParty, phonePurpose);
        if (phoneGV != null) {
            GenericValue phoneNumberGV = phoneGV.getRelatedOne("TelecomNumber");
            if (phoneNumberGV != null) { //we do have phone number info to send to outlook
                //different OT fields are indicated by their String format, as FBol does not separate them
                //+countryCode (areaCode) contactNumber x extension
                String country = phoneNumberGV.getString("countryCode");
                country = UtilValidate.isEmpty(country) ? "" : "+" + country + " ";

                String area = phoneNumberGV.getString("areaCode");
                area = UtilValidate.isEmpty(area) ? "" : "("+area+") ";

                String contact = phoneNumberGV.getString("contactNumber");  //will just include extension as text
                if (UtilValidate.isEmpty(contact)) {
                    contact = "";
                }

                String extension = phoneGV.getString("extension");  //bizarrely, extension is store in the PCM
                if (UtilValidate.isEmpty(extension)) {
                    extension = "";
                } else {
                    extension = " " + extension; //convert to outlook format
                }

                //finally concatenate all the parts
                phoneString = country + area + contact; // + extension;
            } else {
                log.warn(phonePurpose +  " without TelecomNumber");
            }
        }

        //finally add the phone to the list
        phone.setPropertyValue(phoneString);
        phones.add(phone);
    }

    /**
     * Set a Funambol PIM property with a certain value, only if the value is not null or empty.
     */
    private void setIfNotEmpty(Property property, Object value) {
        if (!UtilValidate.isEmpty(value)) {
            property.setPropertyValue(value);
        }
    }

    /**
     * Check if a field in contact is empty.
     * @param contact the funambol Contact which has the field to be checked
     * @param mechPurpose partyContactMechPurposeId which is related to a type of a field in Cunambol Contact
     * @return true if the value of the field is null or has a empty String
     */
    private boolean isContactFieldEmpty(Contact contact, String mechPurpose) {
        String contactFieldType = mechPurposeAndContactFieldType.get(mechPurpose);

        TypifiedProperty contactField = null;

        if (_busPhoneTypes.containsValue(contactFieldType)) {
            contactField = getFieldFromType(contact.getBusinessDetail().getPhones(), contactFieldType);
        } else if (_persPhoneTypes.containsValue(contactFieldType)) {
            contactField = getFieldFromType(contact.getPersonalDetail().getPhones(), contactFieldType);
        } else if (_emailTypes.containsValue(contactFieldType)) {
            contactField = getFieldFromType(contact.getPersonalDetail().getEmails(), contactFieldType);
        } else if ("WebPage".equals(contactFieldType)) {
            contactField = getFieldFromType(contact.getPersonalDetail().getWebPages(), contactFieldType);
        } else if ("address".equals(contactFieldType)) {
            return false;
        }

        if (contactField == null) {
            return true;
        } else {
            log.debug("Empty validation returned " + UtilValidate.isEmpty(contactField.getPropertyValueAsString()) + " for " + contactFieldType);
            return UtilValidate.isEmpty(contactField.getPropertyValueAsString());
        }
    }

    /**
     * Get a funambol Contact field from a list given a type.
     * @param list - a list of TypifiedProperty instances
     * @param type - String that identify a certain contact field
     * @return TypifiedProperty which <code>getPropertyType</code> returns a value equals to <code>type</code>
     */
    private TypifiedProperty getFieldFromType(List<TypifiedProperty> list, String type) {
        for (TypifiedProperty property : list) {
            if (property.getPropertyType().equals(type)) {
                return property;
            }
        }
        return null;
    }

    /**
     * @return true if the current merge strategy means it is safe for server to overwrite client records
     * @return
     */
    private boolean serverWinsOnMerge() { return _mergeStrategy == MergeStrategy.server_wins || _mergeStrategy == MergeStrategy.merge; }

    /**
     * Mapping between a opentaps porpuse and funambol contact field.
     * @return map with <purpuse,contactFieldType>
     */
    private Map<String, String> initMechPurposeContactFieldType() {
        Map<String, String> tmp = new HashMap<String, String>();

        tmp.putAll(_busPhoneTypes);
        tmp.putAll(_emailTypes);
        tmp.putAll(_persPhoneTypes);
        tmp.put("GENERAL_LOCATION", "address");
        tmp.put("HOME_LOCATION", "address");
        tmp.put("OTHER_LOCATION", "address");
        tmp.put("PRIMARY_WEB_URL", "WebPage");
        return tmp;
    }

    public Contact convertDataToBean(SyncItem o) throws GeneralException {
        try {
            return vcard2Contact((SyncItem) o);
        } catch (ParseException e) {
            throw new GeneralException(e);
        }
    }

    public byte[] convertBeanToData(Contact contact) throws GeneralException {
        return contact2vcard(contact);
    }

    /**
     * Copied from PIMSyncSource.vcard2Contact.
     * TODO: refactor and make more generic, put in a centralized place
     * TODO: verify that vcardItem really has correct mimetype
     *
     * @param vcard
     * @return a Contact bean populated from the data in the vcard
     * @throws SyncSourceException if conversion was not possible
     * @throws ParseException
     */
    public static Contact vcard2Contact(SyncItem vcardItem) throws GeneralException, ParseException {
        log.info("Converting: VCARD => Contact: " + vcardItem.getKey());

        ByteArrayInputStream buffer = null;
        VcardParser parser = null;
        Contact contact = null;

        contact = new Contact();

        buffer = new ByteArrayInputStream(vcardItem.getContent());
        if (buffer.available() > 0) {
            parser = new VcardParser(buffer);
            contact = (Contact) parser.vCard();
        } else {
            throw new GeneralException("VCARD contained no data");  //TODO: error message and code
        }

        log.info("Conversion done.");

        return contact;
    }

    /**
     * Copied from PIMSyncSource.
     * TODO: refactor and make more generic, put in a centralized place
     *
     * @param vcard
     * @return vcard converted to binary data
     * @throws SyncSourceException
     * @throws ConverterException
     */
    public static byte[] contact2vcard(Contact contact) throws GeneralException
    {
        log.info("Converting: Contact => VCARD");

        String vcard = null;

        ContactToVcard c2vc = new ContactToVcard(null,null);
        try {
            vcard = c2vc.convert(contact);
        } catch (ConverterException e) {
            throw new GeneralException(e);
        }

        log.info("Conversion done, OUTPUT = {" + vcard + "}");
        return vcard.getBytes();
     }

    /**
     * Perform the following "tweaks" to a PIM contact (this method is typically called before passing the contact to OT)
     *  <ol>
     *   <li>Convert state and country text strings to corresponding geoId codes if they can be found.</li>
     *   <li>Split addressLine1 and put everything after the first CRLF, into extendedAddress</li>
     *  </ol>
     * @param contact - this contact will be altered in place
     *
     * TODO: botar este metodo num sitio especifico a contactos?
     */
    private void addressCompatibilization(Contact contact) throws GenericEntityException {
        //0. get list of addresses
        Address[] addresses = {
            contact.getBusinessDetail().getAddress(),
            contact.getPersonalDetail().getAddress(),
            contact.getPersonalDetail().getOtherAddress()
        };

        //1. alter each one
        for (Address address : addresses) {
            //1.1 country
            String country = address.getCountry().getPropertyValueAsString();
            String countryId = _defaultCountryGeoId;
            if (!UtilValidate.isEmpty(country)) {
                countryId = geoNameToId("COUNTRY", country);
                if (countryId == null) {
                    countryId = _defaultCountryGeoId;  //we can use default value
                }

            }
            address.getCountry().setPropertyValue(countryId);

            //1.2 state - here the algorithm is more complicated, we try (in order): geoId, geoCode, abbreviation, geoName
            String state = address.getState().getPropertyValueAsString();
            String stateId = null;
            if (!UtilValidate.isEmpty(state)) { //1.2.1 try a reverse lookup
                stateId = geoNameToId(null, state);  //we do not filter by STATE, PROVINCE etc. because there are so many possible options

                for (String fieldToCheck : new String[] {"geoId", "geoCode", "abbreviation", "geoName" }) {
                    stateId = geoFieldToId(null, fieldToCheck, state);
                    if (stateId != null) {
                        break;  //found something!
                    }
                }
            }
            if (stateId == null) { //if we still have nothing, use a default
                if ("USA".equals(countryId)) {
                    stateId = _defaultStateGeoId_US;
                } else {
                    stateId = "";  //there is no logical default value here
                }
            }
            address.getState().setPropertyValue(stateId);

            //1.3 address lines
            Property street = address.getStreet();
            String[] streets = StringUtils.split(street.getPropertyValueAsString(), "\r\n", 2);  //second element will contain any excess
            if (streets != null && streets.length > 0) {
                street.setPropertyValue(streets[0]);

                if (streets.length > 1) {
                    address.getExtendedAddress().setPropertyValue(streets[1]);
                }
            } else { //we must at least have a street value to pass OT service validation
                street.setPropertyValue(_emptyFieldString);
            }

            //1.4 postal code (some OT services do not allow it to be empty)
            Property postcode = address.getPostalCode();
            if (StringUtils.isBlank(postcode.getPropertyValueAsString())) {
                postcode.setPropertyValue(_emptyFieldString);
            }

            //1.5 city (some OT services do not allow it to be empty)
            Property city = address.getCity();
            if (StringUtils.isBlank(city.getPropertyValueAsString())) {
                city.setPropertyValue(_emptyFieldString);
            }
        }
    }

    /**
     * Perform a reverse lookup geoName -> geoId.
     *
     * @param geoType geoTypeId, ex. "COUNTRY" - if null will be ignored
     * @param geoName name for which we want the corresponding id
     * @return geoId which was found, or null if nothing was found
     */
    private String geoNameToId(String geoType, String geoName) throws GenericEntityException {
        return geoFieldToId(geoType, "geoName", geoName);
    }

    /**
     * Perform a reverse lookup <geoField> -> geoId.
     *
     * @param geoType geoTypeId, ex. "COUNTRY" - if null will be ignored
     * @param geoField a field from the Geo entity, e.g. geoName, geoCode
     * @param value value for <code>geoField</code> for which we want the corresponding id
     * @return geoId which was found, or null if nothing was found
     */
    private String geoFieldToId(String geoType, String geoField, String value) throws GenericEntityException {
        List<EntityCondition> criteria = new ArrayList<EntityCondition>();
        criteria.add(EntityCondition.makeCondition(geoField, EntityOperator.EQUALS, value));

        if (geoType != null) {
            criteria.add(EntityCondition.makeCondition("geoTypeId", EntityOperator.EQUALS, geoType));
        }

        List<GenericValue> likes = (List<GenericValue>) _syncSource._delegator.findByAnd("Geo", criteria);
        if (UtilValidate.isEmpty(likes)) {
            log.warn("Could not find expression like:" + geoField + "=" + value + " for geoTypeId: " + geoType);
            return null;
        } else {
            return likes.get(0).getString("geoId");
        }
    }
}
