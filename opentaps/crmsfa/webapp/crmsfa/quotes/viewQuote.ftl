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
        <@displayPartyLinkCell partyId=quote.partyId />
      <#else>
        <@displayCell text="" />
      </#if>
      <@displayTitleCell title=uiLabelMap.CommonCurrency />
      <@displayCell text=quote.currencyUomId?if_exists />
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.CrmContact />
      <#if quote.contactPartyId?has_content>
        <@displayPartyLinkCell partyId=quote.contactPartyId />
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
