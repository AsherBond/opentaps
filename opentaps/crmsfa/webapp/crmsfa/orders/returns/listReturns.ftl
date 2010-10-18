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
        
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl" />

<div class="subSectionBlock">

<@paginate name=paginateName list=findReturnsBuilder rememberPage=rememberPage>
    <#noparse>
        <@navigationHeader title=uiLabelMap.CrmReturns />
        <table class="listTable">
            <tr class="listTableHeader">
                <@headerCell title=uiLabelMap.OrderReturnId orderBy="returnId" />
                <@headerCell title=uiLabelMap.CrmReturnFromCustomer orderBy="fromPartyId" />
                <@headerCell title=uiLabelMap.CommonStatus orderBy="statusId" />
                <@headerCell title=uiLabelMap.OpentapsDateRequested orderBy="entryDate" />
            </tr>
            <#list pageRows as return>
              <tr class="${tableRowClass(return_index)}">
                <@displayLinkCell text=return.returnId href="viewReturn?returnId=${return.returnId}" />
                <@displayCell text=return.returnPartyName />
                <@displayCell text=return.statusDescription />
                <@displayDateCell date=return.entryDate />
              </tr>
            </#list>
        </table>
    </#noparse>
</@paginate>

</div>
