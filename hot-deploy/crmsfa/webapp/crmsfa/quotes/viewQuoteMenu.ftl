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

<div class="subSectionHeader">
  <div class="subSectionTitle"><#if quote?exists>${uiLabelMap.OrderQuote} ${uiLabelMap.OrderNbr}${quote.quoteId}</#if></div>
  <div class="subMenuBar">
    <#if quote?exists>
      <#list validChanges as validChange>
        <#if "QUO_ORDERED" == validChange.statusIdTo>
          <a href="<@ofbizUrl>loadCartFromQuote?quoteId=${quote.quoteId}&amp;finalizeMode=init</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCreateOrder}</a>
        <#else>
          <#if "QUO_FINALIZED" == validChange.statusIdTo>
            <a href="<@ofbizUrl>finalizeQuote?quoteId=${quote.quoteId}</@ofbizUrl>" class="buttontext">${validChange.transitionName}</a>
          <#else>
            <a href="<@ofbizUrl>setQuoteStatus?quoteId=${quote.quoteId}&amp;statusId=${validChange.statusIdTo}</@ofbizUrl>" class="buttontext">${validChange.transitionName}</a>
          </#if>
        </#if>
      </#list>
      <#if canEditQuote>
        <a href="<@ofbizUrl>EditQuote?quoteId=${quote.quoteId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
      </#if>
      <a href="<@ofbizUrl>quote.pdf?quoteId=${quote.quoteId}</@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingInvoicePDF}</a>
      <a href="<@ofbizUrl>writeQuoteEmail?quoteId=${quote.quoteId}&amp;emailType=PRDS_QUO_CONFIRM</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEmail}</a>          
      <a href="<@ofbizUrl>QuoteMiniCatalog.pdf?quoteId=${quote.quoteId}</@ofbizUrl>" class="buttontext">${uiLabelMap.OpentapsMiniCatalog}</a>
    </#if>
  </div>
</div>
