<#--
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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

<div class="subSectionBlock">
    <form method="get" target="" name="findSuppliersForm">
        <@inputHidden name="performFind" value="Y"/>
        <table class="twoColumnForm">
            <@inputTextRow name="groupName" title=uiLabelMap.OpentapsSupplierName />
            <@inputSubmitRow title=uiLabelMap.CommonFind />
        </table>
    </form>
</div>

<@sectionHeader title="${uiLabelMap.OpentapsSupplierName}"/>
<div class="subSectionBlock">
    <@paginate name="suppliersList" list=suppliers rememberPage="false" sectionName=sectionName>
    <#noparse>
    <@navigationBar/>
    <table class="listTable">
        <tr class="listTableHeader">
            <@headerCell title=uiLabelMap.OpentapsSupplierName orderBy="groupName,firstName,lastName"/>
            <@headerCell title=uiLabelMap.CommonCity orderBy="primaryCity"/>
            <@headerCell title=uiLabelMap.CrmPrimaryEmail orderBy="primaryEmail"/>
            <@headerCell title=uiLabelMap.PartyPhoneNumber orderBy=""/>
        <tr>
        <#list pageRows as supplier>
        <tr class="${tableRowClass(supplier_index)}">
            <#if parameters.sectionName?default("") == "lookup">
                <@displayLinkCell href="javascript:set_value('${supplier.partyId}')" text="${supplier.compositeName?if_exists} (${supplier.partyId})" blockClass="fieldWidth25pct"/>
            <#else>
                <@displayLinkCell href="viewSupplier?partyId=${supplier.partyId}" text="${supplier.compositeName?if_exists} (${supplier.partyId})" blockClass="fieldWidth25pct"/>
            </#if>
            <@displayCell text=supplier.abbrevPostalAddressByPurpose?if_exists blockClass="fieldWidth25pct"/>
            <@displayCell text=supplier.electronicAddressByPurpose?if_exists blockClass="fieldWidth25pct"/>
            <@displayCell text=supplier.telecomNumberByPurpose?if_exists blockClass="fieldWidth25pct"/>
        </tr>
        </#list>
    </table>
    </#noparse>
    </@paginate>
</div>
