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

<#-- 
This file controls the standard layout of all opentaps application sections.  It defines
whether a section is a two column style, one column style, and so on.

There are certain parameters sthat should be set in the context to help this file work:

sectionStyle:  Either "singleColumn" or "twoColumn"
sectionHeaderScreen: Location of the screen to render the section header.

If "twoColumn" is used for sectionStyle, then the following options are also available:
  sectionShortcuts:  A list of Shorcut objects for the list of shortcuts, which should ideally be generated in a shortcuts.bsh file.
  leftBarScreen: Optional location of the left bar screen to be rendered.  Use this to add other screenlets to left bar besides shortcuts.
  leftBarClass:  The style of the left bar to use, default is "left-content-column".  This is used to adjust the width of the left bar.
  centerContentClass:  The style of the center bar to use, default is "center-content-column".  This is used to adjust the width of the center content column.

-->

<#if !sectionStyle?exists><#assign sectionStyle = "singleColumn"></#if>
<#if sectionStyle = "singleColumn"><#assign singleColumn = true><#else><#assign singleColumn = false></#if>

<#if !singleColumn>
  <div id="${leftBarClass?default("left-content-column")}">
  <#if shortcutGroups?has_content>
    <#include "shortcuts.ftl">
  </#if>
  <#if leftBarScreen?exists>
    ${screens.render(leftBarScreen)}
  </#if>
  </div>
  <div id="${centerContentClass?default("center-content-column")}">
<#else>
  <div id="one-column">
</#if>

<#-- if a sectionHeaderScreen is named then use it otherwise use the default -->
<#if sectionHeaderScreen?exists>
  ${screens.render(sectionHeaderScreen)}
<#else> 
  <@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

  <#if sectionHeaderUiLabel?exists>
    <@frameSectionTitleBar title=uiLabelMap.get(sectionHeaderUiLabel) titleClass="sectionHeaderTitle" titleId="sectionHeaderTitle_${sectionName?if_exists}"/>
  </#if>
  
</#if>

<div class="allSubSectionBlocks">
  ${screens.render("component://opentaps-common/widget/screens/common/CommonScreens.xml#main-section-body")}
</div>

<#-- footer goes here because of layout issues -->
<#include "footer.ftl">
</div>
