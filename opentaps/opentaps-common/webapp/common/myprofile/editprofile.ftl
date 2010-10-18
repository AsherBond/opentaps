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

<form action="<@ofbizUrl>updateMyProfile</@ofbizUrl>" method="post">
  <table class="twoColumnForm">
    <@inputHidden name="partyId" value=person.partyId />
    <#assign prefix={
             uiLabelMap.CommonTitleMr:uiLabelMap.CommonTitleMr,
             uiLabelMap.CommonTitleMrs:uiLabelMap.CommonTitleMrs,
             uiLabelMap.CommonTitleMs:uiLabelMap.CommonTitleMs,
             uiLabelMap.CommonTitleDr:uiLabelMap.CommonTitleDr
             }/>
    <@inputSelectHashRow title=uiLabelMap.PartyPersonalTitle name="personalTitle" hash=prefix default=(person.personalTitle)! required=false />
    <@inputTextRow name="firstName" title=uiLabelMap.PartyFirstName default=person.firstName />
    <@inputTextRow name="middleName" title=uiLabelMap.PartyMiddleName default=person.middleName! />
    <@inputTextRow name="lastName" title=uiLabelMap.PartyLastName default=person.lastName />
    <tr>
      <td class="titleCell"><span class="tableheadtext">${uiLabelMap.OpentapsUserLocale}</span></td>
      <td>
        <select name="newLocale" class="selectBox">
          <#list locales as thisLocale>
          <#if locale.toString() == thisLocale.toString()><#assign selected = "selected"><#else><#assign selected = ""></#if>
          <option ${selected} value="${thisLocale.toString()}">${thisLocale.getDisplayName(locale)}</option>
          </#list>
        </select>
      </td>
    </tr>
    <@inputSubmitRow title=uiLabelMap.CommonUpdate />
  </table>
</form>
