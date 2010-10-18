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
package org.opentaps.common.domain.order;

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
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.OrderTypeConstants;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.OrderHeader;
import org.opentaps.base.entities.OrderItem;
import org.opentaps.base.entities.OrderRole;
import org.opentaps.base.entities.ProductAndGoodIdentification;
import org.opentaps.common.util.UtilDate;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.order.OrderViewForListing;
import org.opentaps.domain.order.PurchaseOrderLookupRepositoryInterface;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.entity.hibernate.HibernateUtil;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.CommonLookupRepository;

/**
 * Repository to lookup Purchase Orders.
 */
public class PurchaseOrderLookupRepository extends CommonLookupRepository implements PurchaseOrderLookupRepositoryInterface {

    @SuppressWarnings("unused")
    private static final String MODULE = PurchaseOrderLookupRepository.class.getName();

    private String orderId;
    private String productPattern;
    private String statusId;
    private String orderName;
    private String organizationPartyId;
    private String createdBy;
    private String supplierPartyId;
    private Timestamp fromDate;
    private String fromDateStr;
    private Timestamp thruDate;
    private String thruDateStr;
    private boolean findDesiredOnly = false;
    private Locale locale;
    private TimeZone timeZone;
    private List<String> orderBy;

    private ProductRepositoryInterface productRepository;

    /**
     * Default constructor.
     */
    public PurchaseOrderLookupRepository() {
        super();
    }

