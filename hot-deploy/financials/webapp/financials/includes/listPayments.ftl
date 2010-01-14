<#--
 * Copyright (c) 2010 - 2010 Open Source Strategies, Inc.
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

<@paginate name="listPayments" list=paymentListBuilder rememberPage=false>
  <#noparse>
    <@navigationHeader/>
    <table class="listTable">
      <tr class="listTableHeader">
        <@headerCell title=uiLabelMap.FinancialsPaymentId orderBy="paymentId"/>
        <@headerCell title=uiLabelMap.FinancialsPaymentMethod orderBy="paymentMethodTypeId"/>
        <@headerCell title=uiLabelMap.AccountingEffectiveDate orderBy="effectiveDate"/>
        <@headerCell title=uiLabelMap.FinancialsPaymentRef orderBy="paymentRefNum"/>
        <@headerCell title=uiLabelMap.CommonStatus orderBy="statusId"/>
        <@headerCell title=uiLabelMap.FinancialsReceiveFromParty orderBy="partyIdFrom, effectiveDate DESC"/>
        <@headerCell title=uiLabelMap.FinancialsPayToParty orderBy="partyIdTo, effectiveDate DESC"/>
        <@headerCell title=uiLabelMap.AccountingAmount orderBy="amount, effectiveDate DESC" blockClass="textright"/>
        <@headerCell title=uiLabelMap.FinancialsAmountOutstanding orderBy="openAmount, effectiveDate DESC" blockClass="textright"/>
      </tr>
      <#list pageRows as row>
        <tr class="${tableRowClass(row_index)}">
          <@displayLinkCell text=row.paymentId href="viewPayment?paymentId=${row.paymentId}"/>
          <@displayCell text=row.paymentMethodDescription/>
          <@displayDateCell date=row.effectiveDate/>
          <@displayCell text=row.paymentRefNum/>
          <@displayCell text=row.statusDescription/>
          <@displayCell text=row.partyNameFrom/>
          <@displayCell text=row.partyNameTo/>
          <@displayCurrencyCell amount=row.amount currencyUomId=row.currencyUomId/>
          <@displayCurrencyCell amount=row.openAmount currencyUomId=row.currencyUomId/>
        </tr>
      </#list>
    </table>
  </#noparse>
</@paginate>
