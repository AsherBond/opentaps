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

<@paginate name="${quoteListName}" list=quotes>
  <#noparse>
    <@navigationHeader />
    <table class="crmsfaListTable">
      <tr class="crmsfaListTableHeader">
        <@headerCell title=uiLabelMap.CrmQuoteNameAndID orderBy="quoteName" blockClass="tableheadtext"/>
        <@headerCell title=uiLabelMap.OrderOrderQuoteIssueDate orderBy="issueDate" blockClass="tableheadtext"/>
        <@headerCell title=uiLabelMap.CrmAccount orderBy="partyId" blockClass="tableheadtext"/>
        <@headerCell title=uiLabelMap.CommonStatus orderBy="statusId" blockClass="tableheadtext"/>
      </tr>
      <#list pageRows as row>
        <tr class="${tableRowClass(row_index)}">
          <@displayLinkCell href="ViewQuote?quoteId=${row.quoteId}" text="${row.quoteName?if_exists} (${row.quoteId})" class="linktext"/>
          <@displayDateCell date=row.issueDate format="DATE" />            
          <@displayCell text="${row.partyName?if_exists} (${row.partyId?if_exists})" />    
          <@displayCell text=row.statusDescription />            
        </tr>
      </#list>
    </table>
  </#noparse>
</@paginate>
