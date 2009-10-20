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
