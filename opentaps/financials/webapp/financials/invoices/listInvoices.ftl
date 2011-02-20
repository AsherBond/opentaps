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

<#-- Parametrized find form for invoices. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#assign showFromParty = !isReceivable/>
<#assign showToParty = !isPayable/>
<#assign showOrderIds = isReceivable || isPayable/>

<@paginate name="listInvoices" list=invoiceListBuilder rememberPage=false showFromParty=showFromParty showToParty=showToParty showOrderIds=showOrderIds>
    <#noparse>
    <form name="listInvoices" action="invoice.pdf" method="POST" target="_blank" class="basic-form">
        <@inputHidden name="reportId" value="FININVOICE" />
        <@inputHidden name="reportType" value="application/pdf" />
        <@navigationHeader/>
        <table class="listTable">
            <tr class="listTableHeader">
                <@headerCell title=uiLabelMap.FinancialsInvoiceId orderBy="invoiceId"/>
                <@headerCell title=uiLabelMap.CommonDescription orderBy="description"/>
                <@headerCell title=uiLabelMap.FinancialsReferenceNumber orderBy="referenceNumber"/>
                <#if parameters.showOrderIds><th>${uiLabelMap.CommonOrders}</th></#if>
                <@headerCell title=uiLabelMap.AccountingInvoiceDate orderBy="invoiceDate"/>
                <@headerCell title=uiLabelMap.AccountingDueDate orderBy="dueDate"/>
                <@headerCell title=uiLabelMap.CommonStatus orderBy="statusId"/>
                <@headerCell title=uiLabelMap.FinancialsProcessingStatus orderBy="processingStatusId"/>
                <#if parameters.showFromParty><@headerCell title=uiLabelMap.AccountingFromParty orderBy="partyIdFrom, invoiceDate DESC"/></#if>
                <#if parameters.showToParty><@headerCell title=uiLabelMap.AccountingToParty orderBy="partyId, invoiceDate DESC"/></#if>
                <@headerCell title=uiLabelMap.AccountingAmount orderBy="invoiceTotal, invoiceDate DESC" blockClass="textright"/>
                <@headerCell title=uiLabelMap.OrderOutstanding orderBy="openAmount, invoiceDate DESC" blockClass="textright"/>
                <td><input type="checkbox" name="selectAll" value="N" onclick="javascript:toggleAll(this, 'listInvoices'); showButtons();"></td>
            </tr>
            <#list pageRows as row>
            <tr class="${tableRowClass(row_index)}">
                <@inputHidden name="invoiceId" value=row.invoiceId index=row_index/>
                <@displayLinkCell text=row.invoiceId href="viewInvoice?invoiceId=${row.invoiceId}"/>
                <@displayCell text=row.description/>
                <@displayCell text=row.referenceNumber/>
                <#if parameters.showOrderIds>
                  <td>
                    <#if row.orderIds?has_content>
                      <#list row.orderIds as orderId>${orderId}<#if orderId_has_next>, </#if></#list>
                    </#if>
                  </td>
                </#if>
                <@displayDateCell date=row.invoiceDate/>
                <@displayDateCell date=row.dueDate/>
                <@displayCell text=row.statusDescription/>
                <@displayCell text=row.processingStatusDescription/>
                <#if parameters.showFromParty><@displayCell text=row.partyNameFrom/></#if>
                <#if parameters.showToParty><@displayCell text=row.partyName/></#if>
                <@displayCurrencyCell amount=row.amount currencyUomId=row.currencyUomId/>
                <@displayCurrencyCell amount=row.outstanding currencyUomId=row.currencyUomId/>
                <#if row.statusId != "INVOICE_CANCELLED" && row.statusId != "INVOICE_WRITEOFF" && row.statusId != "INVOICE_VOIDED">
                    <td><input type="checkbox" name="_rowSubmit_o_${row_index}" value="Y" onclick="javascript:showButtons();"></td>
                <#else>
                    <td>&nbsp;</td>
                </#if>
            </tr>
            </#list>
        <tr><td colspan="10" id="buttonsBar" class="textright hidden"><@inputSubmit title="${uiLabelMap.CommonPrint}" onClick=""/></td></tr>
        </table>
    </form>
    </#noparse>
</@paginate>
