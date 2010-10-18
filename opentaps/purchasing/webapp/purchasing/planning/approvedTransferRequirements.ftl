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

<form method="get" name="ApprovedTransferRequirements" action="<@ofbizUrl>ApprovedTransferRequirements</@ofbizUrl>">
  <div class="subSectionBlock">
    <table class="twoColumnForm">
      <@inputAutoCompleteProductRow title=uiLabelMap.ProductProduct name="productId" form="ApprovedTransferRequirements"/>
      <@inputSelectRow name="facilityId" title=uiLabelMap.CommonFrom list=facilities key="facilityId" displayField="facilityName" required=false />
      <@inputSelectRow name="facilityIdTo" title=uiLabelMap.CommonTo list=facilities key="facilityId" displayField="facilityName" required=false />
      <@inputSubmitRow title=uiLabelMap.CommonFind />
    </table>
  </div>
</form>

<form method="post" name="transferRequirementForm" action="<@ofbizUrl>createInventoryTransfersFromPendingTransferRequirements</@ofbizUrl>">
  <table class="listTable">
    <tr class="listTableHeader">
      <td>${uiLabelMap.ProductProduct}</td>
      <td>${uiLabelMap.CommonQuantity}</td>
      <td>${uiLabelMap.CommonFrom}</td>
      <td>${uiLabelMap.CommonTo}</td>
      <td>${uiLabelMap.CommonStartDateTime}</td>
      <td><@inputMultiSelectAll form="transferRequirementForm"/></td>
    </tr>

    <#list requirementList as requirement>
      <tr class="${tableRowClass(requirement_index)}">
        <@inputHidden name="requirementId" value=requirement.requirementId index=requirement_index />
        <@displayCell text=requirement.productId />
        <@displayCell text=requirement.quantity />
        <@inputSelectCell name="facilityId" list=facilities key="facilityId" displayField="facilityName" index=requirement_index default=requirement.facilityId! ignoreParameters=true />
        <@inputSelectCell name="facilityIdTo" list=facilities key="facilityId" displayField="facilityName" index=requirement_index default=requirement.facilityIdTo! ignoreParameters=true />
        <@displayDateCell date=requirement.requirementStartDate! />
        <@inputMultiCheckCell index=requirement_index />
      </tr>
    </#list>

    <tr>
      <td colspan="5" align="right">
        <@inputSubmit title=uiLabelMap.PurchCreateInventoryTransfer />
        <@inputSubmit onClick="javascript:this.form.action='cancelTransferRequirements'; opentaps.confirmSubmitAction('${uiLabelMap.OpentapsAreYouSure}', this.form);" title="${uiLabelMap.CommonCancel}"/>
      </td>
      <td/>
    </tr>

    <@inputHiddenUseRowSubmit />
    <@inputHiddenRowCount list=requirementList />
  </table>
</form>
