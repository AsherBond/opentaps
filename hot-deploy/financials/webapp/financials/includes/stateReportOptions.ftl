<#-- 
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
 *  
-->

<#-- 
Re-usable form for state reports (between two dates or two time periods) 
Make sure you call parseReportOptions.bsh so that this form can work.
@author Leon Torres (leon@opensourcestrategies.com)
-->

<@import location="component://financials/webapp/financials/includes/commonReportMacros.ftl"/>

<#-- preserve the last checked date option -->
<#if customTimePeriodId?exists || fromTimePeriodId?exists>
  <#assign byTimePeriodChecked = "checked=\"checked\"">
<#else>
  <#assign byDateChecked = "checked=\"checked\"">
</#if>

<@commonReportJs formName="stateReportForm" />

<#-- this form needs no action since it returns to the same page, allowing multiple different views to use it in the same pattern -->
<form method="POST" name="stateReportForm" action="">
  <@inputHidden name="reportFormType" value="state" />

  <#-- some forms need a partyId, they should define this map which contains a "label" for the label -->
  <#if partyIdInputRequested?exists>
    <@partyInput label=partyIdInputRequested.label form="stateReportForm" />
  </#if>

  <#-- some forms need a productId, they should define this value -->
  <#if productIdInputRequested?exists>
    <@productInput form="stateReportForm" />
  </#if>

  <@asOfDateInputRow byDateChecked=byDateChecked! />

  <#if customTimePeriods?has_content>
    <@timePeriodInputRow customTimePeriods=customTimePeriods defaultTimePeriodId=customTimePeriodId! byTimePeriodChecked=byTimePeriodChecked!/>
  </#if>

  <#if glFiscalTypes?has_content>
    <@glFiscalTypeInputRow glFiscalTypes=glFiscalTypes />
  </#if>

  <#if !disableTags?exists && tagTypes?has_content>
    <@accountingTagsInputs tagTypes=tagTypes />
  </#if>

  <@submitReportOptions reportRequest=reportRequest! screenRequest=screenRequest! returnPage=returnPage! returnLabel=returnLabel!/>

</form>

