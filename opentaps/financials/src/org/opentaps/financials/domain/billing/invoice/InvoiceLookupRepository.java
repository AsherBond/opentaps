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


import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.base.entities.Invoice;
import org.opentaps.base.entities.InvoiceItem;
import org.opentaps.base.entities.OrderItemBilling;
import org.opentaps.common.util.UtilDate;
import org.opentaps.domain.billing.invoice.InvoiceLookupRepositoryInterface;
import org.opentaps.domain.billing.invoice.InvoiceViewForListing;
import org.opentaps.foundation.entity.hibernate.HibernateUtil;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.CommonLookupRepository;

/**
 * Repository to lookup Invoices.
 */
public class InvoiceLookupRepository extends CommonLookupRepository implements InvoiceLookupRepositoryInterface {

    @SuppressWarnings("unused")
    private static final String MODULE = InvoiceLookupRepository.class.getName();

    private String invoiceTypeId;
    private String invoiceId;
    private String partyId;
    private String partyIdFrom;
    private String statusId;
    private String processingStatusId;
    private String message;
    private String orderId;
    private String itemDescription;
    private String referenceNumber;

    private Timestamp fromInvoiceDate;
    private Timestamp thruInvoiceDate;
    private Timestamp fromDueDate;
    private Timestamp thruDueDate;
    private Timestamp fromPaidDate;
    private Timestamp thruPaidDate;
    private String fromInvoiceDateString;
    private String thruInvoiceDateString;
    private String fromDueDateString;
    private String thruDueDateString;
    private String fromPaidDateString;
    private String thruPaidDateString;

    private BigDecimal fromAmount;
    private BigDecimal thruAmount;
    private BigDecimal fromOpenAmount;
    private BigDecimal thruOpenAmount;

    private String userLoginId;
    private String organizationPartyId;
    private Locale locale;
    private TimeZone timeZone;
    private List<String> orderBy;

    /**
     * Default constructor.
     */
    public InvoiceLookupRepository() {
        super();
    }

