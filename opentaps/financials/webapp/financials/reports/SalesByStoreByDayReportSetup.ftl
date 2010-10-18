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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<@frameSection title=uiLabelMap.FinancialsSalesByStoreByDayReport>
    <form method="post" name="SalesByStoreByDayReportSetupForm" action="<@ofbizUrl>SalesByStoreByDayReportPrepareData</@ofbizUrl>">
        <table class="twoColumnForm">
            <@inputHidden name="organizationPartyId" value="${parameters.organizationPartyId}"/>
            <@inputHidden name="reportPath" value="${reportPath}"/>
            <@inputSelectRow title=uiLabelMap.ProductProductStore name="productStoreId" list=productStores required=false displayField="storeName"/>
            <@inputDateTimeRow name="fromDate" title="${uiLabelMap.CommonFromDate}" default=thirtyDaysAgo?if_exists/>
            <@inputDateTimeRow name="thruDate" title="${uiLabelMap.CommonThruDate}" default=today?if_exists/>
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
</@frameSection>
