/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
package org.opentaps.financials.domain.billing.invoice;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.base.entities.AgreementInvoiceItemType;
import org.opentaps.domain.base.entities.InvoiceAdjustment;
import org.opentaps.domain.base.entities.InvoiceAdjustmentType;
import org.opentaps.domain.base.entities.InvoiceAndInvoiceItem;
import org.opentaps.domain.base.entities.InvoiceContactMech;
import org.opentaps.domain.base.entities.InvoiceItem;
import org.opentaps.domain.base.entities.InvoiceItemType;
import org.opentaps.domain.base.entities.InvoiceItemTypeAndOrgGlAccount;
import org.opentaps.domain.base.entities.PaymentAndApplication;
import org.opentaps.domain.base.entities.PostalAddress;
import org.opentaps.domain.base.entities.ProductInvoiceItemType;
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
    public InvoiceRepository(GenericDelegator delegator) {
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
        return findList(Invoice.class, new EntityExpr(Invoice.Fields.invoiceId.getName(), EntityOperator.IN, invoiceIds));
    }

    /** {@inheritDoc} */
    public InvoiceItem getInvoiceItemById(String invoiceId, String invoiceItemSeqId) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(InvoiceItem.class, map(InvoiceItem.Fields.invoiceId, invoiceId, InvoiceItem.Fields.invoiceItemSeqId, invoiceItemSeqId), "InvoiceItem [" + invoiceId + "/" + invoiceItemSeqId + "] not found");
    }

    /** {@inheritDoc} */
    public List<InvoiceAndInvoiceItem> getRelatedInterestInvoiceItems(Invoice invoice) throws RepositoryException {
        return findList(InvoiceAndInvoiceItem.class, Arrays.asList(
                    new EntityExpr(InvoiceAndInvoiceItem.Fields.itemParentInvoiceId.getName(), EntityOperator.EQUALS, invoice.getInvoiceId()),
                    new EntityExpr(InvoiceAndInvoiceItem.Fields.itemInvoiceItemTypeId.getName(), EntityOperator.EQUALS, "INV_INTRST_CHRG"),
                    new EntityExpr(InvoiceAndInvoiceItem.Fields.statusId.getName(), EntityOperator.NOT_IN, UtilMisc.toList("INVOICE_CANCELLED", "INVOICE_WRITEOFF", "INVOICE_VOIDED"))));
    }

    /** {@inheritDoc} */
    public List<PaymentAndApplication> getPaymentsApplied(Invoice invoice, Timestamp asOfDateTime) throws RepositoryException {
        EntityConditionList dateCondition = new EntityConditionList(UtilMisc.toList(
                new EntityExpr(PaymentAndApplication.Fields.effectiveDate.getName(), EntityOperator.EQUALS, null),
                new EntityExpr(PaymentAndApplication.Fields.effectiveDate.getName(), EntityOperator.LESS_THAN_EQUAL_TO, asOfDateTime)
            ), EntityOperator.OR);

        EntityConditionList conditions = new EntityConditionList(UtilMisc.toList(
                dateCondition,
                new EntityExpr(PaymentAndApplication.Fields.invoiceId.getName(), EntityOperator.EQUALS, invoice.getInvoiceId()),
                new EntityExpr(PaymentAndApplication.Fields.statusId.getName(), EntityOperator.IN, UtilMisc.toList("PMNT_RECEIVED", "PMNT_SENT", "PMNT_CONFIRMED"))
            ), EntityOperator.AND);

        return findList(PaymentAndApplication.class, conditions, Arrays.asList(PaymentAndApplication.Fields.effectiveDate.getName()));
    }

    /** {@inheritDoc} */
    public List<PaymentAndApplication> getPendingPaymentsApplied(Invoice invoice, Timestamp asOfDateTime) throws RepositoryException {
        EntityConditionList dateCondition = new EntityConditionList(UtilMisc.toList(
                new EntityExpr(PaymentAndApplication.Fields.effectiveDate.getName(), EntityOperator.EQUALS, null),
                new EntityExpr(PaymentAndApplication.Fields.effectiveDate.getName(), EntityOperator.LESS_THAN_EQUAL_TO, asOfDateTime)
            ), EntityOperator.OR);

        EntityConditionList conditions = new EntityConditionList(UtilMisc.toList(
                dateCondition,
                new EntityExpr(PaymentAndApplication.Fields.invoiceId.getName(), EntityOperator.EQUALS, invoice.getInvoiceId()),
                new EntityExpr(PaymentAndApplication.Fields.statusId.getName(), EntityOperator.EQUALS, "PMNT_NOT_PAID")
            ), EntityOperator.AND);

        return findList(PaymentAndApplication.class, conditions, Arrays.asList(PaymentAndApplication.Fields.effectiveDate.getName()));
    }

    /** {@inheritDoc} */
    public InvoiceAdjustment getInvoiceAdjustmentById(String invoiceAdjustmentId) throws EntityNotFoundException, RepositoryException {
        return findOneNotNull(InvoiceAdjustment.class, map(InvoiceAdjustment.Fields.invoiceAdjustmentId, invoiceAdjustmentId));
    }

    /** {@inheritDoc} */
    public List<InvoiceAdjustment> getAdjustmentsApplied(Invoice invoice, Timestamp asOfDateTime) throws RepositoryException {
        EntityConditionList dateCondition = new EntityConditionList(UtilMisc.toList(
                new EntityExpr(InvoiceAdjustment.Fields.effectiveDate.getName(), EntityOperator.EQUALS, null),
                new EntityExpr(InvoiceAdjustment.Fields.effectiveDate.getName(), EntityOperator.LESS_THAN_EQUAL_TO, asOfDateTime)
            ), EntityOperator.OR);

        EntityConditionList conditions = new EntityConditionList(UtilMisc.toList(
                dateCondition,
                new EntityExpr(InvoiceAdjustment.Fields.invoiceId.getName(), EntityOperator.EQUALS, invoice.getInvoiceId())
            ), EntityOperator.AND);

        return findList(InvoiceAdjustment.class, conditions, Arrays.asList(InvoiceAdjustment.Fields.effectiveDate.getName()));
    }

    /** {@inheritDoc} */
    public List<InvoiceItemType> getApplicableInvoiceItemTypes(String invoiceTypeId, String organizationPartyId) throws RepositoryException {
        Set<String> typeIds;
        // partner invoices are treated in a special way
        if (InvoiceSpecification.InvoiceTypeEnum.PARTNER.equals(invoiceTypeId)) {
            List<AgreementInvoiceItemType> agreements = findListCache(AgreementInvoiceItemType.class, map(AgreementInvoiceItemType.Fields.agreementTypeId, "PARTNER_AGREEMENT"), Arrays.asList(AgreementInvoiceItemType.Fields.sequenceNum.desc()));
            typeIds = Entity.getDistinctFieldValues(String.class, agreements, AgreementInvoiceItemType.Fields.invoiceItemTypeIdFrom);
        } else {
            // for other invoice types, get invoice item types which have a GL account configured for them, either in InvoiceItemType.defaultGlAccountId or InvoiceItemTypeGlAccount entity.
            EntityConditionList invoiceItemTypeCondition = new EntityConditionList(UtilMisc.toList(
                new EntityConditionList(UtilMisc.toList(
                        new EntityExpr(InvoiceItemTypeAndOrgGlAccount.Fields.defaultGlAccountId.name(), EntityOperator.NOT_EQUAL, null),
                        new EntityConditionList(UtilMisc.toList(
                                new EntityExpr(InvoiceItemTypeAndOrgGlAccount.Fields.organizationPartyId.name(), EntityOperator.EQUALS, organizationPartyId),
                                new EntityExpr(InvoiceItemTypeAndOrgGlAccount.Fields.orgGlAccountId.name(), EntityOperator.NOT_EQUAL, null)
                                ),
                          EntityOperator.AND)),
                   EntityOperator.OR),
                new EntityExpr(InvoiceItemTypeAndOrgGlAccount.Fields.invoiceTypeId.name(), EntityOperator.EQUALS, invoiceTypeId)
                ), EntityOperator.AND);
            List<InvoiceItemTypeAndOrgGlAccount> itemTypes = findListCache(InvoiceItemTypeAndOrgGlAccount.class, invoiceItemTypeCondition);
            typeIds = Entity.getDistinctFieldValues(String.class, itemTypes, InvoiceItemTypeAndOrgGlAccount.Fields.invoiceItemTypeId);
        }
        return findListCache(InvoiceItemType.class, new EntityExpr(InvoiceItemType.Fields.invoiceItemTypeId.name(), EntityOperator.IN, typeIds));
    }

    /** {@inheritDoc} */
    public List<InvoiceItemType> getApplicableInvoiceItemTypes(Invoice invoice) throws RepositoryException {
        return getApplicableInvoiceItemTypes(invoice.getInvoiceTypeId(), invoice.getOrganizationPartyId());
    }

    /** {@inheritDoc} */
    public PostalAddress getShippingAddress(Invoice invoice) throws RepositoryException {
        // if more than one defined, then return a "random" one
        InvoiceContactMech mech = getFirst(findList(InvoiceContactMech.class, map(InvoiceContactMech.Fields.invoiceId, invoice.getInvoiceId(), InvoiceContactMech.Fields.contactMechPurposeTypeId, "SHIPPING_LOCATION")));
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
        invoice.setStatusId(InvoiceSpecification.InvoiceStatusEnum.PAID.getStatusId());
        update(invoice);
    }

    /** {@inheritDoc} */
    public PostalAddress getBillingAddress(Invoice invoice) throws RepositoryException {
        String billingPartyId = invoice.isReceivable() ? invoice.getPartyId() : invoice.getPartyIdFrom();

        InvoiceContactMech mech = getFirst(findList(InvoiceContactMech.class, map(InvoiceContactMech.Fields.invoiceId, invoice.getInvoiceId(), InvoiceContactMech.Fields.contactMechPurposeTypeId, "BILLING_LOCATION")));
        if (mech != null) {
            return findOne(PostalAddress.class, map(PostalAddress.Fields.contactMechId, mech.getContactMechId()));
        }

        // if the address is not in InvoiceContactMech, use the billing address of the party
        DomainsDirectory dd = this.getDomainsDirectory();
        PartyRepositoryInterface partyRepository = dd.getPartyDomain().getPartyRepository();
        PostalAddress billingAddress = null;

        try {
            Party billingParty = partyRepository.getPartyById(billingPartyId);
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
    public void setBillingAddress(Invoice invoice, PostalAddress billingAddress) throws RepositoryException {
        try {
            // TODO: this needs a solution to the Party.ContactPurpose issue
            List<InvoiceContactMech> addresses = invoice.getRelated(InvoiceContactMech.class);
            for (InvoiceContactMech address : addresses) {
                if ("BILLING_LOCATION".equals(address.getContactMechPurposeTypeId())) {
                    remove(address);
                }
            }

            // create the billing address
            InvoiceContactMech contactMech = new InvoiceContactMech();
            contactMech.setInvoiceId(invoice.getInvoiceId());
            contactMech.setContactMechPurposeTypeId("BILLING_LOCATION");
            contactMech.setContactMechId(billingAddress.getContactMechId());
            createOrUpdate(contactMech);

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
        if (invoice.getInvoiceTypeId().equals("SALES_INVOICE")) {
            accountingTagUsageTypeId = UtilAccountingTags.SALES_INVOICES_TAG;
            organizationPartyId = invoice.getPartyIdFrom();
        } else if (invoice.getInvoiceTypeId().equals("PURCHASE_INVOICE")) {
            accountingTagUsageTypeId = UtilAccountingTags.SALES_INVOICES_TAG;
            organizationPartyId = invoice.getPartyId();
        } else if (invoice.getInvoiceTypeId().equals("COMMISSION_INVOICE")) {
            accountingTagUsageTypeId = UtilAccountingTags.COMMISSION_INVOICES_TAG;
            organizationPartyId = invoice.getPartyId();
        } else {
            return new ArrayList<AccountingTagConfigurationForOrganizationAndUsage>();
        }
        return getOrganizationRepository().validateTagParameters(item, organizationPartyId, accountingTagUsageTypeId);
    }

    protected OrganizationRepositoryInterface getOrganizationRepository() throws RepositoryException {
        if (organizationRepository == null) {
            organizationRepository = getDomainsDirectory().getOrganizationDomain().getOrganizationRepository();
        }
        return organizationRepository;
    }


    /** {@inheritDoc} */
    public List<InvoiceAdjustmentType> getInvoiceAdjustmentTypes(Organization organization, Invoice invoice) throws RepositoryException {
        String hql = "select distinct eo.invoiceAdjustmentType from InvoiceAdjustmentGlAccount eo"
            + " where eo.id.organizationPartyId=:organizationPartyId and eo.id.invoiceTypeId=:invoiceTypeId";
        try {
            Session session = getInfrastructure().getSession();
            Query query = session.createQuery(hql);
            query.setString("organizationPartyId", organization.getPartyId());
            query.setString("invoiceTypeId", invoice.getInvoiceTypeId());
            return query.list();
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        }
    }
}
