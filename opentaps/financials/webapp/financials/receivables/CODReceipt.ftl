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

<@frameSection title=uiLabelMap.FinancialsReceiveCODStatement>
  <form method="post" action="<@ofbizUrl>CODReceipt</@ofbizUrl>" name="FindInvoicesForCODReceipt">
    <table>
      <@inputSelectRow title=uiLabelMap.ProductCarrier list=carriers?sort_by("partyName") name="carrierPartyId" key="partyId" displayField="partyName"/>
      <@inputSubmitRow title=uiLabelMap.CommonFind />
    </table>
  </form>
</@frameSection>
  
<#if codInvoices?default([])?has_content>
    <form method="post" action="<@ofbizUrl>processCODReceipt</@ofbizUrl>" name="processCODReceipt">
      <table class="listTable" cellspacing="0">
          <tbody>
            <tr class="boxtop">
                <td><span class="boxhead">${uiLabelMap.ProductShipmentId}</span></td>
                <td><span class="boxhead">${uiLabelMap.OpentapsTrackingNumber}</span></td>
                <td><span class="boxhead">${uiLabelMap.FinancialsInvoiceId}</span></td>
                <td><span class="boxhead">${uiLabelMap.AccountingInvoiceDate}</span></td>
                <td><span class="boxhead">${uiLabelMap.AccountingCustomer}</span></td>
                <td class="currencyCell"><span class="boxhead">${uiLabelMap.FinancialsAmountOutstanding}</span></td>
                <td><span class="boxhead">${uiLabelMap.FinancialsPaymentRefNum}</span></td>
                <td><span class="boxhead">${uiLabelMap.AccountingAmount}</span></td>
                <td><input type="checkbox" onclick="javascript:toggleAll(this, 'processCODReceipt');" checked="checked"/></td>
            </tr>
            <#list codInvoices as codInvoice>
                <tr class="${tableRowClass(codInvoice_index)}">
                    <@displayCell text=codInvoice.shipmentId?if_exists/>
                    <@displayCell text=codInvoice.trackingIdNumber?if_exists/>
                    <@displayLinkCell href="viewInvoice?invoiceId=${codInvoice.invoiceId}" text=codInvoice.invoiceId/>
                    <@displayDateCell date=codInvoice.invoice.invoiceDate format="DATE"/>
                    <@displayCell text="${codInvoice.toPartyName?if_exists} (${codInvoice.invoice.partyId?if_exists})"/>
                    <@displayCurrencyCell currencyUomId=codInvoice.invoice.currencyUomId amount=codInvoice.outstanding?if_exists/>
                    <@inputTextCell name="paymentRefNum" size=15 index=codInvoice_index/>
                    <td><@inputText name="amount" size=10  index=codInvoice_index/></td>
                    <td>
                      <input type="checkbox" name="_rowSubmit_o_${codInvoice_index}" checked="checked"/>
                      <@inputHidden name="invoiceId" value=codInvoice.invoiceId index=codInvoice_index/>
                      <@inputHidden name="trackingCode" value=codInvoice.trackingIdNumber?if_exists index=codInvoice_index/>
                    </td>
                </tr>
            </#list>
            <tr>
              <td colspan="7" style="text-align:right">${uiLabelMap.FinancialsStatementDate}:</td>
              <@inputDateCell name="invoiceDate" default=getLocalizedDate(now, "DATE")/>
            </tr>
            <tr>
              <td colspan="7" style="text-align:right">${uiLabelMap.AccountingReferenceNumber}:</td>
              <@inputTextCell name="referenceNumber" size=10/>
            </tr>
            <tr>
              <td colspan="7" style="text-align:right">${uiLabelMap.FinancialsCODCommission}:</td>
              <@inputTextCell name="adjustmentAmount" size=10/>
              <@inputHidden name="carrierPartyId" value=parameters.carrierPartyId/>
              <@inputHidden name="organizationPartyId" value=parameters.organizationPartyId/>
            </tr>
            <tr>
              <td colspan="7" style="text-align:right">${uiLabelMap.CommonCurrency}:</td>
              <td>
                <select name="currencyUomId" class="inputBox">
                  <#list currencies as currency>
                    <#if currency.uomId == defaultCurrencyUomId><#assign selected = "selected"><#else><#assign selected = ""></#if>
                    <option ${selected} value="${currency.uomId}">${currency.abbreviation}</option>
                  </#list>
                </select>
              </td>
            </tr>
            <tr>
              <td colspan="8" style="text-align:right">
                  <div style="margin:10px 15px 10px 10px;"><@inputConfirm title=uiLabelMap.FinancialsReceiveStatement form="processCODReceipt"/></div>
              </td>
            </tr>
          </tbody>
      </table>
  </form>
</#if>
