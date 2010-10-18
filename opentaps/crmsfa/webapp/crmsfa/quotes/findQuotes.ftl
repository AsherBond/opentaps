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

<form name="findQuotes" method="post" action="<@ofbizUrl>findQuotes</@ofbizUrl>">
  <@inputHidden name="performFind" value="Y" />
  <table class="twoColumnForm">
    <@inputTextRow name="quoteId" title=uiLabelMap.OrderOrderQuoteId />
    <@inputTextRow name="quoteName" title=uiLabelMap.OrderOrderQuoteName />
    <@inputSelectRow name="statusId" title=uiLabelMap.CommonStatus list=quoteStatuses displayField="description" required=false />
    <@inputLookupRow name="partyId" title=uiLabelMap.CrmAccount lookup="LookupPartyName" form="findQuotes" />
    <@inputSelectRow name="productStoreId" title=uiLabelMap.OrderProductStore list=productStores displayField="storeName" required=false />
    <@inputSelectRow name="salesChannelEnumId" title=uiLabelMap.OrderSalesChannel list=salesChannels displayField="description" key="enumId" required=false />
    <@inputSubmitRow title=uiLabelMap.CrmFindQuotes />
  </table>
</form>
