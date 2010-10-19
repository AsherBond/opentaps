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
<#-- Copyright (c) Open Source Strategies, Inc. -->
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if workEffort?has_content>

<form name="viewActivity" onsubmit="javascript:submitFormDisableSubmits(this)" class="basic-form" action="" method="post">
    <table cellspacing="0">
    <tbody>
        <tr>
            <td class="label"><span class="tableheadtext">${uiLabelMap.CrmActivity}</span></td>
            <td colspan="4">  
                <span class="tabletext">${workEffort.workEffortName}</span>
            </td>            
        </tr>	            
        <tr>
            <td class="label"><span class="tableheadtext">${uiLabelMap.CommonStatus}</span></td>
            <td colspan="4">  
                <span class="tabletext">${workEffort.localizedCurrentStatus!}</span>
            </td>            
        </tr>	      
        <#if localizedScopeEnumDescription?has_content>
        <tr>
            <td class="label"><span class="tableheadtext">${uiLabelMap.CrmActivitySecurityScope}</span></td>
            <td colspan="4">  
                <span class="tabletext">${workEffort.localizedScopeEnumDescription!}</span>
            </td>            
        </tr>
        </#if>
        <#if salesOpportunity?has_content>
            <tr>
                <td class="label"><span class="tableheadtext">${uiLabelMap.CrmOpportunity}</span></td>
                <td colspan="4">  
                    <span class="tabletext">${salesOpportunity.get('opportunityName', 'CRMSFAUiLabels', locale)} (${salesOpportunity.get('salesOpportunityId', 'CRMSFAUiLabels', locale)})</span>
                    <a class="buttontext" href="<@ofbizUrl>viewOpportunity?salesOpportunityId=${workEffort.salesOpportunityId}</@ofbizUrl>">${uiLabelMap.CommonView}</a>
                </td>
            </tr>        
        </#if>	    
        <#if custRequest?has_content>
            <tr>
                <td class="label"><span class="tableheadtext">${uiLabelMap.CrmCase}</span></td>
                <td colspan=4>  
                    <span class="tabletext">${custRequest.get('custRequestName', 'CRMSFAUiLabels', locale)} (${custRequest.get('custRequestId', 'CRMSFAUiLabels', locale)})</span>
                    <a class="buttontext" href="<@ofbizUrl>viewCase?custRequestId=${workEffort.custRequestId}</@ofbizUrl>">${uiLabelMap.CommonView}</a>
                </td>                
            </tr>
        </#if>
        <tr>
            <td class="label"><span class="tableheadtext">${uiLabelMap.CrmActivityScheduledDate}</span></td>        
            <@displayDateCell date=workEffort.estimatedStartDate />
            <td class="label"><span class="tableheadtext">${uiLabelMap.CrmActivityActualStartDate}</span></td>        
            <@displayDateCell date=workEffort.actualStartDate />                            
        </tr>
        <tr>
            <td class="label"><span class="tableheadtext">${uiLabelMap.CrmActivityDueDate}</span></td>        
            <@displayDateCell date=workEffort.estimatedCompletionDate />
            <td class="label"><span class="tableheadtext">${uiLabelMap.CrmActivityActualCompletionDate}</span></td>        
            <@displayDateCell date=workEffort.actualCompletionDate />                            
        </tr>        
        <tr>
            <td class="label"><span class="tableheadtext">${uiLabelMap.FormFieldTitle_locationDesc}</span></td>
            <td colspan="4">  
                <span class="tabletext">${workEffort.locationDesc?if_exists}</span>
            </td>            
        </tr>
        <tr>
            <td class="label"><span class="tableheadtext">${uiLabelMap.CommonDescription}</span></td>
            <td colspan="4">  
                <span class="tabletext">${workEffort.description?if_exists}</span>
            </td>            
        </tr>
        
    </tbody>
    </table>
</form>
        
</#if>
