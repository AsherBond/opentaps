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
package com.opensourcestrategies.crmsfa.leads;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.base.constants.ContactMechPurposeTypeConstants;
import org.opentaps.base.constants.ContactMechTypeConstants;
import org.opentaps.base.constants.SecurityPermissionConstants;
import org.opentaps.base.services.CreatePartyContactMechService;
import org.opentaps.base.services.CreatePartyEmailAddressService;
import org.opentaps.base.services.CreatePartyNoteService;
import org.opentaps.base.services.CreatePartyPostalAddressService;
import org.opentaps.base.services.CreatePartyTelecomNumberService;
import org.opentaps.base.services.CrmsfaCreateLeadService;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.domain.DomainService;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.service.ServiceException;

/**
 * Service to upload leads from an Excel spreadsheet.
 */
public class UploadLeadsServices extends DomainService {

    private static final String MODULE = UploadLeadsServices.class.getName();

    private String fileName;
    private String contentType;
    private ByteBuffer uploadedFile;

    private List<String> createdLeadIds;

    /**
     * Default constructor.
     */
    public UploadLeadsServices() {
        super();
    }

    /**
     * Creates a new <code>UploadLeadsServices</code> instance.
     *
     * @param infrastructure an <code>Infrastructure</code> value
     * @param user an <code>User</code> value
     * @param locale a <code>Locale</code> value
     * @exception ServiceException if an error occurs
     */
    public UploadLeadsServices(Infrastructure infrastructure, User user, Locale locale) throws ServiceException {
        super(infrastructure, user, locale);
    }

