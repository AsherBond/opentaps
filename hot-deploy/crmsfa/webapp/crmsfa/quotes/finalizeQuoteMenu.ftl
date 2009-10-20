<#--
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

<div class="subSectionHeader">
  <div class="subSectionTitle"><#if quote?exists>${quote.quoteName?if_exists} (${quote.quoteId})</#if></div>
  <div class="subMenuBar">
    <#if quote?exists>
      <a href="<@ofbizUrl>ViewQuote?quoteId=${quote.quoteId}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderViewQuote}</a>
      <#if canEditQuote>
        <a href="<@ofbizUrl>EditQuote?quoteId=${quote.quoteId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
      </#if>
      <a href="<@ofbizUrl>quote.pdf?quoteId=${quote.quoteId}</@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingInvoicePDF}</a>
      <a href="<@ofbizUrl>writeQuoteEmail?quoteId=${quote.quoteId}&amp;emailType=PRDS_QUO_CONFIRM</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEmail}</a>          
      <a href="<@ofbizUrl>QuoteMiniCatalog.pdf?quoteId=${quote.quoteId}</@ofbizUrl>" class="buttontext">${uiLabelMap.OpentapsMiniCatalog}</a>
    </#if>
  </div>
</div>
