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
package org.opentaps.financials.domain.billing.invoice;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.ofbiz.accounting.invoice.InvoiceServices;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.base.constants.AgreementTypeConstants;
import org.opentaps.base.constants.ContactMechPurposeTypeConstants;
import org.opentaps.base.constants.InvoiceItemTypeConstants;
import org.opentaps.base.constants.InvoiceTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.AgreementInvoiceItemType;
import org.opentaps.base.entities.InvoiceAdjustment;
import org.opentaps.base.entities.InvoiceAdjustmentType;
import org.opentaps.base.entities.InvoiceAndInvoiceItem;
import org.opentaps.base.entities.InvoiceContactMech;
import org.opentaps.base.entities.InvoiceItem;
import org.opentaps.base.entities.InvoiceItemType;
import org.opentaps.base.entities.InvoiceItemTypeAndOrgGlAccount;
import org.opentaps.base.entities.OrderItem;
import org.opentaps.base.entities.PaymentAndApplication;
import org.opentaps.base.entities.PaymentApplication;
import org.opentaps.base.entities.PostalAddress;
import org.opentaps.base.entities.ProductInvoiceItemType;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.billing.invoice.InvoiceSpecificationInterface;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.domain.product.Product;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.entity.hibernate.Query;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

/**
 * Repository for Invoices to handle interaction of Invoice-related domain with the entity engine (database) and the service engine.
 */
public class InvoiceRepository extends Repository implements InvoiceRepositoryInterface {
	 private static final String MODULE = InvoiceRepository.class.getName();
    private InvoiceSpecificationInterface invoiceSpecification = new InvoiceSpecification();
    private OrganizationRepositoryInterface organizationRepository;

    /**
     * Default constructor.
     */
    public InvoiceRepository() {
        super();
    }

    /**
     * Use this for Repositories which will only access the database via the delegator.
     * @param delegator the delegator
     * @deprecated don't use this, always use domain loader, refactor all code using it
     */
    public InvoiceRepository(Delegator delegator) {
        super(delegator);
    }

    /** {@inheritDoc} */
    public InvoiceSpecificationInterface getInvoiceSpecification() {
        return invoiceSpecification;
    }

