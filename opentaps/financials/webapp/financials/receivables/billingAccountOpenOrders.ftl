<#--
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
 *  
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<table width="100%">
    <tr>
       <td class="tableheadtext">${uiLabelMap.OrderDate}</td>
       <td class="tableheadtext">${uiLabelMap.OrderOrder}</td>
       <td class="tableheadtext" width="30%">${uiLabelMap.OrderOrderName}</td>
       <td class="tableheadtext" align="right">${uiLabelMap.OrderGrandTotal}</td>
       <td class="tableheadtext" align="right">${uiLabelMap.FinancialsCustomerBillingAccountOpenOrdersTotal}</td>
       <td class="tableheadtext" align="right">${uiLabelMap.AccountingBillingAvailableBalance}</td>
    </tr>
    <#assign openOrdersTotal = 0.0>
    <#assign accountLimit = billingAccount.accountLimit>    
    <#assign netBalance = Static['org.ofbiz.accounting.payment.BillingAccountWorker'].getBillingAccountNetBalance(delegator, billingAccountId)>    

    <#-- we're going to use the account limit - net balance - open orders = available balance and recompute it here
    note that we're not doing any currency conversions here -- we're not too choosy about currency because were assuming they're all the same .... for now
    TODO: fix currency conversions -->
    <#list billAcctOpenOrders as openOrder>
       <#assign openOrdersTotal = openOrdersTotal + openOrder.grandTotal/>
       <#assign availableBalance = accountLimit - netBalance - openOrdersTotal>
    <tr>
       <td class="tabletext">${getLocalizedDate(openOrder.orderDate)}</td> 
       <td class="tabletext"><a href="/ordermgr/control/orderview?orderId=${openOrder.orderId}&externalLoginKey=${externalLoginKey}">${openOrder.orderId}</a></td> 
       <td class="tabletext">${openOrder.orderName?default("N/A")}</td> 
       <td class="tabletextright"><@ofbizCurrency amount=openOrder.grandTotal isoCode=openOrder.currencyUomId/></td> 
       <td class="tabletextright"><@ofbizCurrency amount=openOrdersTotal isoCode=openOrder.currencyUomId/></td> 
       <td class="tabletextright"><@ofbizCurrency amount=availableBalance isoCode=openOrder.currencyUomId/></td> 
    </tr>
   </#list>
</table>

