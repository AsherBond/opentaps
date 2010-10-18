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
<#-- Copyright (c) Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if hasUpdatePermission?exists>
  <#assign updateLink = "<a class='subMenuButton' href='updateCaseForm?custRequestId=" + case.custRequestId + "'>" + uiLabelMap.CommonEdit + "</a>"/>
  <#if hasClosePermission?exists>
    <@form name="closeActionForm" url="closeCase" custRequestId=case.custRequestId />
    <#assign closeLink><@submitFormLinkConfirm form="closeActionForm" text=uiLabelMap.CrmCloseCase class="subMenuButtonDangerous" /></#assign>
  </#if>
</#if>

<#assign title>
  ${uiLabelMap.CrmCase}
  <#if caseClosed?exists><span class="subSectionWarning">${uiLabelMap.CrmCaseClosed}</span></#if>
</#assign>

<@frameSectionHeader title=title extra="${updateLink?if_exists}${closeLink?if_exists}" />
