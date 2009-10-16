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
Re-usable form for comparative state reports (between two dates or two time periods) 
Make sure you call parseComparativeStateOptions.bsh so that this form can work.
@author Leon Torres (leon@opensourcestrategies.com)
-->

<@import location="component://financials/webapp/financials/includes/commonReportMacros.ftl"/>

<#-- preserve the last checked date option -->
<#if customTimePeriodId?exists || fromCustomTimePeriodId?exists>
  <#assign byTimePeriodChecked = "checked=''">
  <#else>
  <#assign byDateChecked = "checked=''">
</#if>

<@commonReportJs formName="comparativeReportForm" />

<#-- this form needs no action since it returns to the same page, allowing multiple different views to use it in the same pattern -->
<form method="POST" name="comparativeReportForm" action="">
  <@inputHidden name="reportFormType" value="comparativeState" />

  <#-- some forms need a partyId, they should define this map which contains a "label" for the label -->
  <#if partyIdInputRequested?exists>
    <@partyInput label=partyIdInputRequested.label form="comparativeReportForm" />
  </#if>

  <@compareDateInputRow byDateChecked=byDateChecked! />

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