    /** {@inheritDoc} */
    public List<OrderViewForListing> findOrders() throws RepositoryException {

        // convert fromDateStr / thruDateStr into Timestamps if the string versions were given
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            fromDate = UtilDate.toTimestamp(fromDateStr, timeZone, locale);
        }
        if (UtilValidate.isNotEmpty(thruDateStr)) {
            thruDate = UtilDate.toTimestamp(thruDateStr, timeZone, locale);
        }
        Session session = null;
        try {
            // get a hibernate session
            session = getInfrastructure().getSession();
            Criteria criteria = session.createCriteria(OrderHeader.class);

            // always filter by the current organization
            criteria.add(Restrictions.eq(OrderHeader.Fields.billToPartyId.name(), organizationPartyId));

            // filters by order type, we only want purchase order
            criteria.add(Restrictions.eq(OrderHeader.Fields.orderTypeId.name(), OrderTypeConstants.PURCHASE_ORDER));

            // set the from/thru date filter if they were given
            if (fromDate != null) {
                criteria.add(Restrictions.ge(OrderHeader.Fields.orderDate.name(), fromDate));
            }
            if (thruDate != null) {
                criteria.add(Restrictions.le(OrderHeader.Fields.orderDate.name(), thruDate));
            }

            // filter the role assoc, there is only one supplier role per order
            Criteria roleCriteria = criteria.createAlias("orderRoles", "or");
            roleCriteria.add(Restrictions.eq("or.id." + OrderRole.Fields.roleTypeId.name(), RoleTypeConstants.BILL_FROM_VENDOR));

            // filter by order status
            if (findDesiredOnly) {
                List<String> statuses = UtilMisc.toList(StatusItemConstants.OrderStatus.ORDER_APPROVED, StatusItemConstants.OrderStatus.ORDER_CREATED, StatusItemConstants.OrderStatus.ORDER_HOLD);
                criteria.add(Restrictions.in(OrderHeader.Fields.statusId.name(), statuses));
            }

            // filter by the given orderId string
            if (UtilValidate.isNotEmpty(orderId)) {
                criteria.add(Restrictions.ilike(OrderHeader.Fields.orderId.name(), orderId, MatchMode.START));
            }

            // filter by exact matching status, if a statusId was given
            if (UtilValidate.isNotEmpty(statusId)) {
                criteria.add(Restrictions.eq(OrderHeader.Fields.statusId.name(), statusId));
            }

            // filter by the user who created the order if given
            if (UtilValidate.isNotEmpty(createdBy)) {
                criteria.add(Restrictions.eq(OrderHeader.Fields.createdBy.name(), createdBy));
            }

            // filter by the given orderName string
            if (UtilValidate.isNotEmpty(orderName)) {
                criteria.add(Restrictions.ilike(OrderHeader.Fields.orderName.name(), orderName, MatchMode.START));
            }

            // filter by the given supplierPartyId string, from the OrderRole entity
            if (UtilValidate.isNotEmpty(supplierPartyId)) {
                roleCriteria.add(Restrictions.ilike("or.id." + OrderRole.Fields.partyId.name(), supplierPartyId, MatchMode.START));
            }

            // filter by product, if given
            criteria.createAlias("orderItems", "oi");
            if (UtilValidate.isNotEmpty(productPattern)) {
                try {
                    // try to get product by using productPattern as productId
                    Product product = getProductRepository().getProductById(productPattern);
                    criteria.add(Restrictions.eq("oi." + OrderItem.Fields.productId.name(), product.getProductId()));
                } catch (EntityNotFoundException e) {
                    // could not get the product by using productPattern as productId
                    // find all the products that may match
                    String likePattern = "%" + productPattern + "%";
                    EntityCondition conditionList = EntityCondition.makeCondition(EntityOperator.OR,
                        EntityCondition.makeCondition(ProductAndGoodIdentification.Fields.productId.getName(), EntityOperator.LIKE, likePattern),
                        EntityCondition.makeCondition(ProductAndGoodIdentification.Fields.internalName.getName(), EntityOperator.LIKE, likePattern),
                        EntityCondition.makeCondition(ProductAndGoodIdentification.Fields.productName.getName(), EntityOperator.LIKE, likePattern),
                        EntityCondition.makeCondition(ProductAndGoodIdentification.Fields.comments.getName(), EntityOperator.LIKE, likePattern),
                        EntityCondition.makeCondition(ProductAndGoodIdentification.Fields.description.getName(), EntityOperator.LIKE, likePattern),
                        EntityCondition.makeCondition(ProductAndGoodIdentification.Fields.longDescription.getName(), EntityOperator.LIKE, likePattern),
                        EntityCondition.makeCondition(ProductAndGoodIdentification.Fields.idValue.getName(), EntityOperator.LIKE, likePattern)
                    );
                    List<ProductAndGoodIdentification> products = findList(ProductAndGoodIdentification.class, conditionList);
                    if (products.size() > 0) {
                        criteria.add(Restrictions.in("oi." + OrderItem.Fields.productId.name(), Entity.getDistinctFieldValues(products, ProductAndGoodIdentification.Fields.productId)));
                    }
                }
            }

            // specify the fields to return
            criteria.setProjection(Projections.projectionList()
                                   .add(Projections.distinct(Projections.property(OrderHeader.Fields.orderId.name())))
                                   .add(Projections.property(OrderHeader.Fields.orderName.name()))
                                   .add(Projections.property(OrderHeader.Fields.statusId.name()))
                                   .add(Projections.property(OrderHeader.Fields.grandTotal.name()))
                                   .add(Projections.property(OrderHeader.Fields.orderDate.name()))
                                   .add(Projections.property(OrderHeader.Fields.currencyUom.name()))
                                   .add(Projections.property("or.id." + OrderRole.Fields.partyId.name())));

            // set the order by
            if (orderBy == null) {
                orderBy = Arrays.asList(OrderHeader.Fields.orderDate.desc());
            }
            // some substitution is needed to fit the hibernate field names
            // this also maps the calculated fields and indicates the non sortable fields
            Map<String, String> subs = new HashMap<String, String>();
            subs.put("partyId", "or.id.partyId");
            subs.put("partyName", "or.id.partyId");
            subs.put("orderDateString", "orderDate");
            subs.put("orderNameId", "orderId");
            subs.put("statusDescription", "statusId");
            HibernateUtil.setCriteriaOrder(criteria, orderBy, subs);

            ScrollableResults results = null;
            List<OrderViewForListing> results2 = new ArrayList<OrderViewForListing>();
            try {
                // fetch the paginated results
                results = criteria.scroll(ScrollMode.SCROLL_INSENSITIVE);
                if (usePagination()) {
                    results.setRowNumber(getPageStart());
                } else {
                    results.first();
                }

                // convert them into OrderViewForListing objects which will also calculate or format some fields for display
                Object[] o = results.get();
                int n = 0; // number of results actually read
                while (o != null) {
                    OrderViewForListing r = new OrderViewForListing();
                    r.initRepository(this);
                    int i = 0;
                    r.setOrderId((String) o[i++]);
                    r.setOrderName((String) o[i++]);
                    r.setStatusId((String) o[i++]);
                    r.setGrandTotal((BigDecimal) o[i++]);
                    r.setOrderDate((Timestamp) o[i++]);
                    r.setCurrencyUom((String) o[i++]);
                    r.setPartyId((String) o[i++]);
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
        }  finally {
            if (session != null) {
                session.close();
            }
        }
    }

    protected ProductRepositoryInterface getProductRepository() throws RepositoryException {
        if (productRepository == null) {
            productRepository = DomainsDirectory.getDomainsDirectory(this).getProductDomain().getProductRepository();
        }
        return productRepository;
    }

    /** {@inheritDoc} */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /** {@inheritDoc} */
    public void setOrderBy(List<String> orderBy) {
        this.orderBy = orderBy;
    }

    /** {@inheritDoc} */
    public void setSupplierPartyId(String supplierPartyId) {
        this.supplierPartyId = supplierPartyId;
    }

    /** {@inheritDoc} */
    public void setFromDate(String fromDate) {
        this.fromDateStr = fromDate;
    }

    /** {@inheritDoc} */
    public void setFromDate(Timestamp fromDate) {
        this.fromDate = fromDate;
    }

    /** {@inheritDoc} */
    public void setThruDate(String thruDate) {
        this.thruDateStr = thruDate;
    }

    /** {@inheritDoc} */
    public void setThruDate(Timestamp thruDate) {
        this.thruDate = thruDate;
    }

    /** {@inheritDoc} */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /** {@inheritDoc} */
    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    /** {@inheritDoc} */
    public void setProductPattern(String productPattern) {
        this.productPattern = productPattern;
    }

    /** {@inheritDoc} */
    public void setOrderName(String orderName) {
        this.orderName = orderName;
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

    /** {@inheritDoc} */
    public void setFindDesiredOnly(boolean findDesiredOnly) {
        this.findDesiredOnly = findDesiredOnly;
    }
}
