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

<table class="twoColumnForm">
  <@displayRow title=uiLabelMap.CommonName text=contactList.contactListName!/>
  <@displayRow title=uiLabelMap.CrmContactType text=(contactList.contactMechType.get("description", locale))!/>
  <@displayRow title=uiLabelMap.CommonDescription text=contactList.description!/>
  <@displayRow title=uiLabelMap.CrmNumberOfMembers text=contactList.numberOfMembers!/>
  <#if "POSTAL_ADDRESS" == (contactList.contactMechTypeId)!>
    <@form name="calculate3605PostageAction" url="calculate3605Postage" contactListId=contactList.contactListId />
    <@form name="sortUSPSBusinessMailAction" url="sortUSPSBusinessMail" contactListId=contactList.contactListId />
    <@form name="viewUSPSSortResultAction" url="viewUSPSSortResult" contactListId=contactList.contactListId />
    <tr>
      <@displayTitleCell title=uiLabelMap.CrmCalculate3605Postage/>
      <td><@submitFormLink form="calculate3605PostageAction" text=uiLabelMap.CrmCalculate3605Postage /></td>
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.CrmSortUSPSBusinessMail/>
      <td><@submitFormLink form="sortUSPSBusinessMailAction" text=uiLabelMap.CrmSortUSPSBusinessMail /></td>
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.CrmReportUSPSHeader/>
      <td><@submitFormLink form="viewUSPSSortResultAction" text=uiLabelMap.CrmReportUSPSHeader /></td>
    </tr>
  </#if>
</table>
