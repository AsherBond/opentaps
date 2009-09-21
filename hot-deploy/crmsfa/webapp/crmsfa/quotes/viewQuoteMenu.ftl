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
<#assign quoteStatusChangeAction = "">
    <#if quote?exists>
      <#list validChanges as validChange>
        <#if "QUO_ORDERED" == validChange.statusIdTo>
          <@form name="loadCartFromQuote${validChange_index}" url="loadCartFromQuote" quoteId="${quote.quoteId}" finalizeMode="init"/>
          <#assign quoteStatusChangeAction>${quoteStatusChangeAction}<@submitFormLink form="loadCartFromQuote${validChange_index}" text=uiLabelMap.OrderCreateOrder class="buttontext" /></#assign>
        <#else>
          <#if "QUO_FINALIZED" == validChange.statusIdTo>
            <@form name="finalizeQuote${validChange_index}" url="finalizeQuote" quoteId="${quote.quoteId}" />
            <#assign quoteStatusChangeAction>${quoteStatusChangeAction}<@submitFormLink form="finalizeQuote${validChange_index}" text=validChange.transitionName class="buttontext" /></#assign>
          <#else>
            <@form name="setQuoteStatus${validChange_index}" url="setQuoteStatus" quoteId="${quote.quoteId}" statusId="${validChange.statusIdTo}" />
            <#assign quoteStatusChangeAction>${quoteStatusChangeAction}<@submitFormLink form="setQuoteStatus${validChange_index}" text=validChange.transitionName class="buttontext" /></#assign>
          </#if>
        </#if>
      </#list>
      <#if canEditQuote>
        <@form name="editQuote" url="EditQuote" quoteId="${quote.quoteId}" />
        <#assign quoteStatusChangeAction>${quoteStatusChangeAction}<@submitFormLink form="editQuote" text=uiLabelMap.CommonEdit class="buttontext" /></#assign>
      </#if>

      <@form name="toQuotePdf" url="quote.pdf" quoteId="${quote.quoteId}" />
      <#assign quoteStatusChangeAction>${quoteStatusChangeAction}<@submitFormLink form="toQuotePdf" text=uiLabelMap.AccountingInvoicePDF class="buttontext" /></#assign>
      <@form name="writeQuoteEmail" url="writeQuoteEmail" quoteId="${quote.quoteId}" emailType="PRDS_QUO_CONFIRM" />
      <#assign quoteStatusChangeAction>${quoteStatusChangeAction}<@submitFormLink form="writeQuoteEmail" text=uiLabelMap.CommonEmail class="buttontext" /></#assign>
      <@form name="toCatalogPdf" url="QuoteMiniCatalog.pdf" quoteId="${quote.quoteId}" />
      <#assign quoteStatusChangeAction>${quoteStatusChangeAction}<@submitFormLink form="toCatalogPdf" text=uiLabelMap.OpentapsMiniCatalog class="buttontext" /></#assign>
    </#if>
<div class="subSectionHeader">
  <div class="subSectionTitle"><#if quote?exists>${uiLabelMap.OrderQuote} ${uiLabelMap.OrderNbr}${quote.quoteId}</#if></div>
  <div class="subMenuBar">
    ${quoteStatusChangeAction?if_exists}
  </div>
</div>
