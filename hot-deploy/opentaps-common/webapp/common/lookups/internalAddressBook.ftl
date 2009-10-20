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
    <@sectionHeader title="${uiLabelMap.OpentapsAddressBook}"/>
</div>

<div class="subSectionBlock">
    <form method="post" action="<@ofbizUrl>LookupInternalAddressBook</@ofbizUrl>" name="LookupAddressBookForm">
        <@inputHidden name="performFind" value="Y"/>
        <table class="twoColumnForm">
            <@inputTextRow name="namePattern" title="${uiLabelMap.CommonName}"/>
            <@inputSubmitRow title=uiLabelMap.CommonFind />
        </table>
    </form>
</div>

<div class="subSectionBlock">

    <@sectionHeader title="${uiLabelMap.Categories}"/>
    <@paginate name="FindProductCategory" list=addressBook rememberPage=false>
    <#noparse>
    <@navigationBar />
    <table class="listTable">

        <tr class="listTableHeader">
            <@headerCell title="${uiLabelMap.ProductCategoryId}" orderBy="partyId" blockClass="tableheadtext"/>
            <@headerCell title="${uiLabelMap.ProductCategory}" orderBy="partyName" blockClass="tableheadtext"/>
        </tr>

        <#list pageRows as addressee>
        <tr class="${tableRowClass(addressee_index)}">
            <@displayLinkCell href="javascript:add_value('${addressee.partyAddress}')" text=addressee.partyId blockClass="fieldWidth300"/>
            <@displayCell text=addressee.partyAddress/>
        </tr>
        </#list>

    </table>
    </#noparse>
    </@paginate>
</div>
