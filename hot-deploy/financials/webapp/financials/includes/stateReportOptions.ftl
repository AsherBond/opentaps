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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#macro listCustomTimePeriods customTimePeriods defaultTimePeriodId>
  <#list customTimePeriods as customTimePeriod>
    <#assign selected=""/>
    <#if defaultTimePeriodId == customTimePeriod.customTimePeriodId><#assign selected="selected"></#if>
    <option ${selected?if_exists} value="${customTimePeriod.customTimePeriodId}">${customTimePeriod.periodName?if_exists} 
      <#if customTimePeriod.periodNum?has_content>
        ${customTimePeriod.periodNum?string("####")}
      </#if> 
        ${uiLabelMap.FinancialsEndingOn} <#if customTimePeriod.thruDate?has_content>${getLocalizedDate(customTimePeriod.thruDate, "DATE_ONLY")}</#if> 
      <#if customTimePeriod.isClosed == "Y">
        (${uiLabelMap.isClosed})
      <#else>
        (${uiLabelMap.isNotClosed})
      </#if>
    </option>
  </#list>
</#macro>

<#macro listGlFiscalTypes glFiscalTypes>
  <#list glFiscalTypes as glFiscalType>
    <#assign selected = ""/>
    <#if glFiscalTypeId?exists && glFiscalTypeId == glFiscalType.glFiscalTypeId><#assign selected = "selected"/></#if>
    <option ${selected?if_exists} value="${glFiscalType.glFiscalTypeId}">${glFiscalType.description}</option>
  </#list>
</#macro>

<#-- preserve the last checked date option -->
<#if customTimePeriodId?exists || fromTimePeriodId?exists>
  <#assign byTimePeriodChecked = "checked=''">
  <#else>
  <#assign byDateChecked = "checked=''">
</#if>

<#-- a function to "disable" the other date option when one is selected -->
<script type="text/javascript">
  <!--
  function selectReportDateOption(button) {
    if (button.checked == true) {
      if (button.value == "byDate") {
        button.form.asOfDate.disabled = false;
        button.form.customTimePeriodId.disabled = true;
      }
      if (button.value == "byTimePeriod") {
        button.form.asOfDate.disabled = true;
        button.form.customTimePeriodId.disabled = false;
      }
    }
  }

  function doSubmit(action, target) {
    document.stateReportForm.type.value = target;
    document.stateReportForm.reportAction.value = '';
    document.stateReportForm.action = action;
    document.stateReportForm.submit();
  }
  //-->
</script>

<#-- this form needs no action since it returns to the same page, allowing multiple different views to use it in the same pattern -->
<form method="POST" name="stateReportForm" action="">
  <input type="hidden" name="reportFormType" value="state"></input>

  <#-- some forms need a partyId, they should define this map which contains a "label" for the label -->
  <#if partyIdInputRequested?exists>
  <div style="margin-left: 5px; margin-bottom: 5px;">
    <span class="tableheadtext">${uiLabelMap.get(partyIdInputRequested.get("label"))}</span>
    <input type="text" name="partyId" size="20" maxlength="20" value="${partyId?if_exists}" class="inputBox"></input>
    <a href="javascript:call_fieldlookup2(document.stateReportForm.partyId, 'LookupPartyName');">
      <img src="/images/fieldlookup.gif" width="16" height="16" border="0" alt="Lookup"></img>
    </a>
  </div>
  </#if>

  <#-- some forms need a productId, they should define this value -->
  <#-- TODO: use a macro to generate filter options like in product lookup -->
  <#if productIdInputRequested?exists>
  <div style="margin-left: 5px; margin-bottom: 5px;">
    <span class="tableheadtext">${uiLabelMap.ProductProductId}</span>
    <input type="text" name="productId" size="20" maxlength="20" value="${productId?if_exists}" class="inputBox"></input>
  </div>
  </#if>

  <table>
    <tr>
      <td><input type="radio" name="reportDateOption" value="byDate" onClick="javascript:selectReportDateOption(this)" ${byDateChecked?default("")}></input></td>
      <td class="tableheadtext">${uiLabelMap.AsOf}</td>
      <td>
        <#if requestParameters.asOfDate?exists>
          <#assign date = requestParameters.asOfDate />
        <#else>
          <#assign date = defaultAsOfDate />
        </#if>
        <@inputDate name="asOfDate" default=date?if_exists/>
      </td>
    </tr>
  </table>

  <#if customTimePeriods.size() != 0>
  <table>
    <tr>
      <td><input type="radio" name="reportDateOption" value="byTimePeriod" onClick="javascript:selectReportDateOption(this)" ${byTimePeriodChecked?default("")}></input></td>
      <td class="tableheadtext">${uiLabelMap.AccountingTimePeriod}</td>
      <td class="tabletext">
        <select class="selectBox" name="customTimePeriodId" size="1">
          <@listCustomTimePeriods customTimePeriods =customTimePeriods defaultTimePeriodId=customTimePeriodId?default("")/>
        </select> 
      </td>
    </tr>
  </table>
  </#if>

  <div style="margin-left: 30px; margin-top: 5px;">
    <span class="tableheadtext">${uiLabelMap.GlFiscalType}</span>
    <select class="selectBox" name="glFiscalTypeId" size="1"><@listGlFiscalTypes glFiscalTypes=glFiscalTypes/></select>
  </div>

  <#if !disableTags?exists>
    <#-- List possible tags -->
    <#list tagTypes as tag>
      <div style="margin-left: 30px; margin-top: 5px;">
        <span class="tableheadtext">${tag.description}</span>
        <@inputSelect name="tag${tag.index}" list=tag.tagValues key="enumId" required=true ; tagValue>
          ${tagValue.description}
        </@inputSelect>
      </div>
    </#list>
  </#if>

  <div style="margin-left: 30px; margin-top: 10px;">
    <@inputHidden name="type" value="pdf"/>
    <#if reportRequest?has_content && screenRequest?has_content>
        <@selectAction name="reportAction" prompt="${uiLabelMap.OpentapsRunReportIn}">
            <@action url="javascript: doSubmit('${screenRequest}', 'screen')" text="${uiLabelMap.OpentapsReportOptionScreen}"/>
            <@action url="javascript: doSubmit('${reportRequest}', 'pdf')" text="${uiLabelMap.OpentapsReportOptionPdf}"/>
            <@action url="javascript: doSubmit('${reportRequest}', 'xls')" text="${uiLabelMap.OpentapsReportOptionXls}"/>
    <#if returnPage?exists && returnLabel?exists>
            <@action url="javascript: void()" text="${uiLabelMap.OpentapsDefaultActionSeparator}"/>
            <@action url="javascript: window.location.href='/financials/control/${returnPage}'" text="${uiLabelMap.get(returnLabel)}"/>
    </#if>            
        </@selectAction>
    <#else>
        <input type="Submit" class="smallSubmit" name="submitButton" value="${uiLabelMap.CommonRun}"></input>
    </#if>

  </div>  

</form>

