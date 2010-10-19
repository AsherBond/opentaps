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

<#if parameters.partyId?exists>
    <#assign donePageEscaped = donePage + "?partyId%3d" + parameters.partyId>
</#if>

<#if isInternalMessageSender>

<form action="<@ofbizUrl>sendInternalMessage</@ofbizUrl>" name="sendInternalMessageForm">
<div id="headersPane">
            <table>
                <tr>
                    <td colspan="2" style="text-align: right; font-weight: bold;">
                        <@displayLink text=uiLabelMap.OpentapsSendMessage href="javascript: window.helper.submit(document.sendInternalMessageForm);"/>
                        <@displayLink id="separator" text=uiLabelMap.OpentapsHelp href="#"/>
                    </td>
                    <td></td>
                </tr>
                <tr>
                    <@displayCell text=uiLabelMap.CommonTo blockClass="headerTitle"/>
                    <td style="padding-right: 5px;">
                        <input name="partyIdToAsString" <#if partyIdToAsString?has_content>value="${partyIdToAsString}"</#if>class="inputBox" style="width: 100%;"/>
                    </td>
                    <td width="20px">
                        <a href="javascript:call_fieldlookup2(document.sendInternalMessageForm.partyIdToAsString,'LookupInternalAddressBook');"><img src="/images/fieldlookup.gif" width="16" height="16" border="0" alt="Lookup"/></a>
                    </td>
                </tr>
                <tr>
                    <@displayCell text=uiLabelMap.OpentapsSubject blockClass="headerTitle"/>
                    <td style="padding-right: 5px;">
                        <input name="subject" <#if message?has_content>value="${message.subject}" </#if>class="inputBox" style="width: 100%;"/>
                    </td>
                    <td></td>
                </tr>
            </table>
</div>

<div id="bodyPane">
    <@inputTextareaRow title="" name="message" default=message?if_exists.content?if_exists/>
</div>

</form>

</#if>
