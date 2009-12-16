/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javolution.util.FastList;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.OrderTypeConstants;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.OrderHeaderAndItems;
import org.opentaps.base.entities.OrderHeaderAndRoles;
import org.opentaps.base.entities.OrderHeaderItemAndRolesAndInvPending;
import org.opentaps.base.entities.ProductAndGoodIdentification;
import org.opentaps.common.util.UtilDate;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.domain.search.order.PurchaseOrderSearchRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.entity.util.EntityComparator;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

/**
 * This is the implement of the Purchase Order search interface.
 */
public class PurchaseOrderSearchRepository  extends Repository implements PurchaseOrderSearchRepositoryInterface {

    @SuppressWarnings("unused")
    private static final String MODULE = PurchaseOrderSearchRepository.class.getName();

    private String orderId;
    private String productPattern;
    private String statusId;
    private String orderName;
    private String organizationPartyId;
    private String createdBy;
    private String supplierPartyId;
    private String fromDate;
    private String thruDate;
    private boolean findDesiredOnly = false;
    private Locale locale;
    private TimeZone timeZone;
    private List<String> orderBy;

    private ProductRepositoryInterface productRepository;
    /**
     * Default constructor.
     */
    public PurchaseOrderSearchRepository() {
        super();
    }

    /** {@inheritDoc} */
    public List<OrderViewForListing> findOrders() throws RepositoryException {
        List<EntityCondition> searchConditions = FastList.newInstance();

        // convert fromDate into Timestamp and construct EntityExpr for searching
        if (UtilValidate.isNotEmpty(fromDate)) {
            Timestamp fromDateTimestamp = UtilDate.toTimestamp(fromDate, timeZone, locale);
            if (UtilValidate.isNotEmpty(fromDateTimestamp)) {
                searchConditions.add(EntityCondition.makeCondition(OrderHeaderAndRoles.Fields.orderDate.name(), EntityOperator.GREATER_THAN_EQUAL_TO, fromDateTimestamp));
            }
        }

        // convert thruDate into Timestamp and construct EntityExpr for searching
        if (UtilValidate.isNotEmpty(thruDate)) {
            Timestamp thruDateTimestamp = UtilDate.toTimestamp(thruDate, timeZone, locale);
            if (UtilValidate.isNotEmpty(thruDateTimestamp)) {
                searchConditions.add(EntityCondition.makeCondition(OrderHeaderAndRoles.Fields.orderDate.name(), EntityOperator.LESS_THAN_EQUAL_TO, thruDateTimestamp));
            }
        }

        if (UtilValidate.isNotEmpty(orderId)) {
            searchConditions.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(OrderHeaderAndRoles.Fields.orderId.name()), EntityOperator.LIKE, EntityFunction.UPPER(orderId + "%")));
        }

        if (UtilValidate.isNotEmpty(orderName)) {
            searchConditions.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(OrderHeaderAndRoles.Fields.orderName.name()), EntityOperator.LIKE, EntityFunction.UPPER(orderName + "%")));
        }

        if (UtilValidate.isNotEmpty(supplierPartyId)) {
            searchConditions.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(OrderHeaderAndRoles.Fields.partyId.name()), EntityOperator.LIKE, EntityFunction.UPPER(supplierPartyId + "%")));
        }

        if (UtilValidate.isNotEmpty(statusId)) {
            searchConditions.add(EntityCondition.makeCondition(OrderHeaderAndRoles.Fields.statusId.name(), EntityOperator.EQUALS, statusId));
        }

        if (UtilValidate.isNotEmpty(createdBy)) {
            searchConditions.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(OrderHeaderAndRoles.Fields.createdBy.name()), EntityOperator.EQUALS, EntityFunction.UPPER(createdBy)));
        }

        if (UtilValidate.isNotEmpty(productPattern)) {
            addProductPatternSearchCondition(searchConditions);
        }

        // restrict search results to the company set up for PURCHASING
        searchConditions.add(EntityCondition.makeCondition(OrderHeaderAndRoles.Fields.billToPartyId.name(), EntityOperator.EQUALS, organizationPartyId));


        // other conditions to limit the list
        searchConditions.add(EntityCondition.makeCondition(OrderHeaderAndRoles.Fields.orderTypeId.name(), EntityOperator.EQUALS, OrderTypeConstants.PURCHASE_ORDER));
        searchConditions.add(EntityCondition.makeCondition(OrderHeaderAndRoles.Fields.roleTypeId.name(), EntityOperator.EQUALS, RoleTypeConstants.BILL_FROM_VENDOR));

        if (findDesiredOnly) {
            searchConditions.add(EntityCondition.makeCondition(OrderHeaderItemAndRolesAndInvPending.Fields.statusId.name(), EntityOperator.IN, 
                    UtilMisc.toList(
                            StatusItemConstants.OrderStatus.ORDER_APPROVED,
                            StatusItemConstants.OrderStatus.ORDER_CREATED,
                            StatusItemConstants.OrderStatus.ORDER_HOLD)
            ));
        }
        EntityCondition searchConditionList = EntityCondition.makeCondition(searchConditions, EntityOperator.AND);


        // must specify minimum required fields so that the distinct select works
        List<String> fieldsToSelect = FastList.newInstance();
        fieldsToSelect.add(OrderHeaderAndRoles.Fields.orderId.name());
        fieldsToSelect.add(OrderHeaderAndRoles.Fields.orderDate.name());
        fieldsToSelect.add(OrderHeaderAndRoles.Fields.orderName.name());
        fieldsToSelect.add(OrderHeaderAndRoles.Fields.partyId.name());
        fieldsToSelect.add(OrderHeaderAndRoles.Fields.statusId.name());
        fieldsToSelect.add(OrderHeaderAndRoles.Fields.grandTotal.name());
        fieldsToSelect.add(OrderHeaderAndRoles.Fields.currencyUom.name());

        List<String> queryOrderBy = new ArrayList<String>();
        if (orderBy == null) {
            orderBy = Arrays.asList(OrderHeaderAndRoles.Fields.orderDate.desc());
        }
        List<OrderHeaderAndRoles> orders = findList(OrderHeaderAndRoles.class, searchConditionList, fieldsToSelect, queryOrderBy);

        // add value for extra fields
        List<OrderViewForListing> results = OrderViewForListing.makeOrderView(orders, getDelegator(), timeZone, locale);
        Collections.sort(results, new EntityComparator(orderBy));
        return results;
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
        this.fromDate = fromDate;
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
    public void setThruDate(String thruDate) {
        this.thruDate = thruDate;
    }

    /** {@inheritDoc} */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /** {@inheritDoc} */
    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    protected ProductRepositoryInterface getProductRepository() throws RepositoryException {
        if (productRepository == null) {
            productRepository = DomainsDirectory.getDomainsDirectory(this).getProductDomain().getProductRepository();
        }
        return productRepository;
    }


    /**
     * Add product pattern search condition to searchConditions.
     * @param searchConditions a <code>List<EntityCondition> searchConditions</code> value
     * @throws RepositoryException if error occur
     */
    private void addProductPatternSearchCondition(List<EntityCondition> searchConditions) throws RepositoryException {
        List<EntityCondition> productSearchConditions = FastList.newInstance();
        try {
            // try to get product by using productPattern as productId
            Product product = getProductRepository().getProductById(productPattern);
            productSearchConditions.add(EntityCondition.makeCondition(OrderHeaderAndItems.Fields.productId.name(), EntityOperator.EQUALS, product.getProductId()));
        } catch (EntityNotFoundException e) {
            // cannot get the product by using productPattern as productId
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
                productSearchConditions.add(EntityCondition.makeCondition(OrderHeaderAndItems.Fields.productId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(products, ProductAndGoodIdentification.Fields.productId)));
            }
        }
        if (productSearchConditions.size() > 0) {
            // find all OrderHeaderAndItems records that match the given productSearchConditions
            List<OrderHeaderAndItems> orders = findList(OrderHeaderAndItems.class, productSearchConditions);
            searchConditions.add(EntityCondition.makeCondition(OrderHeaderAndRoles.Fields.orderId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(orders, OrderHeaderAndItems.Fields.orderId)));
        }
    }


    /** {@inheritDoc} */
    public void setFindDesiredOnly(boolean findDesiredOnly) {
        this.findDesiredOnly = findDesiredOnly;
    }
}
