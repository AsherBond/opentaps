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

<#-- Sort of like the find invoice form, except the action in this list is to create a partner sales invoice from the selected partner invoices -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#macro dateRangeInput fromParam thruParam formName>
<span class="tabletext">
${uiLabelMap.CommonFrom} <@inputDate name="${fromParam}" default=parameters.get(fromParam)?if_exists/>
&nbsp;&nbsp;
${uiLabelMap.CommonThru} <@inputDate name="${thruParam}" default=parameters.get(thruParam)?if_exists/>
</span>
</#macro>

<div class="subSectionBlock">
<form method="post" action="invoicePartnersForm" name="invoicePartnersForm">
  <@inputHidden name="statusId" value="INVOICE_READY"/>
  <@inputHidden name="performFind" value="Y"/>
  <#-- note that invoiceTypeId is set by the screen definition -->

<table class="twoColumnForm">
    <@inputSelectRow name="partyIdFrom" title=uiLabelMap.OpentapsPartner list=partners key="partyId" ; partner >
      ${partner.firstName?if_exists} ${partner.lastName?if_exists} ${partner.groupName?if_exists} (${partner.partyId})
    </@inputSelectRow>

  <tr>
    <td class="titleCell">
      <span class="tableheadtext">${uiLabelMap.AccountingInvoiceDate}</span>
    </td>
    <td align="left" width="80%">
      <@dateRangeInput fromParam="invoiceDateFrom" thruParam="invoiceDateThru" formName="invoicePartnersForm"/>
    </td>
  </tr>

  <tr>
    <td class="titleCell">
      <span class="tableheadtext">${uiLabelMap.AccountingDueDate}</span>
    </td>
    <td align="left" width="80%">
      <@dateRangeInput fromParam="dueDateFrom" thruParam="dueDateThru" formName="invoicePartnersForm"/>
    </td>
  </tr>

  <@inputSubmitRow title=uiLabelMap.CommonFind />
</table>

</form>
</div>

<@paginate name="invoicePartners" list=invoiceListBuilder rememberPage=false organizationPartyId=parameters.organizationPartyId partnerPartyId=parameters.partyIdFrom>
    <#noparse>
    <form name="listInvoices" action="createPartnerSalesInvoice" method="POST">
        <@inputHiddenUseRowSubmit/>
        <@inputHiddenRowCount list=pageRows/>
        <@inputHidden name="organizationPartyId" value=parameters.organizationPartyId />
        <@inputHidden name="partnerPartyId" value=parameters.partnerPartyId />

        <@navigationHeader/>
        <table class="listTable">
            <tr class="listTableHeader">
                <@displayCell text=uiLabelMap.FinancialsInvoiceId/>
                <@displayCell text=uiLabelMap.FinancialsReferenceNumber/>
                <@headerCell title=uiLabelMap.AccountingInvoiceDate orderBy="invoiceDate"/>
                <@headerCell title=uiLabelMap.AccountingDueDate orderBy="dueDate"/>
                <@headerCell title=uiLabelMap.CommonStatus orderBy="statusId"/>
                <@headerCell title=uiLabelMap.AccountingFromParty orderBy="partyIdFrom, invoiceDate DESC"/>
                <@headerCell title=uiLabelMap.AccountingToParty orderBy="partyId, invoiceDate DESC"/>
                <@headerCell title=uiLabelMap.AccountingAmount orderBy="amount" blockClass="textright"/>
                <@headerCell title=uiLabelMap.OrderOutstanding orderBy="outstanding" blockClass="textright"/>
                <@inputMultiSelectAllCell form="listInvoices" onClick="showButtons();" />
            </tr>
            <#list pageRows as row>
            <tr class="${tableRowClass(row_index)}">
                <@inputHidden name="invoiceId" value=row.invoiceId index=row_index />
                <@displayLinkCell text=row.invoiceId href="viewInvoice?invoiceId=${row.invoiceId}"/>
                <@displayCell text=row.referenceNumber/>
                <@displayCell text=row.invoiceDate?string.short/>
                <@displayCell text=row.dueDate/>
                <@displayCell text=row.statusDescription/>
                <@displayCell text=row.partyNameFrom/>
                <@displayCell text=row.partyName/>
                <@displayCurrencyCell amount=row.amount currencyUomId=row.currencyUomId/>
                <@displayCurrencyCell amount=row.outstanding currencyUomId=row.currencyUomId/>
                <@inputMultiCheckCell index=row_index onClick="javascript:showButtons();" />
            </tr>
            </#list>
        <tr><td colspan="10"><div id="buttonsBar" class="textright hidden"><@inputSubmit title=uiLabelMap.FinancialsInvoiceSelectedInvoices /></div></td></tr>
        </table>
    </form>
    </#noparse>
</@paginate>
