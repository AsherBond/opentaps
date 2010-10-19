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

<@paginate name="${casePaginatorName}" list=caseListBuilder>
<#noparse>
<@navigationBar />
<table class="crmsfaListTable">
    <tr class="crmsfaListTableHeader">
        <@headerCell title=uiLabelMap.CommonPriority orderBy="priority" blockClass="tableheadtext"/>
        <@headerCell title=uiLabelMap.PartySubject orderBy="custRequestName" blockClass="tableheadtext"/>
        <@headerCell title=uiLabelMap.CommonStatus orderBy="statusId" blockClass="tableheadtext"/>
        <@headerCell title=uiLabelMap.CommonType orderBy="custRequestTypeId" blockClass="tableheadtext"/>
        <@headerCell title=uiLabelMap.CrmReason orderBy="custRequestCategoryId" blockClass="tableheadtext"/>
    </tr>
    <#list pageRows as crmCase>
        <#assign class = "case_${crmCase.statusId}" + crmCase.updated?string(" case_updated","")>
        <tr class="${class}">
            <@displayCell text=crmCase.priority />
            <#if crmCase.sectionName?default("") == "lookup">
                 <@displayLinkCell href="javascript:set_value('${crmCase.custRequestId}')" text="${crmCase.custRequestName?if_exists} (${crmCase.custRequestId})" class="linktext" />
            <#else>
                <@displayLinkCell href="viewCase?custRequestId=${crmCase.custRequestId}" text="${crmCase.custRequestName?if_exists} (${crmCase.custRequestId})" class="linktext" />
            </#if>
            <@displayCell text=crmCase.statusDescription />    
            <@displayCell text=crmCase.typeDescription />    
            <@displayCell text=crmCase.reasonDescription />            
        </tr>
    </#list>
</table>
</#noparse>
</@paginate>