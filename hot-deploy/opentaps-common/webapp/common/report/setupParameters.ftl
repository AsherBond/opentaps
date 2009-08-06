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
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<script type="text/javascript">
    opentaps.addOnLoad(function(){fixLayout() /*QueryBuilder JSNI function*/});
</script>


<#if useQueryBuilder?has_content && useQueryBuilder == true>

    <@gwtWidget id="queryBuilderWidget" environment="opentaps"/>

<#else>
<form method="post" name="SetupReportParametersForm" action="<@ofbizUrl>runReport</@ofbizUrl>">
    <@inputHidden name="reportId" value="${parameters.reportId}"/>
    <@inputHidden name="parametersTypeJSON" value="${parametersTypeJSON?if_exists?html}"/>
    <@inputHidden name="printout" value="N"/>
    <#if organizationPartyId?has_content>
        <@inputHidden name="organizationPartyId" value="${organizationPartyId}"/>
    <#elseif parameters.organizationPartyId?has_content>
        <@inputHidden name="organizationPartyId" value="${parameters.organizationPartyId}"/>
    </#if>

<div class="subSectionBlock">
    <@sectionHeader title="${report?if_exists.shortName}"/>
    <table class="twoColumnForm">    
    
    <#if reportParameters?has_content>
       <#list reportParameters as param>
            <#if param.type == "Timestamp" || param.type == "Date" || param.type == "Time">
                <@inputDateTimeRow title="${uiLabelMap.get(param.name)}" name="${param.name}"/>
            <#elseif param.type == "Boolean">
                <@inputIndicatorRow title="${uiLabelMap.get(param.name)}" name="${param.name}" required=false/>
            <#else>
                <#-- Well known parameters can be formated in specific way here -->
                <#if param.name == "facilityId"> 
                   <@inputSelectRow title=uiLabelMap.ProductFacility name="facilityId" list=facilities required=true titleClass="requiredField" displayField="facilityName"/>
                <#elseif param.name == "supplierId">
                   <@inputLookupRow title=uiLabelMap.PartySupplier name="supplierId" lookup="LookupSupplier" form="SetupReportParametersForm"/>
                <#elseif param.name == "productId"> 
                   <@inputLookupRow title=uiLabelMap.ProductProduct name="productId" lookup="LookupProduct" form="SetupReportParametersForm"/>
                <#else>
                    <#-- Default case, simple text input --> 
                    <@inputTextRow title="${uiLabelMap.get(param.name)}" name="${param.name}"/>
                </#if>   
           </#if>
           <#if (param.description)?has_content>
                <@displayRow title="" text="${param.description?html}"/>
           </#if>
        </#list>
    </#if>
    
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
   		        <@inputSubmit title="${uiLabelMap.CommonPrint}" onClick="javascript:document.SetupReportParametersForm.printout.value='Y'; document.SetupReportParametersForm.submit();"/>
            </td>
        </tr>
        </#if>
        <tr><td colspan="2">&nbsp;</td></tr>
        <@inputSubmitRow title="${uiLabelMap.OpentapsRunReport}" onClick="javascript:document.SetupReportParametersForm.printout.value='N'; document.SetupReportParametersForm.submit();"/>
    </table>
</div>
</form>
</#if>
