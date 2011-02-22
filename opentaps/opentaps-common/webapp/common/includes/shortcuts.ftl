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

<#if shortcutGroups?has_content>
  <#list shortcutGroups as sg>
    <@frameSection title=uiLabelMap.get(sg.uiLabel?default("OpentapsShortcuts"))>
      <ul class="shortcuts">
        <#list sg.allowedShortcuts as shortcut>
          <#if sg.showAsDisabled() || shortcut.showAsDisabled()>
            <li class="disabled"><div>${uiLabelMap.get(shortcut.uiLabel!)}</div></li>
          <#else>
            <#assign shortcutClass = (parameters.thisRequestUri?default("") == shortcut.linkUrl!)?string("class=\"selected\"", "")/>
            <li><a href="<@ofbizUrl>${shortcut.linkUrl}</@ofbizUrl>" ${shortcutClass}>${uiLabelMap.get(shortcut.uiLabel!)}</a></li>
          </#if>
        </#list>
      </ul>
    </@frameSection>
  </#list>
</#if>
