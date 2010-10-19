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
package org.opentaps.common.party;

import javolution.util.FastSet;

import org.apache.commons.lang.StringUtils;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;

import java.util.List;
import java.util.Set;

/**
 * Party Reader encapsulates many things about a given party, such as the name, classifications, etc.
 * Note that in this reader, I use an on-demand strategy to load data.  The idea is to reduce the
 * data loading impact to what is actually used by consumers of this class.
 *
 * TODO: this class would be a great place to put the party name stuff.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */

public class PartyReader {

    public static final String module = PartyReader.class.getName();

    protected GenericValue party = null;
    protected Delegator delegator = null;
    protected Set classifications = null;

    protected PartyReader() {}

    /**
     * Construct a PartyReader from a party entity or any entity that has partyId.
     */
    public PartyReader(GenericValue party) throws GenericEntityException, PartyNotFoundException {
        if (party == null) throw new PartyNotFoundException();
        this.delegator = party.getDelegator();

        // get the Party entity, because we want to use its relationships
        if (! "Party".equals(party.getEntityName())) {
            this.party = party.getRelatedOne("Party");
        } else {
            this.party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", party.get("partyId")));
        }
    }

    /** As a bove, but construct from a partyId and delegator */
    public PartyReader(String partyId, Delegator delegator) throws GenericEntityException, PartyNotFoundException {
        GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
        this.delegator = delegator;
        if (UtilValidate.isEmpty(party)) {
            throw new PartyNotFoundException();
        } else {
            this.party = party;
        }
    }

    public String getPartyId() {
        return party.getString("partyId");
    }

    /** Checks if a party has a particular PartyClassificationGroup */
    public boolean hasClassification(String partyClassificationGroupId) throws GenericEntityException {
        if (classifications == null) loadClassifications();
        return classifications.contains(partyClassificationGroupId);
    }

    private void loadClassifications() throws GenericEntityException {
        classifications = FastSet.newInstance();
        List<GenericValue> partyClassifications = EntityUtil.filterByDate( party.getRelated("PartyClassification") );
        for (GenericValue partyClassification : partyClassifications) {
            classifications.add( partyClassification.get("partyClassificationGroupId") );
        }
    }

    /** Gets the party name. */
    @Deprecated public String getPartyName() {
        return org.ofbiz.party.party.PartyHelper.getPartyName(party, false);
    }

    /** Gets the website. */
    public String getWebsite() throws GenericEntityException {
        List contacts = delegator.findByAnd("PartyContactWithPurpose",
                                            UtilMisc.toMap("partyId", party.getString("partyId"),
                                                           "contactMechTypeId", "WEB_ADDRESS"));
        List selContacts = EntityUtil.filterByDate(contacts, UtilDateTime.nowTimestamp(), "contactFromDate", "contactThruDate", true);
        if (UtilValidate.isEmpty(selContacts)) {
            return null;
        }

        GenericValue website = EntityUtil.getFirst(selContacts);
        if (UtilValidate.isEmpty(website)) {
            return null;
        }

        return website.getString("infoString");
    }

    /** Returns the composite name of the given party names. */
    @Deprecated public static String getPartyCompositeName(final GenericValue party) {
        StringBuffer retval = new StringBuffer();

        if (party == null) {
            return retval.toString();
        }

        String firstName = null;
        String lastName = null;
        String groupName = null;
        String partyId = null;
        StringBuffer name = new StringBuffer();

        if ((groupName = party.getString("groupName")) != null) {
            retval.append(groupName);
        }

        if ((firstName = party.getString("firstName")) != null) {
            name.append(firstName);
        }

        if ((lastName = party.getString("lastName")) != null) {
            name.append(" ").append(lastName);
        }

        if (StringUtils.isNotEmpty(name.toString())) {
            retval.append(" ");
            retval.append(name);
        }

        if ((partyId = party.getString("partyId")) != null) {
            retval.append(" (");
            retval.append(partyId);
            retval.append(")");
        }

        return retval.toString();
    }

    /** Gets the email. */
    public String getEmail() throws GenericEntityException {
        GenericValue email = getEmailPartyContactWithPurpose();
        if (email != null) {
            return email.getString("infoString");
        } else {
            return null;
        }

    }

    /** Gets the PartyContactWithPurpose of Email. */
    public GenericValue getEmailPartyContactWithPurpose() throws GenericEntityException {
        List contacts = delegator.findByAnd("PartyContactWithPurpose", UtilMisc.toMap("partyId", party.getString("partyId"),
                    "contactMechPurposeTypeId", "PRIMARY_EMAIL"));
        List selContacts = EntityUtil.filterByDate(contacts, UtilDateTime.nowTimestamp(), "contactFromDate", "contactThruDate", true);
        if (UtilValidate.isEmpty(selContacts)) {
            contacts = delegator.findByAnd("PartyContactWithPurpose", UtilMisc.toMap("partyId", party.getString("partyId"),
                    "contactMechTypeId", "EMAIL_ADDRESS"));
            selContacts = EntityUtil.filterByDate(contacts, UtilDateTime.nowTimestamp(), "contactFromDate", "contactThruDate", true);
        }
        if (UtilValidate.isEmpty(selContacts)) {
            return null;
        }
        GenericValue email = EntityUtil.getFirst(selContacts);
        if (UtilValidate.isEmpty(email)) {
            return null;
        }
        return email;

    }

    /** Gets the Bank account. */
    public GenericValue getBackAccount() throws GenericEntityException {
        List paymentMethods = delegator.findByAnd("PaymentMethod",UtilMisc.toMap("partyId", party.getString("partyId"), "paymentMethodTypeId", "EFT_ACCOUNT"));
        List selPayments = EntityUtil.filterByDate(paymentMethods, UtilDateTime.nowTimestamp(), "fromDate", "thruDate", true);
        if (selPayments != null && selPayments.size() > 0) {
            GenericValue PaymentMethod = (GenericValue) selPayments.iterator().next();
            GenericValue eftAccount = delegator.findByPrimaryKey("EftAccount",UtilMisc.toMap("paymentMethodId", PaymentMethod.getString("paymentMethodId")));
            return eftAccount;
        }
        return null;
   }
}

