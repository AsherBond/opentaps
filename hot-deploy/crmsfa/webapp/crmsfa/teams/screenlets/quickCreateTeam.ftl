<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

<#if (security.hasEntityPermission("CRMSFA_TEAM", "_CREATE", session))>
<form method="post" action="<@ofbizUrl>createTeam</@ofbizUrl>">
<div class="screenlet">
    <div class="screenlet-header"><div class="boxhead">${uiLabelMap.CrmNewTeam}</div></div>
    <div class="screenlet-body">
        <span class="tabletext">${uiLabelMap.CommonName}</span><br/>
        <input name="groupName" type="text" class="inputBox" size="15" maxlength="60"><br/>
        <input name="submitButton" type="submit" class="smallSubmit" value="${uiLabelMap.CommonCreate}" onClick="submitFormWithSingleClick(this)"/>
    </div>
</div>
</form>
</#if>
