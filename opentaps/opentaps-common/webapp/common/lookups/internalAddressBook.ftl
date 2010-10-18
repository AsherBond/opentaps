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
