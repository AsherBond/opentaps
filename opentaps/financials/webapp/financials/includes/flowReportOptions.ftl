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
Re-usable form for flow reports (between two dates or two time periods) 
Make sure you call parseReportOptions.bsh so that this form can work.
@author Leon Torres (leon@opensourcestrategies.com)
-->

<@import location="component://financials/webapp/financials/includes/commonReportMacros.ftl"/>

<#assign now = Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()>
<#assign defaultFromDate = getLocalizedDate(Static["org.ofbiz.base.util.UtilDateTime"].getDayStart(now, -30, timeZone, locale), "DATE")>
<#assign defaultThruDate = getLocalizedDate(Static["org.ofbiz.base.util.UtilDateTime"].getDayEnd(now, timeZone, locale), "DATE")>

<#-- preserve the last checked date option -->
<#if customTimePeriodId?exists || fromTimePeriodId?exists>
  <#assign byTimePeriodChecked = "checked=''">
  <#else>
  <#assign byDateChecked = "checked=''">
</#if>

<@commonReportJs formName="flowReportForm" />

<#-- this form needs no action since it returns to the same page, allowing different views to use it in the same pattern -->
<form method="POST" name="flowReportForm" action="${formTarget?if_exists}">
  <@inputHidden name="reportFormType" value="flow"/>

  <#-- some forms need a partyId, they should define this map which contains a "label" for the label -->
  <#if partyIdInputRequested?exists>
    <@partyInput label=partyIdInputRequested.label form="flowReportForm" />
  </#if>

  <#-- some forms need a productId, they should define this value -->
  <#if productIdInputRequested?exists>
    <@productInput form="stateReportForm" />
  </#if>

  <@dateRangeInputRow byDateChecked=byDateChecked! defaultFromDate=defaultFromDate defaultThruDate=defaultThruDate/>

  <#if customTimePeriods?has_content>
    <@timePeriodInputRow customTimePeriods=customTimePeriods defaultTimePeriodId=customTimePeriodId! />
  </#if>

  <#if reportRequiresGlFiscalType?default(true) && glFiscalTypes?has_content>
    <@glFiscalTypeInputRow glFiscalTypes=glFiscalTypes />
  </#if>

  <#if reportRequiresIsPosted?default(false)>
    <@isPostedInputRow default=isPosted! />
  </#if>

  <#if !disableTags?exists && tagTypes?has_content>
    <@accountingTagsInputs tagTypes=tagTypes />
  </#if>

  <@submitReportOptions reportRequest=reportRequest! screenRequest=screenRequest! returnPage=returnPage! returnLabel=returnLabel!/>

</form>
