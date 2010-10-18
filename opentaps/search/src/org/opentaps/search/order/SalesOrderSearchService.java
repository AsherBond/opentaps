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
package org.opentaps.search.order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.OpentapsConfigurationTypeConstants;
import org.opentaps.base.constants.OrderTypeConstants;
import org.opentaps.base.constants.SecurityPermissionConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.OrderHeader;
import org.opentaps.base.entities.OrderRole;
import org.opentaps.base.entities.OrderRolePk;
import org.opentaps.base.entities.PartyRelationshipAndPermission;
import org.opentaps.base.entities.UserLogin;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.search.SearchDomainInterface;
import org.opentaps.domain.search.SearchRepositoryInterface;
import org.opentaps.domain.search.SearchResult;
import org.opentaps.domain.search.order.SalesOrderSearchServiceInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.search.HibernateSearchRepository;
import org.opentaps.search.HibernateSearchService;
import org.opentaps.search.party.PartySearch;

/**
 * The implementation of the Sales Order search service.
 */
public class SalesOrderSearchService extends HibernateSearchService implements SalesOrderSearchServiceInterface {
    
    private static final String MODULE = SalesOrderSearchService.class.getName();

    /** Common set of class to query for Party related search. */
    public static final Set<Class<?>> ORDER_CLASSES = new HashSet<Class<?>>(Arrays.asList(
            OrderRole.class
        ));

    private List<Order> orders = new ArrayList<Order>();

    /** {@inheritDoc} */
    public List<Order> getOrders() {
        return orders;
    }

    /** {@inheritDoc} */
    public String getQueryString() {
        StringBuilder sb = new StringBuilder();
        // only find sales order
        sb.append("( +orderHeader.").append(OrderHeader.Fields.orderTypeId.name()).append(":\"").append(OrderTypeConstants.SALES_ORDER).append("\"");
        // filter canceled orders
        sb.append(" -orderHeader.").append(OrderHeader.Fields.statusId.name()).append(":\"").append(StatusItemConstants.OrderStatus.ORDER_CANCELLED).append("\"");
        sb.append(" +(");
        makeSalesOrderQuery(sb);
        PartySearch.makePartyGroupFieldsQuery(sb);
        PartySearch.makePersonFieldsQuery(sb);
        sb.append("))");
        return sb.toString();
    }

    /**
     * Builds the Lucene query for a SalesOrder related search.
     * @param sb the string builder instance currently building the query
     */
    public static void makeSalesOrderQuery(StringBuilder sb) {
        for (String f : Arrays.asList(OrderHeader.Fields.orderId.name(), OrderHeader.Fields.orderName.name())) {
            sb.append("orderHeader.").append(f).append(":").append(HibernateSearchRepository.DEFAULT_PLACEHOLDER).append(" ");
        }
    }

    /** {@inheritDoc} */
    public Set<Class<?>> getClassesToQuery() {
        return ORDER_CLASSES;
    }

    /** {@inheritDoc} */
    public void search() throws ServiceException {
        try {
            SearchDomainInterface searchDomain = getDomainsDirectory().getSearchDomain();
            SearchRepositoryInterface searchRepository = searchDomain.getSearchRepository();
            search(searchRepository);
        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public void readSearchResults(List<SearchResult> results) throws ServiceException {

        try {
            OrderRepositoryInterface orderRepository = getDomainsDirectory().getOrderDomain().getOrderRepository();
            // get the entities from the search results
            Set<String> orderIds = new HashSet<String>();
            for (SearchResult o : results) {
                Class<?> c = o.getResultClass();
                if (c.equals(OrderRole.class)) {
                    OrderRolePk pk = (OrderRolePk) o.getResultPk();
                                        
                    if("Y".equals(this.getInfrastructure().getConfigurationValue(OpentapsConfigurationTypeConstants.CRMSFA_FIND_SEC_FILTER))){                            
                        // Checking security if user has permisson to view sales order.                            
                        if(this.hasUserSecurityPermissionToViewSalesOrder(orderRepository, pk.getPartyId()) == true){                                         
                            orderIds.add(pk.getOrderId());                       
                        }                            
                    }else{
                        orderIds.add(pk.getOrderId());
                    }
                }
            }

            // apply pagination here
            setResultSize(orderIds.size());
            if (!orderIds.isEmpty()) {
                if (!usePagination()) {
                    orders = orderRepository.findList(Order.class, EntityCondition.makeCondition(Order.Fields.orderId.name(), EntityOperator.IN, orderIds));
                } else if (getPageStart() < orderIds.size() && getPageSize() > 0) {
                    orders = orderRepository.findPage(Order.class, EntityCondition.makeCondition(Order.Fields.orderId.name(), EntityOperator.IN, orderIds), getPageStart(), getPageSize());
                } else {
                    orders = new ArrayList<Order>();
                }
            } else {
                orders = new ArrayList<Order>();
            }
        } catch (InfrastructureException e) {
            throw new ServiceException(e);
        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }
    }

    private boolean hasUserSecurityPermissionToViewSalesOrder(RepositoryInterface repository, String orderPartyId) throws RepositoryException {
        boolean has = false;
        
        String userPartyId = this.getUser().getOfbizUserLogin().getString(UserLogin.Fields.partyId.name());
                            
        EntityCondition permissionCond = EntityCondition.makeCondition(EntityOperator.AND,
            EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.partyIdFrom.name(), EntityOperator.EQUALS, orderPartyId),
            EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.partyIdTo.name(), EntityOperator.EQUALS, userPartyId),
            EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.permissionId.name(), EntityOperator.EQUALS, SecurityPermissionConstants.CRMSFA_ORDER_VIEW),
            EntityCondition.makeCondition(EntityOperator.OR,
                    EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.fromDate.name(), EntityOperator.EQUALS, null),
                    EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.fromDate.name(), EntityOperator.LESS_THAN, UtilDateTime.nowTimestamp())
                    ),
            EntityCondition.makeCondition(EntityOperator.OR,
                    EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.thruDate.name(), EntityOperator.EQUALS, null),
                    EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.thruDate.name(), EntityOperator.GREATER_THAN, UtilDateTime.nowTimestamp())
                    )
        );

        List<PartyRelationshipAndPermission> permission = repository.findList(PartyRelationshipAndPermission.class, permissionCond);

        if(permission.size() > 0){         

            has = true;

            Debug.logInfo("User ["+userPartyId+"] has security permission to view sales order of party ["+orderPartyId+"].", MODULE);

        }else{

            Debug.logInfo("User ["+userPartyId+"] has not passed security to view sales order of party ["+orderPartyId+"].", MODULE);

        }

        return has;
    }
}
