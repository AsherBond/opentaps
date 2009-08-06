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
<@sectionHeader title="${agreementTypeDescription?if_exists} ${uiLabelMap.OrderNbr}${agreementId}">
    <div class="subMenuBar">
        <#if isEditable?is_boolean && isEditable == true>
            <#assign editTargetAction = editAction?default("editAgreement")/>
            <#assign changeStatusAction = changeStatusAction?default("changeAgreementStatus")/>
    		<#list statusItems as validStatus>
    			<@displayLink href="${changeStatusAction}?agreementId=${agreementId}&statusId=${validStatus.statusIdTo}" text=validStatus.get("transitionName") class="subMenuButton"/>
    		</#list>
            <@displayLink href="${editTargetAction}?agreementId=${agreementId}" text="${uiLabelMap.CommonEdit}" class="subMenuButton"/>
        </#if>
    </div>
</@sectionHeader>
