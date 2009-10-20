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
        <div class="subSectionTitle">${uiLabelMap.FinancialsSalesTaxStatement}</div>
    </div>
    
    <form method="post" name="SalesTaxReportSetupForm" action="<@ofbizUrl>SalesTaxReportPrepareData</@ofbizUrl>">
        <table class="twoColumnForm">
            <@inputHidden name="organizationPartyId" value="${parameters.organizationPartyId}"/>
            <@inputHidden name="reportPath" value="${reportPath}"/>
            <@inputDateRow name="fromDate" form="SalesTaxReportSetupForm" title="${uiLabelMap.CommonFromDate}" default=thirtyDaysAgo?if_exists/>
            <@inputDateRow name="thruDate" form="SalesTaxReportSetupForm" title="${uiLabelMap.CommonThruDate}" default=today?if_exists/>
            <@inputCurrencySelectRow name="currencyUomId" title=uiLabelMap.CommonCurrency defaultCurrencyUomId=parameters.orgCurrencyUomId />            
            <tr><td colspan="2">&nbsp;</td></tr>
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
                    <@inputSubmit title="${uiLabelMap.CommonPrint}" onClick="javascript:document.SalesTaxReportSetupForm.action='${printAction}'; document.SalesTaxReportSetupForm.submit();"/>
                </td>
            </tr>
            </#if>
            <tr><td colspan="2">&nbsp;</td></tr>            
            <@inputSubmitRow title="${uiLabelMap.OpentapsReport}" onClick=""/>
        </table>
    </form>
</div>
