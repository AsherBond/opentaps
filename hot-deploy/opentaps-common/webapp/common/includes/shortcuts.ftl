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

<div class="screenlet">
  <div class="screenlet-header"><div class="boxhead">${uiLabelMap.OpentapsShortcuts}</div></div>
  <div class="screenlet-body">
    <ul class="shortcuts">
      <#list shortcuts as shortcut>
      <#assign shortcutClass = (parameters.thisRequestUri?default("") == shortcut.uri)?string("class=\"selected\"", "")>
      <#assign shortcutParameters = shortcut.parameters?default("")/>
      <li><a href="<@ofbizUrl>${shortcut.uri}${shortcutParameters}</@ofbizUrl>" ${shortcutClass}>${uiLabelMap.get(shortcut.uiLabel)}</a></li>
      </#list>
    </ul>
  </div>
</div>
