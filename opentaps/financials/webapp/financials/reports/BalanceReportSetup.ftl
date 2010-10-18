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

<@import location="component://financials/webapp/financials/includes/commonReportMacros.ftl"/>

<@frameSection title=uiLabelMap.FinancialsBalanceStatement>
    <form method="post" name="BalanceReportSetupForm" action="<@ofbizUrl>BalanceReportPrepareData</@ofbizUrl>">
        <table class="twoColumnForm">
            <@inputHidden name="organizationPartyId" value="${parameters.organizationPartyId}"/>
            <@inputHidden name="reportPath" value="${reportPath}"/>
            <@inputDateTimeRow name="fromDate" form="BalanceReportSetupForm" title="${uiLabelMap.CommonFromDate}" default=thirtyDaysAgo?if_exists/>
            <@inputDateTimeRow name="thruDate" form="BalanceReportSetupForm" title="${uiLabelMap.CommonThruDate}" default=today?if_exists/>
            <@inputCurrencySelectRow name="currencyUomId" title=uiLabelMap.CommonCurrency defaultCurrencyUomId=parameters.orgCurrencyUomId />
            <@inputIndicatorRow name="includeBudgetIncome" title=uiLabelMap.FinancialsIncludeIncomeInBudget default="N" />
            <@accountingTagsInputRow tagTypes=tagTypes/>
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
                    <@inputSubmit title="${uiLabelMap.CommonPrint}" onClick="javascript:document.BalanceReportSetupForm.action='${printAction}'; document.BalanceReportSetupForm.submit();"/>
                </td>
            </tr>
            </#if>
            <tr><td colspan="2">&nbsp;</td></tr>
            <@inputSubmitRow title="${uiLabelMap.OpentapsReport}" onClick=""/>
        </table>
    </form>
</@frameSection>
