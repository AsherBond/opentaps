<#--
 * Copyright (c) Open Source Strategies, Inc.
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

<table class="listTable" >
  <tr class="boxtop">
    <td><span class="boxhead">${uiLabelMap.FinancialsTaxForRegion}</span></td>
    <td><span class="boxhead"></span></td>
    <td><span class="boxhead"></span></td>
    <td><span class="boxhead">${uiLabelMap.FinancialsAmountOutstanding}</span></td>
    <td><span class="boxhead">${uiLabelMap.CommonNote}</span></td>
    <td><span class="boxhead"></span></td>
    <td><span class="boxhead"></span></td>
  </tr>
  <@form name="removePaymentApplicationTaxForm" url="removePaymentApplication" paymentApplicationId="" paymentId="" />
  <#list paymentApplications as row>
    <form name="editPaymentApplicationsTax_${row_index}" action="createPaymentApplication" method="POST" class="basic-form">
      <tr class="viewManyTR2">
        <@inputHidden name="paymentId" value=row.paymentId />
        <@inputHidden name="paymentApplicationId" value=row.paymentApplicationId />
        <@inputSelectCell class="inputBoxFixedWidth" name="taxAuthGeoId" list=taxAuthGeoIds key="geoId" displayField="geoName" default=row.taxAuthGeoId?if_exists/>
        <td></td>
        <td></td>
        <@inputTextCell name="amountApplied" default=row.amountApplied/>
        <@inputTextCell name="note" default=row.note/>
        <@displayLinkCell class="buttontext" href="javascript:document.editPaymentApplicationsTax_${row_index}.submit();" text=uiLabelMap.CommonUpdate/>
        <td>
          <@submitFormLink form="removePaymentApplicationTaxForm" class="buttontext" text=uiLabelMap.CommonRemove paymentApplicationId=row.paymentApplicationId paymentId=row.paymentId/>
        </td>
      </tr>
      <#if tagTypes?has_content && allocatePaymentTagsToApplications>
        <@accountingTagsInputCells tags=tagTypes prefix="acctgTagEnumId" tagColSpan="1" entity=row!/>
      </#if>
    </form>
  </#list>
</table>
