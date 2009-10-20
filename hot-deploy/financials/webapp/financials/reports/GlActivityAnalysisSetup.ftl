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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div class="subSectionBlock">

    <div class="subSectionHeader">
        <div class="subSectionTitle">${uiLabelMap.FinancialsGlActivitySetupTitle}</div>
    </div>
    
    <form method="post" name="GlActivityAnalysisSetupForm" action="<@ofbizUrl>GlActivityAnalysisPrepareData</@ofbizUrl>">
        <table class="twoColumnForm">
            <@inputHidden name="organizationPartyId" value="${parameters.organizationPartyId}"/>
            <@inputHidden name="reportPath" value="${reportPath}"/>
            
            <@inputDateRow name="fromDate" form="GlActivityAnalysisSetupForm" title="${uiLabelMap.CommonFromDate}" default=fromDate?if_exists/>
            <@inputDateRow name="thruDate" form="GlActivityAnalysisSetupForm" title="${uiLabelMap.CommonThruDate}" default=thruDate?if_exists/>
            <@inputAutoCompleteGlAccountRow name="glAccountId" title="${uiLabelMap.AccountingGlAccount}" />

            <#-- List possible tags -->
            <#list tagTypes as tag>
              <@inputSelectRow name="tag${tag.index}" title="${tag.description}" list=tag.tagValues key="enumId" required=true default="" ; tagValue>
                ${tagValue.description}
              </@inputSelectRow>
            </#list>

            <@inputSelectRow name="glFiscalTypeId" title="${uiLabelMap.FinancialsGlFiscalType}" list=glFiscalTypes?default([]) key="glFiscalTypeId" ; glFiscalType>
                ${glFiscalType.get("description", locale)}
            </@inputSelectRow>
            <tr><td colspan="2">&nbsp;</td></tr>
            <#assign detailingHash = {"DETAILED" : "Detail", "SUMMARIZED" : "Summary"}/>
            <@inputSelectHashRow name="detailing" title="Report Option" hash=detailingHash default="DETAILED"/>
            <@inputSelectRow name="reportType" title="${uiLabelMap.OpentapsReportFormat}" list=reportTypes?default([]) key="mimeTypeId" ; mimeType>
                ${mimeType.get("description", locale)}
            </@inputSelectRow>
            <#if printers?has_content>
            <tr>
                <@displayCell text="${uiLabelMap.OpentapsSelectPrinter}" blockClass="titleCell" class="tableheadtext"/>
                <td>
                    <select name="printerName" class="inputBox">
                    <#list printers?default([]) as printer>
                    <option value="${printer}">
                        ${printer}
                    </option>
                    </#list>
                    </select>
   		            <@inputSubmit title="${uiLabelMap.CommonPrint}" onClick="javascript:document.GlActivityAnalysisSetupForm.action='${printAction}'; document.GlActivityAnalysisSetupForm.submit();"/>
                </td>
            </tr>
            </#if>
            <tr><td colspan="2">&nbsp;</td></tr>
            <#if glAccounts?has_content && glFiscalTypes?has_content>
                <@inputSubmitRow title="${uiLabelMap.OpentapsReport}" onClick=""/>
            </#if>
        </table>
    </form>
</div>
