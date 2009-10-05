<#--
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

<@form name="createForecastForm" url="createForecast">

  <@inputHidden name="parentPeriodId" value=parentPeriodId/>
  <@inputHidden name="currencyUomId" value=configProperties.defaultCurrencyUomId!/>
  <@inputHidden name="organizationPartyId" value=organizationPartyId />

  <table class="listTable">
    <tr class="listTableHeader">
      <@displayCell text=uiLabelMap.CrmPeriod/>
      <@displayCell text=uiLabelMap.CommonFromDate/>
      <@displayCell text=uiLabelMap.CommonThruDate/>
      <@displayCell text=uiLabelMap.CrmQuota/>
    </tr>
    <#list periods as period>
      <@inputHidden name="parentPeriodId" value=period.parentPeriodId index=period_index/>
      <@inputHidden name="customTimePeriodId" value=period.customTimePeriodId index=period_index/>
      <@inputHidden name="salesForecastId" value=period.salesForecastId! index=period_index/>
      <@inputHidden name="organizationPartyId" value=period.organizationPartyId index=period_index/>
      <@inputHidden name="currencyUomId" value=configProperties.defaultCurrencyUomId! index=period_index/>
      <tr class="${tableRowClass(period_index)}">
        <@displayCell text="${period.periodName!} ${period.periodNum!}" />
        <@displayDateCell date=period.fromDate! format="DATE_ONLY"/>
        <@displayDateCell date=period.thruDate! format="DATE_ONLY" />
        <td>
          <@inputText name="quotaAmount" size=20 index=period_index/>
        </td>
      </tr>
    </#list>

    <tr>
      <td colspan="3"/>
      <td><@inputSubmit title=uiLabelMap.CrmForecastEnterQuotas /></td>
    </tr>
  </table>
  <@inputHiddenRowCount list=periods />
</@form>
