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

<#if validView?default(false) && !accountDeactivated?exists>

<#if hasViewOrderPermission?exists>
    <#if continueOrder>
	  <#assign orderLink = "<a href='resumeOrder?partyId="+parameters.partyId+"'>"+uiLabelMap.OpentapsResumeOrder+"</a>" />
	<#else>
	  <#assign orderLink = "<a href='newOrder?partyId="+parameters.partyId+"'>"+uiLabelMap.OpentapsCreateOrder+"</a>" />
	</#if>
</#if>

<div class="screenlet">
    <div class="screenlet-header"><div class="boxhead">${uiLabelMap.CrmAccountShortcuts}</div></div>
    <div class="screenlet-body">
      <ul class="shortcuts">
        <#if orderLink?exists>
          <li>${orderLink}</li>
        </#if>
        <li>
            <a href='findQuotes?partyId=${parameters.partyId}'>${uiLabelMap.OrderOrderQuotes}</a>
        </li>
        <li>
            <a href='EditQuote?partyId=${parameters.partyId}'>${uiLabelMap.CrmCreateQuote}</a>
        </li>
      </ul>
    </div>
</div>
        
</#if>