    /** {@inheritDoc} */
    public Invoice getInvoiceById(String invoiceId) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(Invoice.class, map(Invoice.Fields.invoiceId, invoiceId), "Invoice [" + invoiceId + "] not found");
    }

    /** {@inheritDoc} */
    public List<Invoice> getInvoicesByIds(Collection<String> invoiceIds) throws RepositoryException {
        return findList(Invoice.class, EntityCondition.makeCondition(Invoice.Fields.invoiceId.getName(), EntityOperator.IN, invoiceIds));
    }

    /** {@inheritDoc} */
    public InvoiceItem getInvoiceItemById(String invoiceId, String invoiceItemSeqId) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(InvoiceItem.class, map(InvoiceItem.Fields.invoiceId, invoiceId, InvoiceItem.Fields.invoiceItemSeqId, invoiceItemSeqId), "InvoiceItem [" + invoiceId + "/" + invoiceItemSeqId + "] not found");
    }

    /** {@inheritDoc} */
    public List<InvoiceAndInvoiceItem> getRelatedInterestInvoiceItems(Invoice invoice) throws RepositoryException {
        return findList(InvoiceAndInvoiceItem.class, Arrays.asList(
                    EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.itemParentInvoiceId.getName(), EntityOperator.EQUALS, invoice.getInvoiceId()),
                    EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.itemInvoiceItemTypeId.getName(), EntityOperator.EQUALS, InvoiceItemTypeConstants.INV_INTRST_CHRG),
                    EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.statusId.getName(), EntityOperator.NOT_IN, UtilMisc.toList(StatusItemConstants.InvoiceStatus.INVOICE_CANCELLED, StatusItemConstants.InvoiceStatus.INVOICE_WRITEOFF, StatusItemConstants.InvoiceStatus.INVOICE_VOIDED))));
    }

    /** {@inheritDoc} */
    public List<PaymentAndApplication> getPaymentsApplied(Invoice invoice, Timestamp asOfDateTime) throws RepositoryException {
        EntityCondition dateCondition = EntityCondition.makeCondition(EntityOperator.OR,
                EntityCondition.makeCondition(PaymentAndApplication.Fields.effectiveDate.getName(), EntityOperator.EQUALS, null),
                EntityCondition.makeCondition(PaymentAndApplication.Fields.effectiveDate.getName(), EntityOperator.LESS_THAN_EQUAL_TO, asOfDateTime));

        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                dateCondition,
                EntityCondition.makeCondition(PaymentAndApplication.Fields.invoiceId.getName(), EntityOperator.EQUALS, invoice.getInvoiceId()),
                EntityCondition.makeCondition(PaymentAndApplication.Fields.statusId.getName(), EntityOperator.IN, UtilMisc.toList(StatusItemConstants.PmntStatus.PMNT_RECEIVED, StatusItemConstants.PmntStatus.PMNT_SENT, StatusItemConstants.PmntStatus.PMNT_CONFIRMED)));

        return findList(PaymentAndApplication.class, conditions, Arrays.asList(PaymentAndApplication.Fields.effectiveDate.getName()));
    }

    /** {@inheritDoc} */
    public List<PaymentAndApplication> getPendingPaymentsApplied(Invoice invoice, Timestamp asOfDateTime) throws RepositoryException {
        EntityCondition dateCondition = EntityCondition.makeCondition(EntityOperator.OR,
                EntityCondition.makeCondition(PaymentAndApplication.Fields.effectiveDate.getName(), EntityOperator.EQUALS, null),
                EntityCondition.makeCondition(PaymentAndApplication.Fields.effectiveDate.getName(), EntityOperator.LESS_THAN_EQUAL_TO, asOfDateTime));

        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                dateCondition,
                EntityCondition.makeCondition(PaymentAndApplication.Fields.invoiceId.getName(), EntityOperator.EQUALS, invoice.getInvoiceId()),
                EntityCondition.makeCondition(PaymentAndApplication.Fields.statusId.getName(), EntityOperator.EQUALS, StatusItemConstants.PmntStatus.PMNT_NOT_PAID));

        return findList(PaymentAndApplication.class, conditions, Arrays.asList(PaymentAndApplication.Fields.effectiveDate.getName()));
    }

    /** {@inheritDoc} */
    public InvoiceAdjustment getInvoiceAdjustmentById(String invoiceAdjustmentId) throws EntityNotFoundException, RepositoryException {
        return findOneNotNull(InvoiceAdjustment.class, map(InvoiceAdjustment.Fields.invoiceAdjustmentId, invoiceAdjustmentId));
    }

    /** {@inheritDoc} */
    public List<InvoiceAdjustment> getAdjustmentsApplied(Invoice invoice, Timestamp asOfDateTime) throws RepositoryException {
        EntityCondition dateCondition = EntityCondition.makeCondition(EntityOperator.OR,
                EntityCondition.makeCondition(InvoiceAdjustment.Fields.effectiveDate.getName(), EntityOperator.EQUALS, null),
                EntityCondition.makeCondition(InvoiceAdjustment.Fields.effectiveDate.getName(), EntityOperator.LESS_THAN_EQUAL_TO, asOfDateTime));

        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                dateCondition,
                EntityCondition.makeCondition(InvoiceAdjustment.Fields.invoiceId.getName(), EntityOperator.EQUALS, invoice.getInvoiceId()));

        return findList(InvoiceAdjustment.class, conditions, Arrays.asList(InvoiceAdjustment.Fields.effectiveDate.getName()));
    }

    /** {@inheritDoc} */
    public List<InvoiceItemType> getApplicableInvoiceItemTypes(String invoiceTypeId, String organizationPartyId) throws RepositoryException {
        Set<String> typeIds;
        Debug.logInfo("invoiceTypeId : " + invoiceTypeId + ", organizationPartyId : " + organizationPartyId, MODULE);
        // partner invoices are treated in a special way
        if (InvoiceSpecification.InvoiceTypeEnum.PARTNER.equals(invoiceTypeId)) {
            List<AgreementInvoiceItemType> agreements = findListCache(AgreementInvoiceItemType.class, map(AgreementInvoiceItemType.Fields.agreementTypeId, AgreementTypeConstants.PARTNER_AGREEMENT), Arrays.asList(AgreementInvoiceItemType.Fields.sequenceNum.desc()));
            typeIds = Entity.getDistinctFieldValues(String.class, agreements, AgreementInvoiceItemType.Fields.invoiceItemTypeIdFrom);
        } else {
            // for other invoice types, get invoice item types which have a GL account configured for them, either in InvoiceItemType.defaultGlAccountId or InvoiceItemTypeGlAccount entity.
            EntityCondition invoiceItemTypeCondition = EntityCondition.makeCondition(EntityOperator.AND,
                   EntityCondition.makeCondition(InvoiceItemTypeAndOrgGlAccount.Fields.invoiceTypeId.name(), EntityOperator.EQUALS, invoiceTypeId),
                   EntityCondition.makeCondition(EntityOperator.OR,
                        EntityCondition.makeCondition(InvoiceItemTypeAndOrgGlAccount.Fields.defaultGlAccountId.name(), EntityOperator.NOT_EQUAL, null),
                        EntityCondition.makeCondition(EntityOperator.AND,
                                EntityCondition.makeCondition(InvoiceItemTypeAndOrgGlAccount.Fields.organizationPartyId.name(), EntityOperator.EQUALS, organizationPartyId),
                                EntityCondition.makeCondition(InvoiceItemTypeAndOrgGlAccount.Fields.orgGlAccountId.name(), EntityOperator.NOT_EQUAL, null)
                                )));
            Debug.logInfo("invoiceItemTypeCondition : " + invoiceItemTypeCondition.toString(), MODULE);
            List<InvoiceItemTypeAndOrgGlAccount> itemTypes = findListCache(InvoiceItemTypeAndOrgGlAccount.class, invoiceItemTypeCondition);
            typeIds = Entity.getDistinctFieldValues(String.class, itemTypes, InvoiceItemTypeAndOrgGlAccount.Fields.invoiceItemTypeId);
        }
        return findListCache(InvoiceItemType.class, EntityCondition.makeCondition(InvoiceItemType.Fields.invoiceItemTypeId.name(), EntityOperator.IN, typeIds));
    }

    /** {@inheritDoc} */
    public List<InvoiceItemType> getApplicableInvoiceItemTypes(Invoice invoice) throws RepositoryException {
        return getApplicableInvoiceItemTypes(invoice.getInvoiceTypeId(), invoice.getOrganizationPartyId());
    }

    /** {@inheritDoc} */
    public PostalAddress getShippingAddress(Invoice invoice) throws RepositoryException {
        // if more than one defined, then return a "random" one
        InvoiceContactMech mech = getFirst(findList(InvoiceContactMech.class, map(InvoiceContactMech.Fields.invoiceId, invoice.getInvoiceId(), InvoiceContactMech.Fields.contactMechPurposeTypeId, ContactMechPurposeTypeConstants.SHIPPING_LOCATION), UtilMisc.toList("lastUpdatedStamp DESC")));
        if (mech == null) {
            return null;
        }
        return findOne(PostalAddress.class, map(PostalAddress.Fields.contactMechId, mech.getContactMechId()));
    }

    /** {@inheritDoc} */
    public void setPaid(Invoice invoice) throws RepositoryException {
        // not using the ofbiz setInvoiceStatus right now because its checkPaymentApplications service would not
        // take InvoiceAdjustment into account, and also because the method is by design not required to check that
        // the invoice is fully paid
        // set paid date
        if (invoice.getPaymentApplications().size() > 0) {
            // set the last payment.effectiveDate as paid date
            Timestamp paidDate = null;
            for (PaymentApplication paymentApplication: invoice.getPaymentApplications()) {
                if (paidDate == null || paidDate.before(paymentApplication.getPayment().getEffectiveDate())) {
                    paidDate = paymentApplication.getPayment().getEffectiveDate();
                }
            }
            invoice.setPaidDate(paidDate);
            Debug.logInfo("set invoice paid date : [" + paidDate + "]", MODULE);
        }
        invoice.setStatusId(InvoiceSpecification.InvoiceStatusEnum.PAID.getStatusId());
        update(invoice);
    }

    /** {@inheritDoc} */
    public PostalAddress getBillingAddress(Invoice invoice) throws RepositoryException {
        InvoiceContactMech mech = getFirst(findList(InvoiceContactMech.class, map(InvoiceContactMech.Fields.invoiceId, invoice.getInvoiceId(), InvoiceContactMech.Fields.contactMechPurposeTypeId, ContactMechPurposeTypeConstants.BILLING_LOCATION), UtilMisc.toList("lastUpdatedStamp DESC")));
        if (mech != null) {
            return findOne(PostalAddress.class, map(PostalAddress.Fields.contactMechId, mech.getContactMechId()));
        }

        // if the address is not in InvoiceContactMech, use the billing address of the party
        DomainsDirectory dd = DomainsDirectory.getDomainsDirectory(this);
        PartyRepositoryInterface partyRepository = dd.getPartyDomain().getPartyRepository();
        PostalAddress billingAddress = null;

        try {
            Party billingParty = partyRepository.getPartyById(invoice.getTransactionPartyId());
            billingAddress = billingParty.getBillingAddress();
            if (billingAddress == null) {
                // no billing address, use general one
                List<PostalAddress> partyAddresses = partyRepository.getPostalAddresses(billingParty, Party.ContactPurpose.GENERAL_ADDRESS);
                if (UtilValidate.isNotEmpty(partyAddresses)) {
                    billingAddress = partyAddresses.get(0);
                }
            }
        } catch (EntityNotFoundException e) {
            // the EntityNotFoundException here is not relevant to the method
            // so we convert it to a generic RepositoryException
            throw new RepositoryException(e);
        }

        return billingAddress;
    }

    /** {@inheritDoc} */
    public void setShippingAddress(Invoice invoice, PostalAddress shippingAddress) throws RepositoryException {
        try {
            // TODO: this needs a solution to the Party.ContactPurpose issue
            // remove all associated InvoiceContactMech that are SHIPPING_LOCATION
            List<InvoiceContactMech> addresses = invoice.getRelated(InvoiceContactMech.class);
            for (InvoiceContactMech address : addresses) {
                if (ContactMechPurposeTypeConstants.SHIPPING_LOCATION.equals(address.getContactMechPurposeTypeId())) {
                    remove(address);
                }
            }

            if (shippingAddress != null) {
                // create the new shipping address
                InvoiceContactMech contactMech = new InvoiceContactMech();
                contactMech.setInvoiceId(invoice.getInvoiceId());
                contactMech.setContactMechPurposeTypeId(ContactMechPurposeTypeConstants.SHIPPING_LOCATION);
                contactMech.setContactMechId(shippingAddress.getContactMechId());
                createOrUpdate(contactMech);

                // set the invoice contactMechId
                invoice.setContactMechId(shippingAddress.getContactMechId());
                update(invoice);
            }

        } catch (GeneralException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public void setBillingAddress(Invoice invoice, PostalAddress billingAddress) throws RepositoryException {
        try {
            // TODO: this needs a solution to the Party.ContactPurpose issue
            // remove all associated InvoiceContactMech that are BILLING_LOCATION
            List<InvoiceContactMech> addresses = invoice.getRelated(InvoiceContactMech.class);
            for (InvoiceContactMech address : addresses) {
                if (ContactMechPurposeTypeConstants.BILLING_LOCATION.equals(address.getContactMechPurposeTypeId())) {
                    remove(address);
                }
            }

            if (billingAddress != null) {
                // create the new billing address
                InvoiceContactMech contactMech = new InvoiceContactMech();
                contactMech.setInvoiceId(invoice.getInvoiceId());
                contactMech.setContactMechPurposeTypeId(ContactMechPurposeTypeConstants.BILLING_LOCATION);
                contactMech.setContactMechId(billingAddress.getContactMechId());
                createOrUpdate(contactMech);

                // set the invoice contactMechId
                invoice.setContactMechId(billingAddress.getContactMechId());
                update(invoice);
            }

        } catch (GeneralException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public String getInvoiceItemTypeIdForProduct(Invoice invoice, Product product) throws RepositoryException {
        ProductInvoiceItemType productOrderItemType = findOne(ProductInvoiceItemType.class, map(ProductInvoiceItemType.Fields.productTypeId, product.getProductTypeId(), ProductInvoiceItemType.Fields.invoiceTypeId, invoice.getInvoiceTypeId()));
        if (productOrderItemType != null) {
            return productOrderItemType.getInvoiceItemTypeId();
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    public List<AccountingTagConfigurationForOrganizationAndUsage> validateTagParameters(Invoice invoice, InvoiceItem item) throws RepositoryException {
        String organizationPartyId = null;
        String accountingTagUsageTypeId = null;
        if (invoice.getInvoiceTypeId().equals(InvoiceTypeConstants.SALES_INVOICE)) {
            accountingTagUsageTypeId = UtilAccountingTags.SALES_INVOICES_TAG;
            organizationPartyId = invoice.getPartyIdFrom();
        } else if (invoice.getInvoiceTypeId().equals(InvoiceTypeConstants.PURCHASE_INVOICE)) {
            accountingTagUsageTypeId = UtilAccountingTags.SALES_INVOICES_TAG;
            organizationPartyId = invoice.getPartyId();
        } else if (invoice.getInvoiceTypeId().equals(InvoiceTypeConstants.COMMISSION_INVOICE)) {
            accountingTagUsageTypeId = UtilAccountingTags.COMMISSION_INVOICES_TAG;
            organizationPartyId = invoice.getPartyId();
        } else {
            return new ArrayList<AccountingTagConfigurationForOrganizationAndUsage>();
        }
        return getOrganizationRepository().validateTagParameters(item, organizationPartyId, accountingTagUsageTypeId);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    public InvoiceItemType getInvoiceItemType(OrderItem orderItem, String invoiceTypeId) throws RepositoryException {
        org.opentaps.base.entities.Product itemProduct = orderItem.getProduct();
        String invoiceItemTypeId = InvoiceServices.getInvoiceItemType(getDelegator(), orderItem.getOrderItemTypeId(), itemProduct.getProductTypeId(), invoiceTypeId, InvoiceItemTypeConstants.INV_FPROD_ITEM);
        return findOneCache(InvoiceItemType.class, map(InvoiceItemType.Fields.invoiceItemTypeId, invoiceItemTypeId));
    }

    protected OrganizationRepositoryInterface getOrganizationRepository() throws RepositoryException {
        if (organizationRepository == null) {
            organizationRepository = DomainsDirectory.getDomainsDirectory(this).getOrganizationDomain().getOrganizationRepository();
        }
        return organizationRepository;
    }


    /** {@inheritDoc} */
    public List<InvoiceAdjustmentType> getInvoiceAdjustmentTypes(Organization organization, Invoice invoice) throws RepositoryException {
        String hql = "select distinct eo.invoiceAdjustmentType from InvoiceAdjustmentGlAccount eo"
            + " where eo.id.organizationPartyId=:organizationPartyId and eo.id.invoiceTypeId=:invoiceTypeId";
        Session session = null;
        try {
            session = getInfrastructure().getSession();
            Query query = session.createQuery(hql);
            query.setString("organizationPartyId", organization.getPartyId());
            query.setString("invoiceTypeId", invoice.getInvoiceTypeId());
            return query.list();
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
