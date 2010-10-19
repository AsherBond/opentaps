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
    <td><span class="boxhead">${uiLabelMap.GlAccount}</span></td>
    <td><span class="boxhead"></span></td>
    <td><span class="boxhead"></span></td>
    <td><span class="boxhead">${uiLabelMap.AccountingAmountApplied}</span></td>
    <td><span class="boxhead">${uiLabelMap.CommonNote}</span></td>
    <td><span class="boxhead"></span></td>
    <td><span class="boxhead"></span></td>
  </tr>
  <@form name="removePaymentApplicationGlForm" url="removePaymentApplication" paymentApplicationId="" paymentId="" />
  <#list paymentApplicationsListGlAccounts as row>
    <form name="paymentApplicationsListGlAccounts_${row_index}" action="updatePaymentApplication" method="POST" class="basic-form">
      <#assign accountCode = row.glAccount.get("accountCode")/>
      <#assign accountName = row.glAccount.get("accountName")/>
      <#assign glAccountId = row.glAccount.get("glAccountId")/>
      <tr class="viewManyTR2">
        <@inputHidden name="paymentApplicationId" value=row.paymentApplicationId />
        <@inputHidden name="paymentId" value=row.paymentId?if_exists />
        <@inputHidden name="glAccountId" value=glAccountId?if_exists />
        <@displayCell text="${accountCode}: ${accountName} (${glAccountId})"/>
        <td></td>
        <td></td>
        <@inputTextCell name="amountApplied" default=row.amountApplied/>
        <@inputTextCell name="note" default=row.note/>
        <@displayLinkCell class="buttontext" href="javascript:document.paymentApplicationsListGlAccounts_${row_index}.submit();" text=uiLabelMap.CommonUpdate/>
        <td>
          <@submitFormLink form="removePaymentApplicationGlForm" class="buttontext" text=uiLabelMap.CommonRemove paymentApplicationId=row.paymentApplicationId paymentId=row.paymentId/>
        </td>
      </tr>
      <#if tagTypes?has_content && allocatePaymentTagsToApplications>
        <@accountingTagsInputCells tags=tagTypes prefix="acctgTagEnumId" tagColSpan="4" entity=row!/>
      </#if>
    </form>
  </#list>
</table>
