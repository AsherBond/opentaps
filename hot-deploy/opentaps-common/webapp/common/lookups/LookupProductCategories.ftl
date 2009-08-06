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
