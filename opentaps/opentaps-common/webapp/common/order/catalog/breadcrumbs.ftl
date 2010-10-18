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


<#-- variable setup and worker calls -->
<#assign topLevelList = requestAttributes.topLevelList?if_exists>
<#assign curCategoryId = requestAttributes.curCategoryId?if_exists>

<#-- looping macro -->
<#macro categoryList parentCategory category>
  <#if parentCategory.productCategoryId != category.productCategoryId>
    <#assign categoryUrl><@ofbizUrl>category/~category_id=${category.productCategoryId}/~pcategory=${parentCategory.productCategoryId}</@ofbizUrl></#assign>
  <#else>
    <#assign categoryUrl><@ofbizUrl>category/~category_id=${category.productCategoryId}</@ofbizUrl></#assign>
  </#if>

  <#if (Static["org.ofbiz.product.category.CategoryWorker"].checkTrailItem(request, category.getString("productCategoryId"))) || (curCategoryId?exists && curCategoryId == category.productCategoryId)>
    <li>
    <#if catContentWrappers?exists && catContentWrappers[category.productCategoryId]?exists>
      <a href="${categoryUrl}" <#if curCategoryId?exists && curCategoryId == category.productCategoryId> class="buttontextdisabled"<#else> class="buttontext"</#if>>
        <#if catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")?exists>
          ${catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")}
        <#elseif catContentWrappers[category.productCategoryId].get("DESCRIPTION")?exists>
          ${catContentWrappers[category.productCategoryId].get("DESCRIPTION")}
        <#else>
          ${category.description?if_exists}
        </#if>
      </a>
    </#if>
    </li>
    <#local subCatList = Static["org.ofbiz.product.category.CategoryWorker"].getRelatedCategoriesRet(request, "subCatList", category.getString("productCategoryId"), true)>
    <#if subCatList?exists>
      <#list subCatList as subCat>
        <@categoryList parentCategory=category category=subCat/>
      </#list>
    </#if>
  </#if>
</#macro>

<div class="breadcrumbs">
  <ul>
    <#-- Show the category branch -->
    <#list topLevelList as category>
      <@categoryList parentCategory=category category=category/>
    </#list>
    <#-- Show the product, if there is one -->
    <#if productContentWrapper?exists>
    <li><span class="current">${productContentWrapper.get("PRODUCT_NAME")?if_exists}</span></li>
    </#if>
  </ul>
</div>
