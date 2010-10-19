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

<script type="text/javascript">
function onPartyOrGroupChanged() {
    var PrtyGrp = document.getElementById('divPartyGrpTo');
    var PrtyTo = document.getElementById('divPartyIdTo');
    if (PrtyGrp && PrtyTo) {
        if ('partyIdTo' == document.createAgreementForm.selector.value) {
            document.createAgreementForm.toPartyClassGroupId.value = '';
            opentaps.addClass(PrtyGrp, 'hidden');
            opentaps.removeClass(PrtyTo, 'hidden');
        } else if ('toPartyClassGroupId' == document.createAgreementForm.selector.value) {
            document.createAgreementForm.partyIdTo.value = '';
            opentaps.addClass(PrtyTo, 'hidden');
            opentaps.removeClass(PrtyGrp, 'hidden');
        }
    }
}
</script>

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#assign currentTime = Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()/>

<div class="subSectionBlock">
    <div class="form">
        <#assign targetAction = createAgreementAction?default("processCreateAgreement")/>
        <form method="post" action="<@ofbizUrl>${targetAction}</@ofbizUrl>" class="basic-form" onSubmit="javascript:submitFormDisableSubmits(this)" name="createAgreementForm">
        <#if agreementTypeId?has_content><@inputHidden name="agreementTypeId" value=agreementTypeId/></#if>
        <#if partyIdTo?has_content><@inputHidden name="partyIdTo" value=partyIdTo/></#if>
        <#if partyIdFrom?has_content><@inputHidden name="partyIdFrom" value=partyIdFrom/></#if>
        <#if roleTypeIdFrom?has_content><@inputHidden name="roleTypeIdFrom" value=roleTypeIdFrom/></#if>
        <#if roleTypeIdTo?has_content><@inputHidden name="roleTypeIdTo" value=roleTypeIdTo/></#if>
        <table width="100%">
            <tr>
                <@displayCell text="${uiLabelMap.AccountingAgreementDate}" blockClass="titleCell" class="tableheadtext"/>
                <@inputDateTimeCell name="agreementDate" form="createAgreementForm" default=currentTime/>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr>
                <#if !partyIdTo?has_content>
                    <td class="titleCell">
                    <span class="tableheadtext">
                        <select name="selector" class="selectBox" onchange="onPartyOrGroupChanged();">
                            <option value="partyIdTo" selected>${uiLabelMap.AccountingToParty}</option>
                            <option value="toPartyClassGroupId">${uiLabelMap.FinancialsToClsGroup}</option>
                        </select>
                    </span>
                    </td>
                    <td>
                        <div id="divPartyIdTo">
                            <@inputLookup name="partyIdTo" lookup="LookupPartyName" form="createAgreementForm" size=20/>
                        </div>
                        <div id="divPartyGrpTo" class="hidden">
                            <@inputSelect name="toPartyClassGroupId" list=partyClsGroupList?default([]) required=false key="partyClassificationGroupId"; option>
                                ${option.get("description", locale)}
                            </@inputSelect>
                        </div>
                    </td>
                    <td colspan="2">&nbsp;</td>
                <#else>
                    <td colspan="4">&nbsp;</td>
                </#if>
            </tr>
            <tr>
                <@displayCell text="${uiLabelMap.CommonFromDate}" blockClass="titleCell" class="tableheadtext"/>
                <td>              
                    <@inputDateTime name="fromDate" form="createAgreementForm" default=currentTime/>
                    ${uiLabelMap.CommonThru}
                    <@inputDateTime name="thruDate" form="createAgreementForm" default="23:59:59"/>
                </td>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr>
                <@displayCell text="${uiLabelMap.CommonDescription}" blockClass="titleCell" class="tableheadtext"/>
                <td colspan="2"><input name="description" type="text" maxlength="255" style="width:100%;" class="inputBox" value=""></td>
                <td>&nbsp;</td>
            </tr>
            <tr valign="top">
                <@displayCell text="${uiLabelMap.OpentapsTextData}" blockClass="titleCell" class="tableheadtext"/>
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

<script type="text/javascript">
opentaps.addOnLoad(onPartyOrGroupChanged());
</script>

</#if>