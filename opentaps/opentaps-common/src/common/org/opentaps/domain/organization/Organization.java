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
package org.opentaps.domain.organization;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilDateTime;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.base.entities.PartyAcctgPreference;
import org.opentaps.base.entities.PaymentMethod;
import org.opentaps.domain.party.Party;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Organization domain entity as an extension of party domain entity.
 */
public class Organization extends Party {

    /**
     * Default constructor.
     */
    public Organization() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    protected void postInit() {
        super.postInit();
    }

    /**
     * Gets the URL to the logo image of this organization.
     * TODO: not very elegant
     * @return the URL to the logo image (eg: <code>http://www.opentaps.org/images/opentaps_logo.png</code>)
     */
    public String getLogoImageUrl() {
        return getCompleteView().getLogoImageUrl();
    }

    /**
     * Gets the <code>PartyAcctgPreference</code> for this organization.
     * Returns the organization domain object instead of the base entity.
     * @return the list of <code>PartyAcctgPreference</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public PartyAcctgPreference getPartyAcctgPreference() throws RepositoryException {
        return getRelatedOne(PartyAcctgPreference.class);
    }

    /**
     * Checks if this organization use standard costing.
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean usesStandardCosting() throws RepositoryException {
        return "STANDARD_COSTING".equals(getPartyAcctgPreference().getCostingMethodId());
    }

    /**
     * Finds the default <code>PaymentMethod</code>.
     * @return the default <code>PaymentMethod</code>
     * @throws RepositoryException if an error occurs
     */
    public PaymentMethod getDefaultPaymentMethod() throws RepositoryException {
        return getOrganizationRepository().getDefaultPaymentMethod(this.getPartyId());
    }

    /**
     * Converts the given amount into this organization base currency.
     * @param fromAmount a <code>BigDecimal</code> value
     * @param fromCurrencyUomId a <code>String</code> value
     * @return a <code>BigDecimal</code> value
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal convertUom(BigDecimal fromAmount, String fromCurrencyUomId) throws RepositoryException {
        return convertUom(fromAmount, fromCurrencyUomId, UtilDateTime.nowTimestamp());
    }

    /**
     * Converts the given amount into this organization base currency using the conversion rate as of the given date.
     * @param fromAmount a <code>BigDecimal</code> value
     * @param fromCurrencyUomId a <code>String</code> value
     * @param asOfDate a <code>Timestamp</code> value
     * @return a <code>BigDecimal</code> value
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal convertUom(BigDecimal fromAmount, String fromCurrencyUomId, Timestamp asOfDate) throws RepositoryException {
        return fromAmount.multiply(determineUomConversionFactor(fromCurrencyUomId, asOfDate));
    }

    /**
     * Gets the conversion factor from the given organization base currency into the given currency.
     * @param currencyUomId a <code>String</code> value
     * @return a <code>BigDecimal</code> value
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal determineUomConversionFactor(String currencyUomId) throws RepositoryException {
        return determineUomConversionFactor(currencyUomId, UtilDateTime.nowTimestamp());
    }

    /**
     * Gets the conversion factor from the given organization base currency into the given currency taking the conversion rate as of the given date.
     * @param currencyUomId a <code>String</code> value
     * @param asOfDate a <code>Timestamp</code> value
     * @return a <code>BigDecimal</code> value
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal determineUomConversionFactor(String currencyUomId, Timestamp asOfDate) throws RepositoryException {
        return getOrganizationRepository().determineUomConversionFactor(this.getPartyId(), currencyUomId, asOfDate);
    }

    /**
     * Gets the configured (non-null) Tag Types for the given organization, as a <code>Map</code> of
     * {index value: configured <code>enumTypeId</code>}.  For example: {1=DIVISION_TAG, 2=DEPARTMENT_TAG, 3=ACTIVITY_TAG}
     * @param accountingTagUsageTypeId the tag usage, for example "FINANCIALS_REPORTS", "PRCH_ORDER_ITEMS" ...
     * @return a <code>Map</code> of tagIndex: enumtypeId
     * @throws RepositoryException if an error occurs
     */
    public Map<Integer, String> getAccountingTagTypes(String accountingTagUsageTypeId) throws RepositoryException {
        return getOrganizationRepository().getAccountingTagTypes(this.getPartyId(), accountingTagUsageTypeId);
    }

    /**
     * Gets the configuration for this organization and given usage type.
     * @param accountingTagUsageTypeId the tag usage, for example "FINANCIALS_REPORTS", "PRCH_ORDER_ITEMS" ...
     * @return a list of <code>AccountingTagConfigurationForOrganizationAndUsage</code>, each one representing the tag type and available tag values for each tag index
     * @throws RepositoryException if an error occurs
     */
    public List<AccountingTagConfigurationForOrganizationAndUsage> getAccountingTagConfiguration(String accountingTagUsageTypeId) throws RepositoryException {
        return getOrganizationRepository().getAccountingTagConfiguration(this.getPartyId(), accountingTagUsageTypeId);
    }

    // can't name this same as getRepository() from party because of a return value collission, we can't do this in java
    protected OrganizationRepositoryInterface getOrganizationRepository() throws RepositoryException {
        try {
            return OrganizationRepositoryInterface.class.cast(repository);
        } catch (ClassCastException e) {
            repository = DomainsDirectory.getDomainsDirectory(repository).getPartyDomain().getPartyRepository();
            return OrganizationRepositoryInterface.class.cast(repository);
        }
    }

    /**
     * Checks if this organization use allocate Payment tags to Applications.
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean allocatePaymentTagsToApplications() throws RepositoryException {
        if (getPartyAcctgPreference().getAllocPaymentTagsToAppl() == null) {
            return Boolean.FALSE;
        }
        return "Y".equals(getPartyAcctgPreference().getAllocPaymentTagsToAppl());
    }

}
