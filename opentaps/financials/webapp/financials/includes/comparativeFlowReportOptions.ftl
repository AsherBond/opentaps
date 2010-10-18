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
 *  
-->

<#-- 
Re-usable form for comparative state reports (between two dates or two time periods) 
Make sure you call parseComparativeFlowOptions.bsh so that this form can work.
@author Chris Liberty (cliberty@opensourcestrategies.com)
-->

<@import location="component://financials/webapp/financials/includes/commonReportMacros.ftl"/>


<#-- preserve the last checked date options -->
<#if customTimePeriodId?exists || fromTimePeriodId?exists>
  <#assign byTimePeriodChecked = "checked=''">
  <#else>
  <#assign byDateChecked = "checked=''">
</#if>

<@commonReportJs formName="comparativeReportForm" />

<#-- this form needs no action since it returns to the same page, allowing multiple different views to use it in the same pattern -->
<form method="POST" name="comparativeReportForm" action="">
  <@inputHidden name="reportFormType" value="comparativeFlow"/>

  <#-- some forms need a partyId, they should define this map which contains a "label" for the label -->
  <#if partyIdInputRequested?exists>
    <@partyInput label=partyIdInputRequested.label form="comparativeReportForm" />
  </#if>

  <@compareDateRangeInputRow byDateChecked=byDateChecked! />

  <#if customTimePeriods?has_content>
    <@compareTimePeriodInputRow customTimePeriods=customTimePeriods defaultFrom=fromCustomTimePeriodId! defaultThru=thruCustomTimePeriodId! byTimePeriodChecked=byTimePeriodChecked! />
  </#if>

  <#if glFiscalTypes?has_content>
    <@compareGlFiscalTypeInputRow glFiscalTypes=glFiscalTypes defaultGlFiscalTypeId1=glFiscalTypeId1! defaultGlFiscalTypeId2=glFiscalTypeId2!/>
  </#if>

  <#if !disableTags?exists && tagTypes?has_content>
    <@accountingTagsInputs tagTypes=tagTypes />
  </#if>

  <@submitReportOptions reportRequest=reportRequest! screenRequest=screenRequest! returnPage=returnPage! returnLabel=returnLabel!/>

</form>

