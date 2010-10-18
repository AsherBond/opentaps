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

<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<#-- This file has been modified by Open Source Strategies, Inc. -->

<#-- This file provides catalog browsing and searching during order entry -->

<#-- Only show if there is more than 1 (one) catalog, no sense selecting when there is only one option... -->
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#assign advancedButton>
<a href="javascript:document.advancedsearchform.submit()" class="buttontext">${uiLabelMap.ProductAdvancedSearch}</a>
</#assign>

<@frameSection title=uiLabelMap.OpentapsBrowseAndSearch extra=advancedButton>
  <form name="keywordsearchform" method="post" action="<@ofbizUrl>keywordsearch</@ofbizUrl>">
    <input type="hidden" name="VIEW_SIZE" value="10"/>
    <div class="tabletext">
      <input type="text" class="inputBox" name="SEARCH_STRING" size="22" maxlength="50" value="${requestParameters.SEARCH_STRING?if_exists}"/>
      <#if 0 lt otherSearchProdCatalogCategories?size>
        <div class="tabletext">
          <select name="SEARCH_CATEGORY_ID" size="1" class="selectBox">
            <option value="${searchCategoryId?if_exists}">${uiLabelMap.ProductEntireCatalog}</option>
            <#list otherSearchProdCatalogCategories as otherSearchProdCatalogCategory>
              <#assign searchProductCategory = otherSearchProdCatalogCategory.getRelatedOneCache("ProductCategory")/>
              <#if searchProductCategory?exists>
                <option value="${searchProductCategory.productCategoryId}">${searchProductCategory.description?default("No Description " + searchProductCategory.productCategoryId)}</option>
              </#if>
            </#list>
          </select>
        </div>
      <#else/>
        <input type="hidden" name="SEARCH_CATEGORY_ID" value="${searchCategoryId?if_exists}"/>
      </#if>
      <input type="hidden" name="SEARCH_OPERATOR" value="AND">
      <a href="javascript:document.keywordsearchform.submit()" class="buttontext">${uiLabelMap.CommonSearch}</a>
    </div>
  </form>

  <form name="advancedsearchform" method="post" action="<@ofbizUrl>advancedsearch</@ofbizUrl>">
    <#if 0 lt otherSearchProdCatalogCategories?size>
      <div class="tabletext">${uiLabelMap.ProductAdvancedSearchIn}: </div>
      <div class="tabletext">
        <select name="SEARCH_CATEGORY_ID" size="1" class="selectBox">
          <option value="${searchCategoryId?if_exists}">${uiLabelMap.ProductEntireCatalog}</option>
          <#list otherSearchProdCatalogCategories as otherSearchProdCatalogCategory>
            <#assign searchProductCategory = otherSearchProdCatalogCategory.getRelatedOneCache("ProductCategory")/>
            <#if searchProductCategory?exists>
              <option value="${searchProductCategory.productCategoryId}">${searchProductCategory.description?default("No Description " + searchProductCategory.productCategoryId)}</option>
            </#if>
          </#list>
        </select>
      </div>
    <#else/>
      <input type="hidden" name="SEARCH_CATEGORY_ID" value="${searchCategoryId?if_exists}"/>
    </#if>
  </form>

  <hr class="sepbar"/>

   <#-- the catalog selection box -->

  <#if (catalogCol?size gt 1)>
    <div class="screenlet-body" style="text-align: left;">
      <form name="choosecatalogform" method="post" action="<@ofbizUrl>choosecatalog</@ofbizUrl>" style="margin: 0;">
        <select name="CURRENT_CATALOG_ID" class="selectBox">
          <option value="${currentCatalogId}">${currentCatalogName}</option>
          <option value="${currentCatalogId}"></option>
          <#list catalogCol as catalogId>
            <#assign thisCatalogName = Static["org.ofbiz.product.catalog.CatalogWorker"].getCatalogName(request, catalogId)/>
            <option value="${catalogId}">${thisCatalogName}</option>
          </#list>
        </select>
        <a href="javascript:document.choosecatalogform.submit()" class="buttontext">${uiLabelMap.CrmOrderChangeCatalog}</a>
      </form>
    </div>
  </#if>

  <#-- the categories dropdown box -->
  <#if (requestAttributes.topLevelList)?exists><#assign topLevelList = requestAttributes.topLevelList></#if>
  <#if (requestAttributes.curCategoryId)?exists><#assign curCategoryId = requestAttributes.curCategoryId></#if>

