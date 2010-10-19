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

<#assign showFromParty = !findDisbursement/>
<#assign showToParty = findDisbursement/>

<@paginate name="listPayments" list=paymentListBuilder rememberPage=false showFromParty=showFromParty showToParty=showToParty>
  <#noparse>
    <@navigationHeader/>
    <table class="listTable">
      <tr class="listTableHeader">
        <@headerCell title=uiLabelMap.FinancialsPaymentId orderBy="paymentId"/>
        <@headerCell title=uiLabelMap.FinancialsPaymentMethod orderBy="paymentMethodTypeId"/>
        <@headerCell title=uiLabelMap.AccountingEffectiveDate orderBy="effectiveDate"/>
        <@headerCell title=uiLabelMap.FinancialsPaymentRef orderBy="paymentRefNum"/>
        <@headerCell title=uiLabelMap.CommonComments orderBy="comments"/>
        <@headerCell title=uiLabelMap.CommonStatus orderBy="statusId"/>
        <#if parameters.showFromParty><@headerCell title=uiLabelMap.FinancialsReceiveFromParty orderBy="partyIdFrom, effectiveDate DESC"/></#if>
        <#if parameters.showToParty><@headerCell title=uiLabelMap.FinancialsPayToParty orderBy="partyIdTo, effectiveDate DESC"/></#if>
        <@headerCell title=uiLabelMap.AccountingAmount orderBy="amount, effectiveDate DESC" blockClass="textright"/>
        <@headerCell title=uiLabelMap.FinancialsAmountOutstanding orderBy="openAmount, effectiveDate DESC" blockClass="textright"/>
      </tr>
      <#list pageRows as row>
        <tr class="${tableRowClass(row_index)}">
          <@displayLinkCell text=row.paymentId href="viewPayment?paymentId=${row.paymentId}"/>
          <@displayCell text=row.paymentMethodDescription/>
          <@displayDateCell date=row.effectiveDate/>
          <@displayCell text=row.paymentRefNum/>
          <@displayCell text=row.comments/>
          <@displayCell text=row.statusDescription/>
          <#if parameters.showFromParty><@displayCell text=row.partyNameFrom/></#if>
          <#if parameters.showToParty><@displayCell text=row.partyNameTo/></#if>
          <@displayCurrencyCell amount=row.amount currencyUomId=row.currencyUomId/>
          <@displayCurrencyCell amount=row.openAmount currencyUomId=row.currencyUomId/>
        </tr>
      </#list>
    </table>
  </#noparse>
</@paginate>
