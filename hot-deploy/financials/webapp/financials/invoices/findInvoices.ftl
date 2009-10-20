<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

<#-- Parameterized find form for invoices. TODO: use the form macros instead.  See invoicePartnersForm.ftl for some work already done in this regard. -->

<#macro dateRangeInput fromParam thruParam formName>
<span class="tabletext">
${uiLabelMap.CommonFrom} 
<@inputDate name="${fromParam}" default=parameters.get(fromParam)?if_exists/>
&nbsp;&nbsp;
${uiLabelMap.CommonThru} 
<@inputDate name="${thruParam}" default=parameters.get(thruParam)?if_exists/>
</span>
</#macro>

<form method="post" action="<@ofbizUrl>${formTarget}</@ofbizUrl>" name="findInvoiceForm" style="margin: 0pt;">
  <input name="invoiceTypeId" type="hidden" value="${invoiceTypeId}"/>
  <input name="performFind" type="hidden" value="Y"/>
  <table border="0" cellpadding="2" cellspacing="0">
    <tbody>

  <tr>
    <td align="right" width="20%">
      <span class="tableheadtext">${uiLabelMap.FinancialsInvoiceId}</span>
    </td>
    <td>&nbsp;</td>
    <td align="left" width="80%">
      <input class="inputBox" name="invoiceId" size="20" maxlength="20" type="text" value="${parameters.invoiceId?if_exists}">
    </td>
  </tr>

  <#if isReceivable>
  <input name="partyIdFrom" value="${parameters.organizationPartyId}" type="hidden">
  <tr>
    <td align="right" width="20%">
      <span class="tableheadtext">${uiLabelMap.AccountingToParty}</span>
    </td>
    <td>&nbsp;</td>
    <td align="left" width="80%">
      <@inputAutoCompleteParty name="partyId" id="findInvoiceFormPartyId" />
    </td>
  </tr>
  </#if>

  <#if isPayable>
  <input name="partyId" value="${parameters.organizationPartyId}" type="hidden">
  <tr>
    <td align="right" width="20%">
      <span class="tableheadtext">${uiLabelMap.AccountingFromParty}</span>
    </td>
    <td>&nbsp;</td>
    <td align="left" width="80%">
      <@inputAutoCompleteParty name="partyIdFrom" id="findInvoiceFormPartyId" />
    </td>
  </tr>
  </#if>

  <#if isPartner>
  <tr>
    <td align="right" width="20%">
      <span class="tableheadtext">${uiLabelMap.OpentapsPartner}</span>
    </td>
    <td>&nbsp;</td>
    <@inputSelectCell name="partyIdFrom" list=partners key="partyId" required=false ; partner >
      ${partner.firstName?if_exists} ${partner.lastName?if_exists} ${partner.groupName?if_exists} (${partner.partyId})
    </@inputSelectCell>
  </tr>
  </#if>

  <tr>
    <td align="right" width="20%">
      <span class="tableheadtext">${uiLabelMap.CommonStatus}</span>
    </td>
    <td>&nbsp;</td>
    <td align="left" width="80%">
      <select class="inputBox" name="statusId" size="1">
        <option value="">&nbsp;</option>
        <#list statuses as status>
          <#assign selected = "">
          <#if status.statusId == parameters.statusId?default("NA")><#assign selected = "selected"></#if>
          <option ${selected} value="${status.statusId}">${status.statusDescription}</option>
        </#list>
      </select>
    </td>
  </tr>

  <tr>
    <td align="right" width="20%">
      <span class="tableheadtext">${uiLabelMap.FinancialsProcessingStatus}</span>
    </td>
    <td>&nbsp;</td>
    <td align="left" width="80%">
      <select class="inputBox" name="processingStatusId" size="1">
        <option value="">&nbsp;</option>
        <#list processingStatuses as status>
          <#assign selected = "">
          <#if status.statusId == parameters.processingStatusId?default("NA")><#assign selected = "selected"></#if>
          <option ${selected} value="${status.statusId}">${status.statusDescription}</option>
        </#list>
      </select>
    </td>
  </tr>

  <tr>
    <td align="right" width="20%">
      <span class="tableheadtext">${uiLabelMap.AccountingInvoiceDate}</span>
    </td>
    <td>&nbsp;</td>
    <td align="left" width="80%">
      <@dateRangeInput fromParam="invoiceDateFrom" thruParam="invoiceDateThru" formName="findInvoiceForm"/>
    </td>
  </tr>

  <tr>
    <td align="right" width="20%">
      <span class="tableheadtext">${uiLabelMap.AccountingDueDate}</span>
    </td>
    <td>&nbsp;</td>
    <td align="left" width="80%">
      <@dateRangeInput fromParam="dueDateFrom" thruParam="dueDateThru" formName="findInvoiceForm"/>
    </td>
  </tr>

  <tr>
    <td align="right" width="20%">
      <span class="tableheadtext">${uiLabelMap.AccountingPaidDate}</span>
    </td>
    <td>&nbsp;</td>
    <td align="left" width="80%">
      <@dateRangeInput fromParam="paidDateFrom" thruParam="paidDateThru" formName="findInvoiceForm"/>
    </td>
  </tr>

  <tr>
    <td align="right" width="20%">
      <span class="tableheadtext">${uiLabelMap.FinancialsReferenceNumber}</span>
    </td>
    <td>&nbsp;</td>
    <td align="left" width="80%">
      <input type="text" class="inputBox" name="referenceNumber" size="30" value="${parameters.referenceNumber?if_exists}" />
    </td>
  </tr>

<#if enableFindByOrder>
  <tr>
    <td align="right" width="20%">
      <@display text=uiLabelMap.FinancialsRelatedOrderId class="tableheadtext" />
    </td>
    <td>&nbsp;</td>
    <td align="left" width="80%">
      <@inputText name="orderId" size="20" maxlength="20" />
    </td>
  </tr>
</#if>

  <tr>
    <td align="right" width="20%">&nbsp;</td>
    <td>&nbsp;</td>
    <td colspan="4" align="left" width="80%">
      <input class="smallSubmit" name="submitButton" value="${uiLabelMap.CommonFind}" type="submit">
    </td>
  </tr>
  </tbody></table>
</form>
