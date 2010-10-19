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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#assign formAction="outsourceRoutingTask">

<form action="<@ofbizUrl>${formAction}</@ofbizUrl>" method="post" name="${formAction}">
<table class="twoColumnForm">

    <@inputAutoCompleteSupplierRow title=uiLabelMap.ProductSupplier name="partyId" titleClass="requiredField" id="outsourceRoutingTaskSupplierId" />
    <@inputLookupRow title=uiLabelMap.ManufacturingRoutingTaskId name="workEffortId" titleClass="requiredField" form=formAction lookup="LookupRoutingTask" />
    <@inputTextRow title=uiLabelMap.ProductProductId name="productId" size=20 maxlength=20 />
    <@inputTextRow title=uiLabelMap.ProductProductName name="productName" maxlength=100 />
   <tr>
    <td class="titleCell"><span class="requiredField">${uiLabelMap.ProductAvailableFromDate}</span></td>
    <td>
    <@inputDateTime name="availableFromDate" form=formAction default=nowTimestamp/>
    <b>${uiLabelMap.CommonThru}</b>
    <@inputDateTime name="availableThruDate" form=formAction />
    </td>
   </tr>
    <@inputTextRow title=uiLabelMap.ProductMinimumOrderQuantity name="minimumOrderQuantity" size=6 titleClass="requiredField" default="0"/>
    <@inputTextRow title=uiLabelMap.ProductSupplierProductId name="supplierProductId" size=20 maxlength=20 titleClass="requiredField" />
    <@inputTextRow title=uiLabelMap.ProductSupplierProductName name="supplierProductName" maxlength=100 />
    <@inputCurrencyRow title=uiLabelMap.ProductPrice name="lastPrice" titleClass="requiredField" />
    <@inputSelectRow title=uiLabelMap.FormFieldTitle_costGlAccountTypeId name="glAccountTypeId" list=glAccountTypes displayField="description" default="MFG_EXPENSE_CONTRACT" />
    <@inputSubmitRow title=uiLabelMap.CommonCreate />

</table>
</form>
