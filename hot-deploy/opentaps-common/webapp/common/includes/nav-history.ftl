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
<#if userLogin?exists && applicationSections?exists && historyList?has_content>

<#if historyBgColor?has_content && historyDecorationColor?has_content && historyTextColor?has_content && historyTextHoverColor?has_content>
<style type="text/css">
ul.navHistory {
    background: ${historyBgColor};
    border-top: 1px solid ${historyDecorationColor};
    border-bottom: 1px solid ${historyDecorationColor};
}

ul.navHistory a {
    color: ${historyTextColor};
    border-left: 1px solid ${historyDecorationColor};
}

ul.navHistory a:hover {
    color: ${historyTextHoverColor};
}
</style>
</#if>

<table cellpadding="0" cellspacing="0" border="0" width="100%">
  <tbody>
    <tr>
      <td>
        <ul class="navHistory">
        <li><span class="navTitle">${uiLabelMap.OpentapsNavHistoryTitle}:</span></li>
          <#list historyList as historyItem>
            <#assign tooltip = historyItem.text/>
            <#if (tooltip?length > historyItemChars)><#assign text = tooltip?substring(0, historyItemChars)+"${uiLabelMap.OpentapsNavHistoryTruncMark}"/><#else><#assign text = tooltip/></#if>
            <li><a href="<@ofbizUrl>${historyItem.get("uri")}</@ofbizUrl>" title="${tooltip?html}">${text}</a></li>
          </#list>
        </ul>
      </td>
    </tr>
  </tbody>
</table>
</#if>
