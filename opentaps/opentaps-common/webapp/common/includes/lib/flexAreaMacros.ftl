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

<#-- Sections and screenlet macros -->

<#macro frameSectionTitleBar title extra="" titleId="" titleClass="" style="">
<div class="titleBar">
  <div class="frameSectionHeader" style="${style}">
    <div class="x-panel-tl">
      <div class="x-panel-tr">
        <div class="x-panel-tc">
          <div class="x-panel-header<#if titleClass?has_content> ${titleClass}</#if>" style="float:left" id="${titleId}">${title}</div>
          <div class="frameSectionExtra">${extra}</div>
        </div>
      </div>
    </div>
    <div class="x-panel-bl">
      <div class="x-panel-br">
        <div class="x-panel-bc"></div>
      </div>
    </div>
  </div>
</div>
</#macro>

<#macro frameSectionHeader title extra="" titleId="" class="frameSectionHeader" titleClass="" style="">
  <div class="${class}" style="${style}">
    <div class="x-panel-tl">
      <div class="x-panel-tr">
        <div class="x-panel-tc">
          <div class="x-panel-header<#if titleClass?has_content> ${titleClass}</#if>" id="${titleId}">${title}</div>
          <#if extra?has_content><div class="frameSectionExtra">${extra}</div></#if>
        </div>
      </div>
    </div>
  </div>
</#macro>

<#macro frameSection title extra="" innerStyle="" style="" class="frameSection" headerClass="frameSectionHeader">
<div class="${class}" style="${style}">
  <@frameSectionHeader title=title extra=extra class=headerClass/>
  <div class="frameSectionBody" style="${innerStyle}">
    <#nested/>
  </div>
</div>
</#macro>

<#macro sectionHeader title headerClass="subSectionHeader" titleClass="subSectionTitle">
  <#assign extra>
    <#nested/>
  </#assign>
  <@frameSectionHeader title=title extra=extra/>
</#macro>

<#-- FlexAreas for public consumption -->

<#macro flexArea targetId title="" class="" style="" controlClassOpen="" controlClassClosed="" controlStyle="" decoratorStyle="" state="" defaultState="closed" save=false enabled=true>
    <@flexAreaHeader targetId=targetId title=title controlClassOpen=controlClassOpen controlClassClosed=controlClassClosed decoratorStyle=decoratorStyle state=state defaultState=defaultState controlStyle=controlStyle save=save enabled=enabled/>
    <@flexAreaBody targetId=targetId class=class style=style state=state defaultState=defaultState><#nested/></@flexAreaBody>
</#macro>

<#macro flexAreaClassic targetId title="" class="" style="" state="" defaultState="closed" save=false enabled=true headerContent="">
    <@flexAreaHeaderClassic targetId=targetId title=title state=state defaultState=defaultState save=save enabled=enabled content=headerContent/>
    <@flexAreaBody targetId=targetId class=class style=style state=state defaultState=defaultState><#nested/></@flexAreaBody>
</#macro>


<#-- FlexArea header and body components -->


<#macro flexAreaHeader targetId title="" controlClassOpen="" controlClassClosed="" controlStyle="" decoratorStyle="" state="" defaultState="closed" save=false enabled=true content="">
    <#assign openControlClass = controlClassOpen?has_content?string(controlClassOpen, "flexAreaControl_open")/>
    <#assign closedControlClass = controlClassClosed?has_content?string(controlClassClosed, "flexAreaControl_closed")/>
    <span ${flexAreaHeaderData(targetId, openControlClass, closedControlClass, controlStyle, state, defaultState, save, enabled)}>
        ${title?default("&nbsp;")}
    </span>
    <#if ! controlClassClosed?has_content>
        <div class="flexAreaControl_decorator" <#if decoratorStyle?has_content>style="${decoratorStyle}"</#if>>&nbsp;</div>
    </#if>
</#macro>

<#macro flexAreaHeaderClassic targetId title="" state="" defaultState="closed" save=false enabled=true content="">
  <div class="frameSectionHeader">
    <div class="x-panel-tl">
      <div class="x-panel-tr">
        <div ${flexAreaHeaderData(targetId, "x-panel-tc subSectionHeader_open", "x-panel-tc subSectionHeader_closed", "", state, defaultState, save, enabled)}>
          <div class="x-panel-header" style="float:left">
            <span onclick="opentaps.expandCollapse('${targetId}')">${title}</span>
          </div>
          <div class="frameSectionExtra">${content}
            <span onclick="opentaps.expandCollapse('${targetId}')" class="subSectionHeader_toggle">&nbsp;</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</#macro>

<#macro flexAreaBody targetId class="" style="" state="" defaultState="closed">
    <#assign openContainerClass = class?has_content?string(class, "flexAreaContainer_open")/>
    <#assign closedContainerClass = "flexAreaContainer_closed"/>
    <div id="${targetId}" open="${isOpen(targetId, state, defaultState)?string}" class="${openOrClosedClass(targetId, openContainerClass, closedContainerClass, state, defaultState)}" style="${isOpen(targetId, state, defaultState)?string("", "display:none;")} ${style}" openContainerClass="${openContainerClass}" closedContainerClass="${closedContainerClass}">
        <#nested>
    </div>
</#macro>


<#-- FlexArea helper functions -->


<#function isOpen domId overrideState="" defaultState="">
    <#if overrideState == "open">
      <#return true/>
    <#elseif overrideState == "closed">
      <#return false/>
    </#if>
    <#return "open" == foldedStates?default({})[domId]?default(defaultState)/>
</#function>

<#function openOrClosedClass domId openClassName closedClassName default="" defaultState="">
    <#return isOpen(domId, default, defaultState)?string(openClassName, closedClassName)/>
</#function>

<#function flexAreaHeaderData targetId controlClassOpen="" controlClassClosed="" controlStyle="" state="" defaultState="closed" save=false enabled=true>
    <#assign data> id="${targetId}_flexAreaControl" class="${openOrClosedClass(targetId, controlClassOpen, controlClassClosed, state, defaultState)}" style="${controlStyle}" openControlClass="${controlClassOpen}" closedControlClass="${controlClassClosed}" application="${opentapsApplicationName?if_exists}" screenName="${requestAttributes._CURRENT_VIEW_?if_exists}" save="${save?string}" enabled="${enabled?string}"</#assign>
    <#return data/>
</#function>

