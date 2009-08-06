<#--
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

<div class="subSectionBlock">
  <table class="fourColumnForm">
    <tr>
      <@displayTitleCell title=uiLabelMap.OrderOrderQuoteName />
      <@displayCell text=quote.quoteName />
      <@displayTitleCell title=uiLabelMap.CommonStatus />
      <@displayCell text=statusItem.description />
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.OrderProductStore />
      <@displayCell text=store.storeName />
      <@displayTitleCell title=uiLabelMap.OrderSalesChannel />
      <@displayCell text=salesChannel.description />
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.CrmAccount />
      <#if quote.partyId?has_content>
        <@displayLinkCell text=partyName?if_exists href="${partyUrl}" />
      <#else>
        <@displayCell text="" />
      </#if>
      <@displayTitleCell title=uiLabelMap.CommonCurrency />
      <@displayCell text=quote.currencyUomId?if_exists />
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.CrmContact />
      <#if quote.contactPartyId?has_content>
        <@displayLinkCell text=contactPartyName?if_exists href="${contactPartyUrl}" />
      <#else>
        <@displayCell text="" />
      </#if>
      <@displayTitleCell title=uiLabelMap.CommonCreatedBy />
      <#if quote.createdByPartyId?has_content>
        <@displayCell text=createdByName?if_exists />
      <#else>
        <@displayCell text="" />
      </#if>
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.CommonValidFromDate />
      <@displayDateCell date=quote.validFromDate?if_exists />
      <@displayTitleCell title=uiLabelMap.CommonValidThruDate />
      <@displayDateCell date=quote.validThruDate?if_exists />
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.CommonDescription />
      <td colspan="3">
        <@display text=quote.description?if_exists />
      </td>
    </tr>
  </table>
</div>
