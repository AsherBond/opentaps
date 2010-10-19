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

<#macro listInvoices invoicesWithBalances>
  <#assign subTotal = 0>
  <#list invoicesWithBalances as invoice>
  <#-- each invoice is an org.opentaps.domain.billing.invoice.Invoice object -->
      <#assign isPastDue = invoice.isPastDue()>
      <#assign openAmount = invoice.getOpenAmount() >
      <#assign invoiceTotal = invoice.getInvoiceTotal() >
  <tr>
     <td class="tabletext" width="30%">${getLocalizedDate(invoice.invoiceDate, "DATE")}</td>
     <td class="tabletext" width="50%">
     ${uiLabelMap.AccountingInvoice}
     <a href="<@ofbizUrl>viewInvoice?invoiceId=${invoice.invoiceId}</@ofbizUrl>" class="linktext">${invoice.invoiceId}</a>
     <#if reportType == "ACCOUNTS_RECEIVABLE">
       ${uiLabelMap.CommonFor} ${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, invoice.get("partyId"), false)}
     <#else>
       ${uiLabelMap.CommonFrom} ${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, invoice.get("partyIdFrom"), false)}
     </#if>
     (<a href="<@ofbizUrl>writeInvoiceEmail?invoiceId=${invoice.invoiceId}</@ofbizUrl>" class="buttontext">Email</a>
     <a href="<@ofbizUrl>invoice.pdf?invoiceId=${invoice.invoiceId}&amp;reportId=FININVOICE&amp;reportType=application/pdf</@ofbizUrl>" class="buttontext">PDF</a>)
     </td>
     <td class="tabletext" align="right" width="10%"><@ofbizCurrency amount=invoiceTotal isoCode=orgCurrencyUomId/></td>
     <td class="tabletext" align="right" width="10%"><#if isPastDue><font color="red"></#if><@ofbizCurrency amount=openAmount isoCode=orgCurrencyUomId/><#if isPastDue></font></#if></td>

     <#assign subTotal = subTotal + openAmount>
     <#assign grandTotal = grandTotal + openAmount>
  </tr>
  </#list>
  <tr>
     <td colspan="4">&nbsp;</td>
     <td class="tabletext" width="10%" align="right"><@ofbizCurrency amount=subTotal isoCode=orgCurrencyUomId/></td>
  </tr>
</#macro>

<#if daysOutstandingBreakPoints?exists>
<div style="border: 1px solid #999999; margin-top: 20px; margin-bottom: 20px;"></div>
<table border="0" cellpadding="0">
  <tr>
     <td colspan="4" class="tabletext" align="center">
${uiLabelMap.CommonFor} ${parameters.organizationName?if_exists} (${parameters.organizationPartyId})<br/>
${getLocalizedDate(asOfDate, "DATE")}</td>
     </td>
  </tr>
  <tr><td>&nbsp;</td></tr>  
  <tr>
  <td>&nbsp;</td>
  <td class="tableheadtext">${uiLabelMap.AccountingInvoice}</td>
  <td class="tableheadtext">${uiLabelMap.AccountingInvoiceTotal}</td>
  <td class="tableheadtext">${uiLabelMap.OpentapsOpenAmount}</td>
  </tr>        
<#assign lastDSOBreakPoint = 0>            
<#assign grandTotal = 0>
<#list daysOutstandingBreakPoints as daysOutstandingBreakPoint>
  <tr>
   <td colspan="2" class="tableheadtext">${lastDSOBreakPoint} ${uiLabelMap.CommonThru} ${daysOutstandingBreakPoint} ${uiLabelMap.CommonDays}</td>
  </tr>
  <@listInvoices invoicesWithBalances=invoicesByDSO.get(daysOutstandingBreakPoint)/>
  <tr><td>&nbsp;</td></tr>
  <#assign lastDSOBreakPoint = daysOutstandingBreakPoint>
</#list>
  <tr>
   <td colspan="2" class="tableheadtext">${lastDSOBreakPoint}+ ${uiLabelMap.CommonDays}</td>
  </tr>
  <@listInvoices invoicesWithBalances=olderThanMaxDSOInvoices/>
  <tr>
     <td colspan="3">&nbsp;</td>
     <td class="tabletext" align="right"><hr/></td>
  </tr>
   
  <tr>
     <td colspan="3">&nbsp;</td>
     <td class="tableheadtext" align="right"><@ofbizCurrency amount=grandTotal isoCode=orgCurrencyUomId/></td>
  </tr>
    
</table>
</#if>
