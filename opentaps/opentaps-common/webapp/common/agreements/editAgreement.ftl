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

<#if hasUpdateAgreementPermission?default(false) == true>

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div class="subSectionBlock">
    <@sectionHeader title="${agreementTypeDescription?if_exists} ${uiLabelMap.OrderNbr}${agreementHeader.agreementId?if_exists}"/>
    <div class="form">
        <#assign targetAction = updateAgreementAction?default("processUpdateAgreement")/>
        <form method="post" action="${targetAction}" class="basic-form" onSubmit="javascript:submitFormDisableSubmits(this)" name="editAgreementForm">
        <@inputHidden name="agreementId" value=agreementHeader?if_exists.agreementId?if_exists/>
        <table width="100%">
            <tr>
                <@displayCell text="${uiLabelMap.CommonType}" blockClass="titleCell" class="tableheadtext"/>
                <#if agreementType?has_content>
                    <@displayCell text=agreementType.get("description", locale)/><#else><td>&nbsp;</td></#if>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr>
                <@displayCell text="${uiLabelMap.CommonStatus}" blockClass="titleCell" class="tableheadtext"/>
                <td>
                    <select name="statusId" class="inputBox"/>
                    <#if status?has_content>
                        <option selected value="${agreementHeader.statusId}">${status.get("description")}</option>
                    </#if>
                    <#if statusItems?has_content>
                        <option value="${agreementHeader.statusId}">---</option>
                        <#list statusItems as statusItem>
                            <option value="${statusItem.statusIdTo}">${statusItem.get("description")}</option>
                        </#list>
                    </#if>
                </td>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr>
                <@displayCell text="${uiLabelMap.AccountingFromParty}" blockClass="titleCell" class="tableheadtext"/>
                <@displayCell text="${partyNameFrom?if_exists} (${agreementHeader.partyIdFrom?if_exists})"/>
                <#if agreementHeader.partyIdTo?has_content>
                    <@displayCell text="${uiLabelMap.AccountingToParty}" blockClass="titleCell" class="tableheadtext"/>
                    <@displayCell text="${partyNameTo?if_exists} (${agreementHeader.partyIdTo?if_exists})"/>
                <#elseif agreementHeader.toPartyClassGroupId?has_content>
                    <@displayCell text="${uiLabelMap.CommonTo}&nbsp;${uiLabelMap.PartyClassificationGroup}" blockClass="titleCell" class="tableheadtext"/>
                    <#if partyClsGroup?has_content>
                        <@displayCell text=partyClsGroup.get("description", locale)/>
                    </#if>
                </#if>
            </tr>
            <tr>
                <@displayCell text="${uiLabelMap.CommonFromDate}" blockClass="titleCell" class="tableheadtext"/>
                <@inputDateTimeCell name="fromDate" form="editAgreementForm" default=agreementHeader.fromDate?if_exists/>
                <@displayCell text="${uiLabelMap.CommonThruDate}" blockClass="titleCell" class="tableheadtext"/>
                <@inputDateTimeCell name="thruDate" form="editAgreementForm" default=agreementHeader.thruDate?if_exists/>
            </tr>
            <tr>
                <@displayCell text="${uiLabelMap.CommonDescription}" blockClass="titleCell" class="tableheadtext"/>
                <td colspan="3"><input name="description" type="text" maxlength="255" style="width:100%;" class="inputBox" value="${agreementHeader.description?if_exists}"></td>
            </tr>
            <tr valign="top">
                <@displayCell text="${uiLabelMap.OpentapsTextData}" blockClass="titleCell" class="tableheadtext"/>
                <td colspan="3"><textarea rows="10" name="textData" class="inputBox" style="width:100%;">${agreementHeader.textData?if_exists}</textarea></td>
            </tr>
            <tr>
                <td></td>
                <@inputSubmitCell title="${uiLabelMap.CommonUpdate}"/>
                <td colspan="2">&nbsp;</td>
            </tr>
        </table>
        </form>
    </div>
</div>

</#if>
