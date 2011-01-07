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
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<script type="text/javascript">
    opentaps.addOnLoad(function(){fixLayout() /*QueryBuilder JSNI function*/});
</script>

<#--
  Local macro to display accounting tags row for given index (possible values 0-9). 
-->
<#macro tagRow index>
    <#if tagTypes?has_content && index &lt; tagTypes.size()>
        <#assign tag = tagTypes.get(index)/>
        <tr>
            <td class="titleCell"><span class="tableheadtext">${tag.description}</td>
            <td><@accountingTagsSelect tag=tag activeOnly=false /></td>
        </tr>
    </#if>
</#macro>

<#if useQueryBuilder?has_content && useQueryBuilder == true>

    <@gwtWidget id="queryBuilderWidget" environment="opentaps"/>

<#else>
<#if report?has_content>
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
                <@inputDateTimeRow title="${uiLabelMap.get(param.name)}" name="${param.name}" default=context.get("${param.name}")?default(parameters.get("${param.name}"))?if_exists />
            <#elseif param.type == "Boolean">
                <@inputIndicatorRow title="${uiLabelMap.get(param.name)}" name="${param.name}" required=false/>
            <#else>
                <#-- Well known parameters can be formated in specific way here -->
                <#if param.name == "facilityId"> 
                   <@inputSelectRow title=uiLabelMap.ProductFacility name="facilityId" list=facilities required=true titleClass="requiredField" displayField="facilityName" default=context.get("${param.name}")?default(parameters.get("${param.name}"))?if_exists/>
                <#elseif param.name == "supplierId">
                   <@inputLookupRow title=uiLabelMap.PartySupplier name="supplierId" lookup="LookupSupplier" form="SetupReportParametersForm" default=context.get("${param.name}")?default(parameters.get("${param.name}"))?if_exists/>
                <#elseif param.name == "productId"> 
                   <@inputLookupRow title=uiLabelMap.ProductProduct name="productId" lookup="LookupProduct" form="SetupReportParametersForm" default=context.get("${param.name}")?default(parameters.get("${param.name}"))?if_exists/>
                <#elseif param.name == "tag1"> <#-- accounting tags handling -->
                    <@tagRow index=0 />
                <#elseif param.name == "tag2">
                    <@tagRow index=1 />
                <#elseif param.name == "tag3">
                    <@tagRow index=2 />
                <#elseif param.name == "tag4">
                    <@tagRow index=3 />
                <#elseif param.name == "tag5">
                    <@tagRow index=4 />
                <#elseif param.name == "tag6">
                    <@tagRow index=5 />
                <#elseif param.name == "tag7">
                    <@tagRow index=6 />
                <#elseif param.name == "tag8">
                    <@tagRow index=7 />
                <#elseif param.name == "tag9">
                    <@tagRow index=8 />
                <#elseif param.name == "tag10">
                    <@tagRow index=9 />
                <#else>
                    <#-- Default case, simple text input --> 
                    <@inputTextRow title="${uiLabelMap.get(param.name)}" name="${param.name}" default=context.get("${param.name}")?default(parameters.get("${param.name}"))?if_exists/>
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
<#else> <#-- if report object is empty -->

<div class="subSectionBlock">
  <p>${error?if_exists}</p>
</div>

</#if>
</#if>
