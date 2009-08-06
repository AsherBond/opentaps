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
