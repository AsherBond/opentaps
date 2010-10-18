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
-->

<#-- Orders statistic -->
<#if hasOrdersViewPermissions && chartImage?has_content>
<div style="float: right; " align="right">
  <img src="<@ofbizUrl>showChart?chart=${chartImage?html}</@ofbizUrl>" style="margin-right: 15px; "/>
</div>
</#if>

<div>
  <table class="headedTable">

    <#-- Requirements statistic -->
    <#if hasRequirementsViewPermissions>
      <tr class="header"><td colspan="2">${uiLabelMap.PurchHomeRequirements}</td></tr>
      <#-- Number of New Purchasing Requirements -->
      <#if totalCreatedPurchasingRequirements?exists>
        <tr>
          <td><a href="<@ofbizUrl>openRequirements?requirementTypeId=PRODUCT_REQUIREMENT</@ofbizUrl>">${uiLabelMap.PurchNumPurchasingRequirementsCreated}</a>:</td>
          <td>${totalCreatedPurchasingRequirements}</td>
        </tr>
      </#if>
      <#-- Number of New Production Requirements -->
      <#if totalCreatedProductionRequirements?exists>
        <tr>
          <td><a href="<@ofbizUrl>openRequirements?groupedRequirementTypeId=INTERNAL</@ofbizUrl>">${uiLabelMap.PurchNumProductionRequirementsCreated}</a>:</td>
          <td>${totalCreatedProductionRequirements}</td>
        </tr>
      </#if>
      <#-- Number of New Transfer Requirements -->
      <#if totalNewTransferRequirements?exists>
        <tr>
          <td><a href="<@ofbizUrl>openRequirements?requirementTypeId=TRANSFER_REQUIREMENT</@ofbizUrl>">${uiLabelMap.PurchNumNewTransferRequirements}</a>:</td>
          <td>${totalNewTransferRequirements}</td>
        </tr>
      </#if>
      <#-- Number of Approved Purchasing Requirements -->
      <#if totalApprovedVendorRequirements?exists>
        <tr>
          <td><a href="<@ofbizUrl>ApprovedProductRequirementsByVendor</@ofbizUrl>">${uiLabelMap.PurchNumPurchasingRequirementsApproved}</a>:</td>
          <td>${totalApprovedVendorRequirements}</td>
        </tr>
      </#if>
      <#-- Number of Approved Production Requirements -->
      <#if totalApprovedInternalRequirements?exists>
        <tr>
          <td><a href="<@ofbizUrl>ApprovedInternalRequirements</@ofbizUrl>">${uiLabelMap.PurchNumInternalRequirementsApproved}</a>:</td>
          <td>${totalApprovedInternalRequirements}</td>
        </tr>
      </#if>
      <#-- Number of Approved Transfer Requirements -->
      <#if totalApprovedTransferRequirements?exists>
        <tr>
          <td><a href="<@ofbizUrl>ApprovedTransferRequirements</@ofbizUrl>">${uiLabelMap.PurchNumTransferRequirementsApproved}</a>:</td>
          <td>${totalApprovedTransferRequirements}</td>
        </tr>
      </#if>
      <tr><td>&nbsp;</td></tr>
    </#if>

    <#-- Orders statistic -->
    <#if hasOrdersViewPermissions>
    <tr class="header"><td colspan="2">${uiLabelMap.OpentapsPurchaseOrders}</td></tr>
    <tr>
        <td><a href="<@ofbizUrl>findOrders?statusId=ORDER_CREATED&performFind=Y</@ofbizUrl>">${uiLabelMap.PurchNumOrdersCreated}</a>:</td>
        <td>${numberCreatedPoOrders}</td>
     </tr>
    <tr>
        <td><a href="<@ofbizUrl>findOrders?statusId=ORDER_APPROVED&performFind=Y</@ofbizUrl>">${uiLabelMap.PurchNumOrdersApproved}</a>:</td>
        <td>${numberApprovedPoOrders}</td>
    </tr>
    <tr>
        <td><a href="<@ofbizUrl>findOrders?statusId=ORDER_HOLD&performFind=Y</@ofbizUrl>">${uiLabelMap.PurchNumOrdersHeld}</a>:</td>
        <td>${numberHeldPoOrders}</td>
     </tr>
     </#if>
</table>
</div>