    /**
     * Sets the uploaded file data, required input parameter of the {@link #uploadLeads} service.
     * @param uploadedFile a <code>ByteBuffer</code> value
     */
    public void setUploadedFile(ByteBuffer uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    /**
     * Sets the uploaded file name, required input parameter of the {@link #uploadLeads} service.
     * @param fileName a <code>String</code> value
     */
    public void set_uploadedFile_fileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Sets the uploaded file content type, required input parameter of the {@link #uploadLeads} service.
     * @param contentType a <code>String</code> value
     */
    public void set_uploadedFile_contentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Gets the IDs of the created leads, output parameter from the {@link #uploadLeads} service.
     * @return the list of IDs of the created leads
     */
    public List<String> getCreatedLeadIds() {
        return createdLeadIds;
    }

    /**
     * Parses the uploaded Excel spreadsheet, and then uses the <code>crmsfa.createLead/createNote/createTelecomNumber</code> services to
     *  create the lead, the fax number, and the note from the spreadsheet.
     * For an example of the spreadsheet format, see <code>opentaps/crmsfa/data/xls/lead_import_example.xls</code>.
     * The spreadsheet should contain the following fields: companyName, firstName, lastName, attnName, address1, address2, city, stateProvinceGeoId, postalCode, postalCodeExt, countryGeoId, primaryPhoneCountryCode, primaryPhoneAreaCode, primaryPhoneNumber, primaryPhoneExtension, secondaryPhoneCountryCode, secondaryPhoneAreaCode, secondaryPhoneNumber, secondaryPhoneExtension, faxCountryCode, faxAreaCode, faxNumber, emailAddress, webAddress, note
     *
     * @exception ServiceException if an error occurs
     */
    public void uploadLeads() throws ServiceException {

        // check the CRMSFA_LEADS_UPLOAD permission
        checkPermission(SecurityPermissionConstants.CRMSFA_LEADS_UPLOAD);

        // parse the Excel file
        List<String> fieldsToParse = Arrays.asList("companyName", "firstName", "lastName",
                                                   "attnName", "address1", "address2", "city", "stateProvinceGeoId", "postalCode", "postalCodeExt", "countryGeoId",
                                                   "primaryPhoneCountryCode", "primaryPhoneAreaCode", "primaryPhoneNumber", "primaryPhoneExtension",
                                                   "secondaryPhoneCountryCode", "secondaryPhoneAreaCode", "secondaryPhoneNumber", "secondaryPhoneExtension",
                                                   "faxCountryCode", "faxAreaCode", "faxNumber",
                                                   "emailAddress",
                                                   "webAddress",
                                                   "note");

        // prepare the output list
        createdLeadIds = new ArrayList<String>();

        try {
            List<Map<String, String>> rows = UtilCommon.readExcelFile(new ByteArrayInputStream(uploadedFile.array()), fieldsToParse);
            for (Map<String, String> row : rows) {

                Debug.logInfo("Importing lead [" + row.get("firstName") + " " + row.get("lastName") + "]", MODULE);

                // Create the lead, this must be filled
                CrmsfaCreateLeadService createLeadSer = new CrmsfaCreateLeadService();
                createLeadSer.setInCompanyName(row.get("companyName"));
                createLeadSer.setInFirstName(row.get("firstName"));
                createLeadSer.setInLastName(row.get("lastName"));
                runSync(createLeadSer);
                String leadId = createLeadSer.getOutPartyId();

                // Create the postal address if given
                if (UtilValidate.isNotEmpty(row.get("address1"))) {
                    CreatePartyPostalAddressService addressSer = new CreatePartyPostalAddressService();
                    addressSer.setInPartyId(leadId);
                    addressSer.setInAttnName(row.get("attnName"));
                    addressSer.setInAddress1(row.get("address1"));
                    addressSer.setInAddress2(row.get("address2"));
                    addressSer.setInCity(row.get("city"));
                    addressSer.setInStateProvinceGeoId(row.get("stateProvinceGeoId"));
                    addressSer.setInPostalCode(row.get("postalCode"));
                    addressSer.setInPostalCodeExt(row.get("postalCodeExt"));
                    addressSer.setInCountryGeoId(row.get("countryGeoId"));
                    // set the purpose as General Correspondence Address
                    addressSer.setInContactMechPurposeTypeId(ContactMechPurposeTypeConstants.GENERAL_LOCATION);
                    // set the toName to the lead names
                    addressSer.setInToName(row.get("firstName") + " " + row.get("lastName"));
                    runSync(addressSer);
                }

                // Create the primary phone number if given
                if (UtilValidate.isNotEmpty(row.get("primaryPhoneNumber"))) {
                    CreatePartyTelecomNumberService phoneSer = new CreatePartyTelecomNumberService();
                    phoneSer.setInPartyId(leadId);
                    phoneSer.setInCountryCode(row.get("primaryPhoneCountryCode"));
                    phoneSer.setInAreaCode(row.get("primaryPhoneAreaCode"));
                    phoneSer.setInContactNumber(row.get("primaryPhoneNumber"));
                    phoneSer.setInExtension(row.get("primaryPhoneExtension"));
                    // set the purpose as Primary Phone Number
                    phoneSer.setInContactMechPurposeTypeId(ContactMechPurposeTypeConstants.PRIMARY_PHONE);
                    runSync(phoneSer);
                }

                // Create the secondary phone number if given
                if (UtilValidate.isNotEmpty(row.get("secondaryPhoneNumber"))) {
                    CreatePartyTelecomNumberService phoneSer = new CreatePartyTelecomNumberService();
                    phoneSer.setInPartyId(leadId);
                    phoneSer.setInCountryCode(row.get("secondaryPhoneCountryCode"));
                    phoneSer.setInAreaCode(row.get("secondaryPhoneAreaCode"));
                    phoneSer.setInContactNumber(row.get("secondaryPhoneNumber"));
                    phoneSer.setInExtension(row.get("secondaryPhoneExtension"));
                    // set the purpose as Primary Phone Number (there is no secondary type)
                    phoneSer.setInContactMechPurposeTypeId(ContactMechPurposeTypeConstants.PRIMARY_PHONE);
                    runSync(phoneSer);
                }

                // Create the fax number if given
                if (UtilValidate.isNotEmpty(row.get("faxNumber"))) {
                    CreatePartyTelecomNumberService faxSer = new CreatePartyTelecomNumberService();
                    faxSer.setInPartyId(leadId);
                    faxSer.setInCountryCode(row.get("faxCountryCode"));
                    faxSer.setInAreaCode(row.get("faxAreaCode"));
                    faxSer.setInContactNumber(row.get("faxNumber"));
                    // set the purpose as Fax Number
                    faxSer.setInContactMechPurposeTypeId(ContactMechPurposeTypeConstants.FAX_NUMBER);
                    runSync(faxSer);
                }

                // Create the email address if given
                if (UtilValidate.isNotEmpty(row.get("emailAddress"))) {
                    CreatePartyEmailAddressService emailSer = new CreatePartyEmailAddressService();
                    emailSer.setInPartyId(leadId);
                    emailSer.setInEmailAddress(row.get("emailAddress"));
                    // set the purpose as Primary Email Address
                    emailSer.setInContactMechPurposeTypeId(ContactMechPurposeTypeConstants.PRIMARY_EMAIL);
                    runSync(emailSer);
                }

                // Create the web address if given
                if (UtilValidate.isNotEmpty(row.get("webAddress"))) {
                    CreatePartyContactMechService webSer = new CreatePartyContactMechService();
                    webSer.setInPartyId(leadId);
                    webSer.setInContactMechTypeId(ContactMechTypeConstants.ElectronicAddress.WEB_ADDRESS);
                    webSer.setInInfoString(row.get("webAddress"));
                    // set the purpose as Primary Email Address
                    webSer.setInContactMechPurposeTypeId(ContactMechPurposeTypeConstants.PRIMARY_WEB_URL);
                    runSync(webSer);
                }

                // Create the note if given
                if (UtilValidate.isNotEmpty(row.get("note"))) {
                    CreatePartyNoteService noteSer = new CreatePartyNoteService();
                    noteSer.setInPartyId(leadId);
                    noteSer.setInNote(row.get("note"));
                    runSync(noteSer);
                }

                // add to the list of successfully imported leads
                createdLeadIds.add(leadId);

            }
        } catch (IOException e) {
            throw new ServiceException(e);
        }


    }
}
