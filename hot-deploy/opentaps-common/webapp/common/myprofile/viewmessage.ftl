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
<#-- Copyright (c) 2005-2008 Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if parameters.partyId?exists>
    <#assign donePageEscaped = donePage + "?partyId%3d" + parameters.partyId>
</#if>

<div id="headersPane">
    <table>
        <tr>
            <td colspan="2" style="text-align: right; font-weight: bold;">
                <@displayLink text=uiLabelMap.OpentapsReply href="javascript: window.helper.replyMessage('${message.communicationEventId}');"/>
                <@displayLink id="separator" text=uiLabelMap.OpentapsForward href="javascript: window.helper.forwardMessage('${message.communicationEventId}');"/>
                <@displayLink id="separator" text=uiLabelMap.CommonDelete href="javascript: window.helper.deleteMessage('${message.communicationEventId}');"/>
                <@displayLink id="separator" text=uiLabelMap.OpentapsHelp href="#"/>
            </td>
        </tr>
        <tr>
            <@displayCell text=uiLabelMap.OpentapsSubject blockClass="headerTitle" style="font-weight: bold;"/>
            <@displayCell text=message.subject?if_exists/>
        </tr>
        <tr>
            <@displayCell text=uiLabelMap.CommonFrom blockClass="headerTitle" style="font-weight: bold;"/>
            <@displayCell text=message.partyIdFromAddress?if_exists/>
        </tr>
        <tr>
            <@displayCell text=uiLabelMap.CommonDate blockClass="headerTitle" style="font-weight: bold;"/>
            <@displayDateCell date=message.entryDate/>
        </tr>
    </table>
</div>

<div id="bodyPane">
    <@inputTextareaRow title="" name="message" default=message.content?if_exists/>
</div>
