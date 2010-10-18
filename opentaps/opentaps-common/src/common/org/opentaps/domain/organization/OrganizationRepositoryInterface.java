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

import org.opentaps.base.entities.CustomTimePeriod;
import org.opentaps.base.entities.PartyGroup;
import org.opentaps.base.entities.PaymentMethod;
import org.opentaps.base.entities.TermType;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Organization repository.
 */
public interface OrganizationRepositoryInterface extends RepositoryInterface {

    /**
     * Finds an <code>Organization</code> by ID from the database.
     * @param organizationPartyId the party ID for the organization
     * @return the <code>Organization</code> found
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException no <code>Organization</code> is found for the given id
     */
    public Organization getOrganizationById(String organizationPartyId) throws RepositoryException, EntityNotFoundException;

    /**
     * Get all time periods for the organization, including those which are closed or in the past or future.
     * @param organizationPartyId the party ID for the organization
     * @return the list of <code>CustomTimePeriod</code> for the organization
     * @throws RepositoryException if an error occurs
     */
    public List<CustomTimePeriod> getAllFiscalTimePeriods(String organizationPartyId) throws RepositoryException;

    /**
     * Finds the open time periods for an organization.
     * This method select only those time periods which are relevant to the fiscal
     * operations of a company.
     * @param organizationPartyId the party ID for the organization
     * @return the list of <code>CustomTimePeriod</code> found
     * @throws RepositoryException if an error occurs
     */
    public List<CustomTimePeriod> getOpenFiscalTimePeriods(String organizationPartyId) throws RepositoryException;

    /**
     * Finds the open time periods for an organization that the given date falls in.
     * This method select only those time periods which are relevant to the fiscal
     * operations of a company.
     * @param organizationPartyId the party ID for the organization
     * @param asOfDate the date for which to get the open fiscal periods
     * @return the list of <code>CustomTimePeriod</code> found
     * @throws RepositoryException if an error occurs
     */
    public List<CustomTimePeriod> getOpenFiscalTimePeriods(String organizationPartyId, Timestamp asOfDate) throws RepositoryException;
    
    /**
     * Finds the open time periods for an organization which are of the provided fiscal period types
     * (FISCAL_YEAR, FISCAL_QUARTER, etc.) that the given date falls in.
     * This method select only those time periods which are relevant to the fiscal
     * operations of a company.
     * @param organizationPartyId the party ID for the organization
     * @param fiscalPeriodTypes  a list of the fiscal period types 
     * @param asOfDate the date for which to get the open fiscal periods
     * @return the list of <code>CustomTimePeriod</code> found
     * @throws RepositoryException if an error occurs
     */
    public List<CustomTimePeriod> getOpenFiscalTimePeriods(String organizationPartyId, List<String> fiscalPeriodTypes, Timestamp asOfDate) throws RepositoryException;

    /**
     * Finds the default <code>PaymentMethod</code> for the given organization.
     * @param organizationPartyId the party ID for the organization
     * @return the default <code>PaymentMethod</code>
     * @throws RepositoryException if an error occurs
     */
    public PaymentMethod getDefaultPaymentMethod(String organizationPartyId) throws RepositoryException;

    /**
     * Gets the conversion factor from the given organization base currency into the given currency.
     * @param organizationPartyId the party ID for the organization
     * @param currencyUomId a <code>String</code> value
     * @return a <code>BigDecimal</code> value
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal determineUomConversionFactor(String organizationPartyId, String currencyUomId) throws RepositoryException;

    /**
     * Gets the conversion factor from the given organization base currency into the given currency taking the conversion rate as of the given date.
     * @param organizationPartyId the party ID for the organization
     * @param currencyUomId a <code>String</code> value
     * @param asOfDate a <code>Timestamp</code> value
     * @return a <code>BigDecimal</code> value
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal determineUomConversionFactor(String organizationPartyId, String currencyUomId, Timestamp asOfDate) throws RepositoryException;

    /**
     * Gets the configured (non-null) Tag Types for the given organization, as a <code>Map</code> of
     * {index value: configured <code>enumTypeId</code>}.  For example: {1=DIVISION_TAG, 2=DEPARTMENT_TAG, 3=ACTIVITY_TAG}
     * @param organizationPartyId the party ID for the organization
     * @param accountingTagUsageTypeId  the tag usage, for example "FINANCIALS_REPORTS", "PRCH_ORDER_ITEMS" ...
     * @return  a <code>Map</code> of tagIndex: enumtypeId
     * @throws RepositoryException if an error occurs
     */
    public Map<Integer, String> getAccountingTagTypes(String organizationPartyId, String accountingTagUsageTypeId) throws RepositoryException;

