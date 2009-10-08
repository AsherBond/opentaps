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
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#-- TODO: we need a method to convert a list iterator into a list based on what is in the request -->
<#assign requirementList = requirements.getCompleteList()>

  <table class="listTable">
    <tr class="listTableHeader">
      <td>${uiLabelMap.ProductSupplier}</td>
      <td>${uiLabelMap.PurchNumberProducts}</td>
      <td>${uiLabelMap.ProductFacility}</td>
      <td>${uiLabelMap.PurchConsolidateRequirements}</td>
      <td></td>
      <td></td>
      <td></td>
    </tr>

    <#list requirementList as requirement>
    <#assign name = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, requirement.partyId, false)>
    <tr class="${tableRowClass(requirement_index)}">
      <form method="post" name="requirementOrderForm" action="requirementOrderForm">
        <@inputHidden name="partyId" value=requirement.partyId />
        <@inputHidden name="supplierPartyId" value=requirement.partyId />
        <@inputHidden name="billToCustomerPartyId" value=parameters.organizationPartyId />
        <@displayCell text="${name} (${requirement.partyId})" />
        <@displayCell text=requirement.productId />
        <@inputSelectCell name="facilityId" list=facilities key="facilityId" displayField="facilityName" />
        <@inputCheckboxCell name="consolidateFlag" value="Y" />
        <td style="text-align:center;">
            <@inputSubmit onClick="javascript:this.form.action='requirementOrderForm';" title="${uiLabelMap.OrderReviewOrder}"/>
        </td>
        <td style="text-align:center;">
	  <@inputSubmit onClick="javascript:this.form.action='createCartForAllSupplierRequirements'; opentaps.confirmSubmitAction('${uiLabelMap.OpentapsAreYouSure}', this.form); return false;" title="${uiLabelMap.PurchOrderAll}"/>
        </td>
      </form>
        <td style="text-align:center;">
          <@form name="cancelSupplierRequirementsForm" url="cancelSupplierRequirements" partyId="${requirement.partyId}"/>
          <@submitFormLinkConfirm form="cancelSupplierRequirementsForm" text=uiLabelMap.CommonCancelAll/>
        </td>
    </tr>
    </#list>
  </table>
