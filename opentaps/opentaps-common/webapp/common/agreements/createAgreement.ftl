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

<#if !hasCreateAgreementPermission?has_content>
    <#assign hasCreateAgreementPermission = parameters.hasCreateAgreementPermission/>
</#if>

<#if hasCreateAgreementPermission?default(false)>

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#assign currentTime = Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()/>

<div class="subSectionBlock">
    <div class="form">
        <#assign targetAction = createAgreementAction?default("processCreateAgreement")/>
        <form method="post" action="<@ofbizUrl>${targetAction}</@ofbizUrl>"  onSubmit="javascript:submitFormDisableSubmits(this)" name="createAgreementForm">
        <#if agreementTypeId?has_content><@inputHidden name="agreementTypeId" value=agreementTypeId/></#if>
        <#if partyIdTo?has_content><@inputHidden name="partyIdTo" value=partyIdTo/></#if>
        <#if partyIdFrom?has_content><@inputHidden name="partyIdFrom" value=partyIdFrom/></#if>
        <#if roleTypeIdFrom?has_content><@inputHidden name="roleTypeIdFrom" value=roleTypeIdFrom/></#if>
        <#if roleTypeIdTo?has_content><@inputHidden name="roleTypeIdTo" value=roleTypeIdTo/></#if>
        <table width="100%">
            <tr>
                <@displayTitleCell title="${uiLabelMap.AccountingAgreementDate}"/>
                <@inputDateTimeCell name="agreementDate" form="createAgreementForm" default=currentTime/>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr>
                <#if !partyIdTo?has_content>
                    <@displayTitleCell title="${uiLabelMap.AccountingPartyIdTo}" titleClass="requiredField"/>
                    <@inputAutoCompleteCrmPartyCell name="partyIdTo" size=20 />
                    <td colspan="2">&nbsp;</td>
                <#else>
                    <td colspan="4">&nbsp;</td>
                </#if>
            </tr>
            <tr>
                <@displayTitleCell title="${uiLabelMap.CommonFromDate}"/>
                <td>              
                    <@inputDateTime name="fromDate" form="createAgreementForm" default=currentTime/>
                    ${uiLabelMap.CommonThru}
                    <@inputDateTime name="thruDate" form="createAgreementForm" default="23:59:59"/>
                </td>
                <td cospan="2">&nbsp;</td>
            </tr>
            <tr>
                <@displayTitleCell title="${uiLabelMap.CommonDescription}"/>
                <td colspan="2"><input name="description" type="text" maxlength="255" style="width:100%;" class="inputBox" value=""></td>
                <td>&nbsp;</td>
            </tr>
            <tr valign="top">
                <@displayTitleCell title="${uiLabelMap.OpentapsTextData}"/>
                <td colspan="2"><textarea rows="10" name="textData" class="inputBox" style="width: 100%;"></textarea></td>
                <td>&nbsp;</td>
            </tr>
            <tr>
                <td></td>
                <@inputSubmitCell title="${uiLabelMap.CommonCreate}"/>
                <td colspan="2">&nbsp;</td>
            </tr>
        </table>
        </form>
    </div>
</div>

</#if>