    /** {@inheritDoc} */
    public List<InvoiceViewForListing> findInvoices() throws RepositoryException {

        // convert from / thru from String into Timestamps if the string versions were given
        if (UtilValidate.isNotEmpty(fromInvoiceDateString)) {
            fromInvoiceDate = UtilDate.toTimestamp(fromInvoiceDateString, timeZone, locale);
        }
        if (UtilValidate.isNotEmpty(thruInvoiceDateString)) {
            thruInvoiceDate = UtilDate.toTimestamp(thruInvoiceDateString, timeZone, locale);
        }
        if (UtilValidate.isNotEmpty(fromDueDateString)) {
            fromDueDate = UtilDate.toTimestamp(fromDueDateString, timeZone, locale);
        }
        if (UtilValidate.isNotEmpty(thruDueDateString)) {
            thruDueDate = UtilDate.toTimestamp(thruDueDateString, timeZone, locale);
        }
        if (UtilValidate.isNotEmpty(fromPaidDateString)) {
            fromPaidDate = UtilDate.toTimestamp(fromPaidDateString, timeZone, locale);
        }
        if (UtilValidate.isNotEmpty(thruPaidDateString)) {
            thruPaidDate = UtilDate.toTimestamp(thruPaidDateString, timeZone, locale);
        }
        Session session = null;
        try {
            // get a hibernate session
            session = getInfrastructure().getSession();
            Criteria criteria = session.createCriteria(Invoice.class);

            // always filter by invoice type
            criteria.add(Restrictions.eq(Invoice.Fields.invoiceTypeId.name(), invoiceTypeId));

            // some id filters
            if (UtilValidate.isNotEmpty(partyId)) {
                criteria.add(Restrictions.eq(Invoice.Fields.partyId.name(), partyId));
            }
            if (UtilValidate.isNotEmpty(partyIdFrom)) {
                criteria.add(Restrictions.eq(Invoice.Fields.partyIdFrom.name(), partyIdFrom));
            }
            if (UtilValidate.isNotEmpty(invoiceId)) {
                criteria.add(Restrictions.eq(Invoice.Fields.invoiceId.name(), invoiceId));
            }
            if (UtilValidate.isNotEmpty(statusId)) {
                criteria.add(Restrictions.eq(Invoice.Fields.statusId.name(), statusId));
            }
            if (UtilValidate.isNotEmpty(processingStatusId)) {
                // this is a special case where we want an empty status
                if ("_NA_".equals(processingStatusId)) {
                    criteria.add(Restrictions.eq(Invoice.Fields.processingStatusId.name(), null));
                } else {
                    criteria.add(Restrictions.eq(Invoice.Fields.processingStatusId.name(), processingStatusId));
                }
            }

            // set the from/thru date filter if they were given
            if (fromInvoiceDate != null) {
                criteria.add(Restrictions.ge(Invoice.Fields.invoiceDate.name(), fromInvoiceDate));
            }
            if (thruInvoiceDate != null) {
                criteria.add(Restrictions.le(Invoice.Fields.invoiceDate.name(), thruInvoiceDate));
            }

            if (fromDueDate != null) {
                criteria.add(Restrictions.ge(Invoice.Fields.dueDate.name(), fromDueDate));
            }
            if (thruDueDate != null) {
                criteria.add(Restrictions.le(Invoice.Fields.dueDate.name(), thruDueDate));
            }

            if (fromPaidDate != null) {
                criteria.add(Restrictions.ge(Invoice.Fields.paidDate.name(), fromPaidDate));
            }
            if (thruPaidDate != null) {
                criteria.add(Restrictions.le(Invoice.Fields.paidDate.name(), thruPaidDate));
            }

            // set the from/thru amount filter if they were given
            if (fromAmount != null) {
                criteria.add(Restrictions.ge(Invoice.Fields.invoiceTotal.name(), fromAmount));
            }
            if (thruAmount != null) {
                criteria.add(Restrictions.le(Invoice.Fields.invoiceTotal.name(), thruAmount));
            }

            if (fromOpenAmount != null) {
                criteria.add(Restrictions.ge(Invoice.Fields.openAmount.name(), fromOpenAmount));
            }
            if (thruOpenAmount != null) {
                criteria.add(Restrictions.le(Invoice.Fields.openAmount.name(), thruOpenAmount));
            }

            // set the other like filters if they were given
            if (UtilValidate.isNotEmpty(referenceNumber)) {
                criteria.add(Restrictions.ilike(Invoice.Fields.referenceNumber.name(), referenceNumber, MatchMode.ANYWHERE));
            }
            if (UtilValidate.isNotEmpty(message)) {
                criteria.add(Restrictions.ilike(Invoice.Fields.invoiceMessage.name(), message, MatchMode.ANYWHERE));
            }
            // order Id search needs a join with OrderItemBilling
            if (UtilValidate.isNotEmpty(orderId)) {
                criteria.createAlias("orderItemBillings", "oib");
                criteria.add(Restrictions.eq("oib." + OrderItemBilling.Fields.orderId.name(), orderId));
            }
            // item description search needs a join with InvoiceItem
            if (UtilValidate.isNotEmpty(itemDescription)) {
                criteria.createAlias("invoiceItems", "ii");
                criteria.add(Restrictions.ilike("ii." + InvoiceItem.Fields.description.name(), itemDescription, MatchMode.ANYWHERE));
            }

            // TODO: accounting tags

            // specify the fields to return
            criteria.setProjection(Projections.projectionList()
                                   .add(Projections.distinct(Projections.property(Invoice.Fields.invoiceId.name())))
                                   .add(Projections.property(Invoice.Fields.partyId.name()))
                                   .add(Projections.property(Invoice.Fields.partyIdFrom.name()))
                                   .add(Projections.property(Invoice.Fields.statusId.name()))
                                   .add(Projections.property(Invoice.Fields.processingStatusId.name()))
                                   .add(Projections.property(Invoice.Fields.invoiceDate.name()))
                                   .add(Projections.property(Invoice.Fields.dueDate.name()))
                                   .add(Projections.property(Invoice.Fields.currencyUomId.name()))
                                   .add(Projections.property(Invoice.Fields.invoiceTotal.name()))
                                   .add(Projections.property(Invoice.Fields.openAmount.name()))
                                   .add(Projections.property(Invoice.Fields.referenceNumber.name())));

            // set the order by
            if (orderBy == null) {
                orderBy = Arrays.asList(Invoice.Fields.invoiceDate.desc());
            }
            // some substitution is needed to fit the hibernate field names
            // this also maps the calculated fields and indicates the non sortable fields
            Map<String, String> subs = new HashMap<String, String>();
            subs.put("partyName", "partyId");
            subs.put("partyNameFrom", "partyIdFrom");
            subs.put("invoiceDateString", "invoiceDate");
            subs.put("dueDateString", "dueDate");
            subs.put("statusDescription", "statusId");
            subs.put("processingStatusDescription", "processingStatusId");
            HibernateUtil.setCriteriaOrder(criteria, orderBy, subs);

            ScrollableResults results = null;
            List<InvoiceViewForListing> results2 = new ArrayList<InvoiceViewForListing>();
            try {
                // fetch the paginated results
                results = criteria.scroll(ScrollMode.SCROLL_INSENSITIVE);
                if (usePagination()) {
                    results.setRowNumber(getPageStart());
                }

                // convert them into InvoiceViewForListing objects which will also calculate or format some fields for display
                Object[] o = results.get();
                int n = 0; // number of results actually read
                while (o != null) {
                    InvoiceViewForListing r = new InvoiceViewForListing();
                    r.initRepository(this);
                    int i = 0;
                    r.setInvoiceId((String) o[i++]);
                    r.setPartyId((String) o[i++]);
                    r.setPartyIdFrom((String) o[i++]);
                    r.setStatusId((String) o[i++]);
                    r.setProcessingStatusId((String) o[i++]);
                    r.setInvoiceDate((Timestamp) o[i++]);
                    r.setDueDate((Timestamp) o[i++]);
                    r.setCurrencyUomId((String) o[i++]);
                    r.setInvoiceTotal((BigDecimal) o[i++]);
                    r.setOpenAmount((BigDecimal) o[i++]);
                    r.setReferenceNumber((String) o[i++]);
                    r.calculateExtraFields(getDelegator(), timeZone, locale);
                    results2.add(r);
                    n++;

                    if (!results.next()) {
                        break;
                    }
                    if (usePagination() && n >= getPageSize()) {
                        break;
                    }
                    o = results.get();
                }
                results.last();
                // note: row number starts at 0
                setResultSize(results.getRowNumber() + 1);
            } finally {
                results.close();
            }

            return results2;

        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /** {@inheritDoc} */
    public void setOrderBy(List<String> orderBy) {
        this.orderBy = orderBy;
    }

    /** {@inheritDoc} */
    public void setInvoiceTypeId(String invoiceTypeId) {
        this.invoiceTypeId = invoiceTypeId;
    }

    /** {@inheritDoc} */
    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    /** {@inheritDoc} */
    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    /** {@inheritDoc} */
    public void setPartyIdFrom(String partyIdFrom) {
        this.partyIdFrom = partyIdFrom;
    }

    /** {@inheritDoc} */
    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    /** {@inheritDoc} */
    public void setProcessingStatusId(String processingStatusId) {
        this.processingStatusId = processingStatusId;
    }

    /** {@inheritDoc} */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /** {@inheritDoc} */
    public void setMessage(String message) {
        this.message = message;
    }

    /** {@inheritDoc} */
    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    /** {@inheritDoc} */
    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    /** {@inheritDoc} */
    public void setFromDate(String fromDate) {
        this.fromInvoiceDateString = fromDate;
    }

    /** {@inheritDoc} */
    public void setThruDate(String thruDate) {
        this.thruInvoiceDateString = thruDate;
    }

    /** {@inheritDoc} */
    public void setFromDueDate(String fromDueDate) {
        this.fromDueDateString = fromDueDate;
    }

    /** {@inheritDoc} */
    public void setThruDueDate(String thruDueDate) {
        this.thruDueDateString = thruDueDate;
    }

    /** {@inheritDoc} */
    public void setFromPaidDate(String fromPaidDate) {
        this.fromPaidDateString = fromPaidDate;
    }

    /** {@inheritDoc} */
    public void setThruPaidDate(String thruPaidDate) {
        this.thruPaidDateString = thruPaidDate;
    }

    /** {@inheritDoc} */
    public void setFromDate(Timestamp fromDate) {
        this.fromInvoiceDate = fromDate;
    }

    /** {@inheritDoc} */
    public void setThruDate(Timestamp thruDate) {
        this.thruInvoiceDate = thruDate;
    }

    /** {@inheritDoc} */
    public void setFromDueDate(Timestamp fromDueDate) {
        this.fromDueDate = fromDueDate;
    }

    /** {@inheritDoc} */
    public void setThruDueDate(Timestamp thruDueDate) {
        this.thruDueDate = thruDueDate;
    }

    /** {@inheritDoc} */
    public void setFromPaidDate(Timestamp fromPaidDate) {
        this.fromPaidDate = fromPaidDate;
    }

    /** {@inheritDoc} */
    public void setThruPaidDate(Timestamp thruPaidDate) {
        this.thruPaidDate = thruPaidDate;
    }

    /** {@inheritDoc} */
    public void setFromAmount(BigDecimal fromAmount) {
        this.fromAmount = fromAmount;
    }

    /** {@inheritDoc} */
    public void setThruAmount(BigDecimal thruAmount) {
        this.thruAmount = thruAmount;
    }

    /** {@inheritDoc} */
    public void setFromOpenAmount(BigDecimal fromOpenAmount) {
        this.fromOpenAmount = fromOpenAmount;
    }

    /** {@inheritDoc} */
    public void setThruOpenAmount(BigDecimal thruOpenAmount) {
        this.thruOpenAmount = thruOpenAmount;
    }

    /** {@inheritDoc} */
    public void setUserLoginId(String userLoginId) {
        this.userLoginId = userLoginId;
    }

    /** {@inheritDoc} */
    public void setOrganizationPartyId(String organizationPartyId) {
        this.organizationPartyId = organizationPartyId;
    }

    /** {@inheritDoc} */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /** {@inheritDoc} */
    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }
}
