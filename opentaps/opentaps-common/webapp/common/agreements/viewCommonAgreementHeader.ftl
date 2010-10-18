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

<#assign agreementId = parameters.agreementId/>
<#if isEditable?default(false)>
  <#assign editTargetAction = editAction?default("editAgreement")/>
  <#assign changeStatusAction = changeStatusAction?default("changeAgreementStatus")/>
  <#assign links = ""/>
  <#list statusItems as validStatus>
    <@form name="${validStatus.statusIdTo}Action" url="${changeStatusAction}" agreementId=agreementId statusId=validStatus.statusIdTo />
    <#assign links>${links}<@submitFormLink form="${validStatus.statusIdTo}Action" text=validStatus.get("transitionName") class="subMenuButton" /></#assign>
  </#list>
  <#assign links>${links}<@displayLink href="${editTargetAction}?agreementId=${agreementId}" text=uiLabelMap.CommonEdit class="subMenuButton"/></#assign>
</#if>

<@sectionHeader title="${agreementTypeDescription?if_exists} ${uiLabelMap.OrderNbr}${agreementId}">
  <div class="subMenuBar">${links?if_exists}</div>
</@sectionHeader>