    /**
     * Gets the configuration for the given organization and usage type.
     * @param organizationPartyId the party ID for the organization
     * @param accountingTagUsageTypeId the tag usage, for example "FINANCIALS_REPORTS", "PRCH_ORDER_ITEMS" ...
     * @return a list of <code>AccountingTagConfigurationForOrganizationAndUsage</code>, each one representing the tag type and available tag values for each tag index
     * @throws RepositoryException if an error occurs
     */
    public List<AccountingTagConfigurationForOrganizationAndUsage> getAccountingTagConfiguration(String organizationPartyId, String accountingTagUsageTypeId) throws RepositoryException;

    /**
     * Gets the list of term type IDs that can be used for the given document type ID.
     * @param documentTypeId the document type to get the agreement term types for, eg: "SALES_INVOICE", "PURCHASE_ORDER", ...
     * @return a list of agreement term type IDs
     * @throws RepositoryException if an error occurs
     */
    public List<String> getValidTermTypeIds(String documentTypeId) throws RepositoryException;

    /**
     * Gets the list of term types that can be used for the given document type ID.
     * @param documentTypeId the document type to get the agreement term types for, eg: "SALES_INVOICE", "PURCHASE_ORDER", ...
     * @return a list of agreement term types
     * @throws RepositoryException if an error occurs
     */
    public List<TermType> getValidTermTypes(String documentTypeId) throws RepositoryException;

    /**
     * Gets all the organization party from the PartyRole = INTERNAL_ORGANIZATIO.
     * @return a list of organization party
     * @throws RepositoryException if an error occurs
     */
    public List<Organization> getAllValidOrganizations() throws RepositoryException;

    /**
     * Validates the accounting tags from a given <code>Map</code>, compared to the required tags as configured for the given organization and usage type.
     * @param tags a <code>Map<String, Object></code> value, which can simply be the Map representing an entity
     * @param organizationPartyId the party ID for the organization
     * @param accountingTagUsageTypeId  the tag usage, for example "FINANCIALS_REPORTS", "PRCH_ORDER_ITEMS" ...
     * @param prefix the prefix of the Map keys corresponding to the accounting tags
     * @return a list of the <code>AccountingTagConfigurationForOrganizationAndUsage</code> that are missing
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public List<AccountingTagConfigurationForOrganizationAndUsage> validateTagParameters(Map tags, String organizationPartyId, String accountingTagUsageTypeId, String prefix) throws RepositoryException;

    /**
     * Validates the accounting tags from a given <code>EntityInterface</code>, compared to the required tags as configured for the given organization and usage type.
     * @param entity an <code>EntityInterface</code> value
     * @param organizationPartyId the party ID for the organization
     * @param accountingTagUsageTypeId  the tag usage, for example "FINANCIALS_REPORTS", "PRCH_ORDER_ITEMS" ...
     * @return a list of the <code>AccountingTagConfigurationForOrganizationAndUsage</code> that are missing
     * @throws RepositoryException if an error occurs
     */
    public List<AccountingTagConfigurationForOrganizationAndUsage> validateTagParameters(EntityInterface entity, String organizationPartyId, String accountingTagUsageTypeId) throws RepositoryException;

    /**
     * Returns a list of the party groups with role ORGANIZATION_TEMPL 
     * @throws RepositoryException if an error occurs
     */
    public List<PartyGroup> getOrganizationTemplates() throws RepositoryException;
    
    /**
     * Returns a list of party groups with role INTERNAL_ORGANIZATIO no PartyAcctgPreference 
     * @throws RepositoryException if an error occurs
     */
    public List<PartyGroup> getOrganizationWithoutLedgerSetup() throws RepositoryException;
}
