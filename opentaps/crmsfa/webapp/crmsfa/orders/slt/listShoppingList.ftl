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

<#if parameters.performFind?exists && parameters.performFind == "Y" && hasViewSltPermission>

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl" />

<div class="subSectionBlock">
<@sectionHeader title=uiLabelMap.CrmShoppingLists/>
<@paginate name="shoppingList" list=shoppingListBuilder>
<#noparse>
    <@navigationBar/>
    <table class="listTable">
        <tr class="listTableHeader">
            <@headerCell title=uiLabelMap.CommonId orderBy="shoppingListId"/>
            <@headerCell title=uiLabelMap.CommonType orderBy=""/>
            <@headerCell title=uiLabelMap.CommonName orderBy="description"/>
            <@headerCell title=uiLabelMap.CommonCreated orderBy="createdStamp" blockClass="fieldDateTime"/>
        </tr>
        <#list pageRows as row>
            <tr class="${tableRowClass(row_index)}">
              <@displayLinkCell href="viewShoppingList?shoppingListId=${row.shoppingListId}" text=row.shoppingListId/>
              <@displayCell text=row.getRelatedOneCache("ShoppingListType").description/>
              <@displayCell text=row.listName/>
              <@displayDateCell date=row.createdStamp/>
            </tr>
        </#list>
    </table>
</#noparse>
</@paginate>

</div>
</#if>