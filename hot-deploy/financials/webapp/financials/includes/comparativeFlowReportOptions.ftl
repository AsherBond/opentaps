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
Make sure you call parseComparativeFlowOptions.bsh so that this form can work.
@author Chris Liberty (cliberty@opensourcestrategies.com)
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
        ${uiLabelMap.FinancialsEndingOn} ${getLocalizedDate(customTimePeriod.thruDate, "DATE_ONLY")} 
      <#if customTimePeriod.isClosed == "Y">
        (${uiLabelMap.isClosed})
      <#else>
        (${uiLabelMap.isNotClosed})
      </#if>
    </option>
  </#list>
</#macro>

<#macro listGlFiscalTypes glFiscalTypes glFiscalTypeIdToMatch>
  <#list glFiscalTypes as glFiscalType>
    <#assign selected = ""/>
    <#if glFiscalTypeIdToMatch?exists && glFiscalTypeIdToMatch == glFiscalType.glFiscalTypeId><#assign selected = "selected"/></#if>
    <option ${selected?if_exists} value="${glFiscalType.glFiscalTypeId}">${glFiscalType.description}</option>
  </#list>
</#macro>

<#-- preserve the last checked date options -->
<#if fromCustomTimePeriodId?exists || thruCustomTimePeriodId?exists>
  <#assign byTimePeriodChecked = "checked=\"checked\"">
  <#else>
  <#assign byDateChecked = "checked=\"checked\"">
</#if>

<#-- a function to "disable" the other date option when one is selected -->
<script type="text/javascript">
  <!--
  function selectReportDateOption(button) {
    if (button.checked == true) {
      if (button.value == "byDate") {
        button.form.fromDate1.disabled = false;
        button.form.thruDate1.disabled = false;
        button.form.fromDate2.disabled = false;
        button.form.thruDate2.disabled = false;
        button.form.fromCustomTimePeriodId.disabled = true;
        button.form.thruCustomTimePeriodId.disabled = true;
      }
      if (button.value == "byTimePeriod") {
        button.form.fromDate1.disabled = true;
        button.form.thruDate1.disabled = true;
        button.form.fromDate2.disabled = true;
        button.form.thruDate2.disabled = true;
        button.form.fromCustomTimePeriodId.disabled = false;
        button.form.thruCustomTimePeriodId.disabled = false;
      }
    }
  }

  function doSubmit(action, target) {
    document.comparativeReportForm.type.value = target;
    document.comparativeReportForm.reportAction.value = '';
    document.comparativeReportForm.action = action;
    document.comparativeReportForm.submit();
  }
  //-->
</script>

<#-- this form needs no action since it returns to the same page, allowing multiple different views to use it in the same pattern -->
<form method="POST" name="comparativeReportForm" action="">
  <input type="hidden" name="reportFormType" value="comparativeFlow"></input>

  <#-- some forms need a partyId, they should define this map which contains a "label" for the label -->
  <#if partyIdInputRequested?exists>
  <div style="margin-left: 5px; margin-bottom: 5px;">
    <span class="tableheadtext">${uiLabelMap.get(partyIdInputRequested.get("label"))}</span>
    <input type="text" name="partyId" size="20" maxlength="20" value="${partyId?if_exists}" class="inputBox"></input>
    <a href="javascript:call_fieldlookup2(document.comparativeReportForm.partyId, 'LookupPartyName');">
      <img src="/images/fieldlookup.gif" width="16" height="16" border="0" alt="Lookup"></img>
    </a>
  </div>
  </#if>

  <table>
    <tr>
      <td><input type="radio" name="reportDateOption" value="byDate" onClick="javascript:selectReportDateOption(this)" ${byDateChecked?default("")}></input></td>
      <td class="tableheadtext">${uiLabelMap.FinancialsCompareDates}:</td>
      <td class="tabletext">
        ${uiLabelMap.CommonFrom}
        <@inputDate name="fromDate1" default=requestParameters.fromDate1?if_exists/>
      </td>
      <td class="tabletext">
        ${uiLabelMap.CommonThru}
        <@inputDate name="thruDate1" default=requestParameters.thruDate1?if_exists/>
      </td>
    </tr>
    <tr>
      <td>&nbsp;</td>
      <td class="tableheadtext">${uiLabelMap.FinancialsCompareDatesTo}:</td>
      <td class="tabletext">
        ${uiLabelMap.CommonFrom}
        <@inputDate name="fromDate2" default=requestParameters.fromDate2?if_exists/>
      </td>
      <td class="tabletext">
        ${uiLabelMap.CommonThru}
        <@inputDate name="thruDate2" default=requestParameters.thruDate2?if_exists/>
      </td>
    </tr>
  </table>

  <#if customTimePeriods.size() != 0>
  <table>
    <tr>
      <td><input type="radio" name="reportDateOption" value="byTimePeriod" onClick="javascript:selectReportDateOption(this)" ${byTimePeriodChecked?default("")}></input></td>
      <td class="tableheadtext">${uiLabelMap.FinancialsCompareTimePeriod}</td>
      <td class="tabletext">
        <select class="selectBox" name="fromCustomTimePeriodId" size="1">
          <@listCustomTimePeriods customTimePeriods=customTimePeriods defaultTimePeriodId=fromCustomTimePeriodId?default("")/>
        </select> 
      </td>
    </tr>
    <tr>
      <td>&nbsp;</td>
      <td class="tableheadtext">${uiLabelMap.FinancialsCompareTimePeriodTo}</td>
      <td class="tabletext">
        <select class="selectBox" name="thruCustomTimePeriodId" size="1">
          <@listCustomTimePeriods customTimePeriods=customTimePeriods defaultTimePeriodId=thruCustomTimePeriodId?default("")/>
        </select> 
      </td>
    </tr>
  </table>
  </#if>

  <div style="margin-left: 30px; margin-top: 5px;">
    <span class="tableheadtext">${uiLabelMap.FinancialsCompareGlFiscalType}</span>
    <select class="selectBox" name="glFiscalTypeId1" size="1"><@listGlFiscalTypes glFiscalTypes=glFiscalTypes glFiscalTypeIdToMatch=glFiscalTypeId1?if_exists/></select>
    <span class="tableheadtext">${uiLabelMap.CommonTo}</span>
    <select class="selectBox" name="glFiscalTypeId2" size="1"><@listGlFiscalTypes glFiscalTypes=glFiscalTypes glFiscalTypeIdToMatch=glFiscalTypeId2?if_exists/></select>
  </div>

  <#-- List possible tags -->
  <#list tagTypes as tag>
    <div style="margin-left: 30px; margin-top: 5px;">
      <span class="tableheadtext">${tag.description}</span>
      <@inputSelect name="tag${tag.index}" list=tag.tagValues key="enumId" required=true ; tagValue>
        ${tagValue.description}
      </@inputSelect>
    </div>
  </#list>

  <div style="margin-left: 30px; margin-top: 10px;">
    <@inputHidden name="type" value="pdf"/>
    <#if reportRequest?has_content && screenRequest?has_content>
        <@selectAction name="reportAction" prompt="Run Report as...">
            <@action url="javascript: doSubmit('${screenRequest}', 'screen')" text="Screen"/>
            <@action url="javascript: doSubmit('${reportRequest}', 'pdf')" text="Portable Document (*.pdf)"/>
            <@action url="javascript: doSubmit('${reportRequest}', 'xls')" text="Excel Spreadsheet (*.xls)"/>
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

