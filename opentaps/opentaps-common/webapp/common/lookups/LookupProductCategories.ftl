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
    <@sectionHeader title="${uiLabelMap.OpentapsFindProductCategory}"/>
</div>

<div class="subSectionBlock">
    <form method="post" action="<@ofbizUrl>LookupProductCategory</@ofbizUrl>" name="LookupProductCategoriesForm">
        <@inputHidden name="performFind" value="Y"/>
        <table class="twoColumnForm">
            <@inputTextRow name="categoryPattern" title="${uiLabelMap.CommonName}"/>
            <@inputSubmitRow title=uiLabelMap.CommonFind />
        </table>
    </form>
</div>

<div class="subSectionBlock">

    <@sectionHeader title="${uiLabelMap.Categories}"/>
    <@paginate name="FindProductCategory" list=listProductCategories rememberPage=false>
    <#noparse>
    <@navigationBar />
    <table class="listTable">

        <tr class="listTableHeader">
            <@headerCell title="${uiLabelMap.ProductCategoryId}" orderBy="productCategoryId" blockClass="tableheadtext"/>
            <@headerCell title="${uiLabelMap.ProductCategory}" orderBy="" blockClass="tableheadtext"/>
        </tr>

        <#list pageRows as category>
        <tr class="${tableRowClass(category_index)}">
            <@displayLinkCell href="javascript:set_value('${category.productCategoryId}')" text=category.productCategoryId blockClass="fieldWidth300"/>
            <@displayCell text=category.category/>
        </tr>
        </#list>

    </table>
    </#noparse>
    </@paginate>
</div>