<#-- looping macro -->
<#macro categoryList parentCategory category>
  <#if parentCategory.productCategoryId != category.productCategoryId>
    <#local pStr = "/~pcategory=" + parentCategory.productCategoryId>
  </#if>
  <#if curCategoryId?exists && curCategoryId == category.productCategoryId>
    <div class="browsecategorytext">
     <#if catContentWrappers?exists && catContentWrappers[category.productCategoryId]?exists && catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")?has_content>
       <#if sessionAttributes.shoppingCart?exists && sessionAttributes.shoppingCart.isPurchaseOrder()>
         -&nbsp;<a href="<@ofbizUrl>keywordsearch/~SEARCH_CATEGORY_ID=${category.productCategoryId}/~SEARCH_SUPPLIER_ID=${sessionAttributes.shoppingCart.partyId?if_exists}/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybuttondisabled">${catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")}</a>
       <#else>
         -&nbsp;<a href="<@ofbizUrl>category/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybuttondisabled">${catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")}</a>
       </#if>
     <#elseif catContentWrappers?exists && catContentWrappers[category.productCategoryId]?exists && catContentWrappers[category.productCategoryId].get("DESCRIPTION")?has_content>
       <#if sessionAttributes.shoppingCart?exists && sessionAttributes.shoppingCart.isPurchaseOrder()>
         -&nbsp;<a href="<@ofbizUrl>keywordsearch/~SEARCH_CATEGORY_ID=${category.productCategoryId}/~SEARCH_SUPPLIER_ID=${sessionAttributes.shoppingCart.partyId?if_exists}/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybuttondisabled">${catContentWrappers[category.productCategoryId].get("DESCRIPTION")}</a>
       <#else>
         -&nbsp;<a href="<@ofbizUrl>category/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybuttondisabled">${catContentWrappers[category.productCategoryId].get("DESCRIPTION")}</a>
       </#if>
     <#else>
      <#if sessionAttributes.shoppingCart?exists && sessionAttributes.shoppingCart.isPurchaseOrder()>
       -&nbsp;<a href="<@ofbizUrl>keywordsearch/~SEARCH_CATEGORY_ID=${category.productCategoryId}/~SEARCH_SUPPLIER_ID=${sessionAttributes.shoppingCart.partyId?if_exists}/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybuttondisabled">${category.categoryName?if_exists}</a>
      <#else>
       -&nbsp;<a href="<@ofbizUrl>category/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybuttondisabled">${category.categoryName?default(category.description)?default(category.productCategoryId)}</a>
      </#if>
     </#if>
    </div>
  <#else>
    <div class="browsecategorytext">
     <#if catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")?has_content>
      <#if sessionAttributes.shoppingCart?exists && sessionAttributes.shoppingCart.isPurchaseOrder()>
       -&nbsp;<a href="<@ofbizUrl>keywordsearch/~SEARCH_CATEGORY_ID=${category.productCategoryId}/~SEARCH_SUPPLIER_ID=${sessionAttributes.shoppingCart.partyId?if_exists}/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybutton">${catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")}</a>
      <#else>
        -&nbsp;<a href="<@ofbizUrl>category/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybutton">${catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")}</a>
      </#if>
     <#elseif catContentWrappers[category.productCategoryId].get("DESCRIPTION")?has_content>
      <#if sessionAttributes.shoppingCart?exists && sessionAttributes.shoppingCart.isPurchaseOrder()>
       -&nbsp;<a href="<@ofbizUrl>keywordsearch/~SEARCH_CATEGORY_ID=${category.productCategoryId}/~SEARCH_SUPPLIER_ID=${sessionAttributes.shoppingCart.partyId?if_exists}/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybutton">${catContentWrappers[category.productCategoryId].get("DESCRIPTION")}</a>
      <#else>
        -&nbsp;<a href="<@ofbizUrl>category/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybutton">${catContentWrappers[category.productCategoryId].get("DESCRIPTION")}</a>
      </#if>
     <#else>
      <#if sessionAttributes.shoppingCart?exists && sessionAttributes.shoppingCart.isPurchaseOrder()>
       -&nbsp;<a href="<@ofbizUrl>keywordsearch/~SEARCH_CATEGORY_ID=${category.productCategoryId}/~SEARCH_SUPPLIER_ID=${sessionAttributes.shoppingCart.partyId?if_exists}/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybutton">${category.categoryName?if_exists}</a>
      <#else>
       -&nbsp;<a href="<@ofbizUrl>category/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybutton">${category.categoryName?default(category.description)?default(category.productCategoryId)}</a>
      </#if>
     </#if>
    </div>
  </#if>

  <#if (Static["org.ofbiz.product.category.CategoryWorker"].checkTrailItem(request, category.getString("productCategoryId"))) || (curCategoryId?exists && curCategoryId == category.productCategoryId)>
    <#local subCatList = Static["org.ofbiz.product.category.CategoryWorker"].getRelatedCategoriesRet(request, "subCatList", category.getString("productCategoryId"), true)>
    <#if subCatList?exists>
      <#list subCatList as subCat>
        <div style="margin-left: 10px">
          <@categoryList parentCategory=category category=subCat/>
        </div>
      </#list>
    </#if>
  </#if>
</#macro>

  <#if topLevelList?has_content>
    <div style="margin-left: 10px;">
      <#list topLevelList as category>
        <@categoryList parentCategory=category category=category/>
      </#list>
    </div>
  </#if>

</@frameSection>
