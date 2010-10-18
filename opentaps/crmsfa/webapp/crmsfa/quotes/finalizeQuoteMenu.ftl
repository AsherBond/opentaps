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

<#assign quoteStatusChangeAction = "">
<#if quote?exists>
  <#assign quoteStatusChangeAction>${quoteStatusChangeAction}<a href="<@ofbizUrl>ViewQuote?quoteId=${quote.quoteId}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderViewQuote}</a></#assign>
  
  <#if canEditQuote>
    <@form name="editQuote" url="EditQuote" quoteId="${quote.quoteId}" />
    <#assign quoteStatusChangeAction>${quoteStatusChangeAction}<@submitFormLink form="editQuote" text=uiLabelMap.CommonEdit class="buttontext" /></#assign>
  </#if>

  <@form name="toQuotePdf" url="quote.pdf" quoteId="${quote.quoteId}" reportId="SALESQUOTE" reportType="application/pdf" />
  <#assign quoteStatusChangeAction>${quoteStatusChangeAction}<@submitFormLink form="toQuotePdf" text=uiLabelMap.AccountingInvoicePDF class="buttontext" /></#assign>
  <@form name="writeQuoteEmail" url="writeQuoteEmail" quoteId="${quote.quoteId}" emailType="PRDS_QUO_CONFIRM" />
  <#assign quoteStatusChangeAction>${quoteStatusChangeAction}<@submitFormLink form="writeQuoteEmail" text=uiLabelMap.CommonEmail class="buttontext" /></#assign>
  <@form name="toCatalogPdf" url="QuoteMiniCatalog.pdf" quoteId="${quote.quoteId}" />
  <#assign quoteStatusChangeAction>${quoteStatusChangeAction}<@submitFormLink form="toCatalogPdf" text=uiLabelMap.OpentapsMiniCatalog class="buttontext" /></#assign>
</#if>

<@frameSectionHeader title="${quote?exists?string(\"${uiLabelMap.OrderQuote} ${uiLabelMap.OrderNbr}${quote.quoteId}\", '')}" extra=quoteStatusChangeAction! />
