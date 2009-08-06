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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl" />

<div class="subSectionBlock">

    <div class="subSectionHeader">
        <div class="subSectionTitle">${uiLabelMap.OpentapsTaxIds}</div>
    </div>

    <table class="listTable">
        <tr class="listTableHeader">
            <td>${uiLabelMap.CommonGeo}</td>
            <td>${uiLabelMap.AccountingTaxAuthority}</td>
            <td>${uiLabelMap.PartyTaxId}</td>
            <td>${uiLabelMap.CommonFrom}</td>
            <td>${uiLabelMap.CommonThru}</td>
            <td></td>
            <td></td>
        </tr>
        <#list supplierTaxAuthorities as taxAuthority>
            <form method="post" action="<@ofbizUrl>updateSupplierTaxAuthInfo</@ofbizUrl>" name="updateSupplierTaxAuthInfo_${taxAuthority_index}">
                <input type="hidden" name="nowDate" value="${nowDate}"/>
                <input type="hidden" name="partyId" value="${parameters.partyId}"/>
                <input type="hidden" name="taxAuthPartyId" value="${taxAuthority.taxAuthPartyId}"/>
                <input type="hidden" name="taxAuthGeoId" value="${taxAuthority.taxAuthGeoId}"/>
                <input type="hidden" name="fromDate" value="${taxAuthority.fromDate}"/>

            <tr class="${tableRowClass(taxAuthority_index)}">
                <@displayCell text=taxAuthority.abbreviation />
                <@displayCell text="${taxAuthority.groupName?if_exists} (${taxAuthority.taxAuthPartyId})" />
                <@inputTextCell name="partyTaxId" default=taxAuthority.partyTaxId size=15 />
                <@displayDateCell date=taxAuthority.fromDate />
                <@inputDateTimeCell name="thruDate" default=taxAuthority.thruDate?if_exists/>
                <@inputSubmitCell title=uiLabelMap.CommonUpdate />
                <@inputSubmitCell title=uiLabelMap.CommonExpire onClick="javascript:{this.form.thruDate.value=this.form.nowDate.value; this.form.submit();}" />
            </tr>
            </form>
        </#list>
    </table>

    <#if hasUpdatePermission?default(false)>
    <form method="post" action="<@ofbizUrl>createSupplierTaxAuthInfo</@ofbizUrl>">
        <input type="hidden" name="isExempt" value="N"/>
        <input type="hidden" name="isNexus" value="N"/>
        <input type="hidden" name="partyId" value="${parameters.partyId}"/>
        <table>
            <tr>
                <td>
                    <span class="tableheadtext">&nbsp;${uiLabelMap.AccountingTaxAuthority}: </span>
                    <@inputSelectTaxAuthority list=taxAuthorities required=true/>
                </td>
                <td>
                    <span class="tableheadtext">&nbsp;${uiLabelMap.PartyTaxId}: </span>
                    <@inputText name="partyTaxId" size=15 />
                </td>
                <@inputSubmitCell title=uiLabelMap.CommonAdd />
            </tr>
        </table>
    </form>
    </#if>

</div>
