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

<@flexAreaClassic targetId="MyMessagesList" title=uiLabelMap.OpentapsFindMessage save=true defaultState="open" style="border:none; margin:0; padding:0" headerContent=toggleInboundView> 
<div class="subSectionBlock">
    <form action="<@ofbizUrl>myMessages</@ofbizUrl>" name="findMessagesForm" method="post">
        <@inputHidden name="performFind" value="Y"/>
        <table class="twoColumnForm">
            <@inputTextRow title=uiLabelMap.OpentapsSubject name="subjPattern" size=50/>
            <@inputTextRow title=uiLabelMap.CommonText name="textPattern" size=50/>
            <@inputTextRow title=uiLabelMap.CommonFrom name="partyIdFrom" size=20 maxlength=20/>
            <tr>
                <@displayTitleCell title=uiLabelMap.CommonFromDate/>
                <td><@inputDate name="fromDate"/>&nbsp;<@displayError name="fromDate"/></td>
            </tr>
            <tr>
                <@displayTitleCell title=uiLabelMap.CommonThruDate/>
                <td><@inputDate name="thruDate"/>&nbsp;<@displayError name="thruDate"/></td>
            </tr>
            <@inputIndicatorRow title=uiLabelMap.OpentapsShowRead name="isRead"/>
            <@inputSubmitRow title=uiLabelMap.CommonFind/>
        </table>
    </form>
</div>
</@flexAreaClassic>

<div class="subSectionBlock" style="margin-top: 5px;">
    <@sectionHeader title=uiLabelMap.OpentapsMyMessages>
        <div class="subMenuBar">
            <@displayLink href="javascript: sendMessage();" text=uiLabelMap.OpentapsComposeNew class="subMenuButton"/>
        </div>
    </@sectionHeader>

    <@paginate name="myMessages" list=internalMessages?default([]) currentPage=currentPage>
    <#noparse>
    <@navigationBar />
    <table class="listTable">
        <tr class="listTableHeader">
            <td><#-- image --></td>
            <@headerCell title=uiLabelMap.OpentapsSubject orderBy="subject" blockClass="fieldWidth50pct"/>
            <@headerCell title=uiLabelMap.CommonFrom orderBy="partyIdFrom" />
            <@headerCell title=uiLabelMap.CommonDate orderBy="entryDate" blockClass="fieldDateTime"/>
        </tr>
        <#list pageRows as row>
            <tr class="${tableRowClass(row_index)}">
                <td><#if row.statusId == "COM_ENTERED"><img src="/opentaps_images/envelopeClosed.png"/><#else><img src="/opentaps_images/envelopeOpened.png"/></#if></td>
                <@displayLinkCell href="javascript: viewMessage('viewInternalMessage?communicationEventId=${row.communicationEventId}');" text=row.subject?default("${uiLabelMap.OpentapsNoSubjectSubjectMessage}") />
                <@displayCell text=row.fromAddress?if_exists />
                <@displayDateCell date=row.entryDate />
            </tr>
        </#list>
    </table>
    </#noparse>
    </@paginate>
</div>