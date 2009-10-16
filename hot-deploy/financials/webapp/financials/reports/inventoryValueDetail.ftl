<#--
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
 *  
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#assign currencyUomId = parameters.orgCurrencyUomId>  <!-- for some reason, putting this in context in main-decorator.bsh does not work -->

<form method="POST" name="" action="${formTarget?if_exists}">
  <#-- some forms need a productId, they should define this value -->
  <#-- TODO: use a macro to generate filter options like in product lookup -->
  <#if productIdInputRequested?exists>
  <div style="margin-left: 5px; margin-bottom: 5px;">
    <span class="tableheadtext">${uiLabelMap.ProductProductId}</span>
    <input type="text" name="productId" size="20" maxlength="20" value="${parameters.productId?if_exists}" class="inputBox"></input>
  </div>
  </#if>

  <div style="margin-left: 30px; margin-top: 10px;">
    <input type="Submit" class="smallSubmit" name="submitButton" value="${uiLabelMap.CommonRun}"></input>
    <#if returnPage?exists && returnLabel?exists>
    <input type="button" class="smallSubmit" name="backButton" value="${uiLabelMap.get(returnLabel)}" onClick="window.location.href='/financials/control/${returnPage}'"></input>
    </#if>
  </div>  
</form>

<div style="border: 1px solid #999999; margin-top: 20px; margin-bottom: 20px;"></div>
<table>
  <tr><td colspan="7" class="tableheadtext" align="center">${uiLabelMap.FinancialsInventoryValueDetail} for ${parameters.organizationName?if_exists} (${organizationPartyId}) <br/>
  ${uiLabelMap.OpentapsAsOfDate} ${getLocalizedDate(Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp(), "DATE")}</td></tr>
  <tr><td colspan="7">&nbsp;</td></tr>
  <tr>
    <td class="tableheadtext">Inventory Item</td>
    <td class="tableheadtext">Product</td>
    <td class="tableheadtext">Serial No.</td>
    <td class="tableheadtext" align="right">QOH</td>
    <td class="tableheadtext" align="right">Unit Cost</td>
    <td class="tableheadtext" align="right">${uiLabelMap.FinancialsValue}</td>
    <td class="tableheadtext" align="right">${uiLabelMap.FinancialsCumulativeValue}</td>
  </tr>

<#assign total = 0/>
<#list inventoryValueDetail.keySet() as inventoryItem>
<#assign total = total + inventoryValueDetail(inventoryItem)/>
<#if inventoryItem.inventoryItemTypeId == "SERIALIZED_INV_ITEM">
  <#assign quantityOnHandTotal = 1/>
<#else>
  <#assign quantityOnHandTotal = inventoryItem.quantityOnHandTotal/>
</#if>
  <tr>
    <td class="tabletext">${inventoryItem.inventoryItemId}</td>
    <td class="tabletext"><a href="<@ofbizUrl>/AccountActivitiesDetail?productId=${inventoryItem.productId}&organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="linktext">${inventoryItem.productId}</a></td>
    <td class="tabletext"><#if "SERIALIZED_INV_ITEM" == inventoryItem.inventoryItemTypeId>${inventoryItem.serialNumber?if_exists}<#else>Non-Serialized</#if></td>
    <td class="tabletext" align="right">${quantityOnHandTotal}</td>
    <td class="tabletext" align="right"><@ofbizCurrency amount=inventoryItem.unitCost isoCode=currencyUomId/></td>
    <td class="tabletext" align="right"><@ofbizCurrency amount=inventoryValueDetail(inventoryItem) isoCode=currencyUomId/></td>
    <td class="tabletext" align="right"><@ofbizCurrency amount=total isoCode=currencyUomId/></td>
  </tr>
</#list>
<tr><td colspan="7"><hr/></td></tr>
<tr>
  <td colspan="3" class="tableheadtext">Inventory Items Sub-Total</td><td colspan="4" class="tableheadtext" align="right"><@ofbizCurrency amount=total isoCode=currencyUomId/></td>
</tr>

<#-- also display the inventory average cost valuation account balances -->
<#if adjustmentTransactions?exists>
  <tr><td colspan="7">&nbsp;</td></tr>
  <tr><td colspan="7" align="center" class="tableheadtext">Adjustments</td></tr>
  <tr>
    <td class="tableheadtext">Inventory Item</td>
    <td class="tableheadtext">Product</td>
    <td class="tableheadtext" colspan="4">Account</td>
    <td class="tableheadtext" align="right">Amount</td>
  </tr>

<#list adjustmentTransactions as trans>
  <#assign account = trans.getRelatedOne("GlAccount")>
  <#if trans.debitCreditFlag == "C">
    <#assign amount = -trans.amount>
  <#else>
    <#assign amount = trans.amount>
  </#if>

  <tr>
    <td class="tabletext"><#if trans.inventoryItemId?has_content>${trans.inventoryItemId}</#if></td>
    <td class="tabletext"><a href="<@ofbizUrl>/AccountActivitiesDetail?productId=${trans.productId}&organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="linktext">${trans.productId}</a></td>
    <td class="tabletext" colspan="4"><a href="<@ofbizUrl>/AccountActivitiesDetail?glAccountId=${account.glAccountId}&productId=${trans.productId}</@ofbizUrl>" class="linktext">${account.accountCode}</a> ${account.accountName}</td>
    <td class="tabletext" align="right"><@ofbizCurrency amount=amount isoCode=currencyUomId/></td>
    <#assign total = total + amount/>
  </tr>
</#list>

<tr><td colspan="7"><hr/></td></tr>
<tr>
  <td colspan="6" class="tableheadtext">Total Inventory Value</td><td colspan="1" class="tableheadtext" align="right"><@ofbizCurrency amount=total isoCode=currencyUomId/></td>
</tr>
</#if>

</tr>
</table>
