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

<#if amazonProduct?has_content>
  <#assign create = false/>
<#else>
  <#assign amazonProduct = {}/>
  <#assign create = true/>
</#if>
<div class="subSectionBlock">
    <div class="form">
        <form method="post" action="<@ofbizUrl>createOrUpdateAmazonProduct</@ofbizUrl>" class="basic-form" onSubmit="javascript:submitFormDisableSubmits(this)" name="updateProductForm">
            <table width="100%"/>
                <#if create>
                  <@inputTextRow title="${uiLabelMap.ProductProductId}" name="productId" size="20" maxlength="20"/>
                <#else>
                  <@inputHidden name="productId" value=amazonProduct.productId/>
                  <@displayRow title="${uiLabelMap.ProductProductId}" text=amazonProduct.productId/>
                </#if>
                ${screens.render("component://amazon/widget/products/AmazonProductScreens.xml#amazonProductFields")}
                <#if create>
                  <@inputSubmitRow title="${uiLabelMap.CommonCreate}"/>        
                <#else>
                  <@inputSubmitRow title="${uiLabelMap.CommonUpdate}"/>        
                </#if>
            </table>
        </form>
    </div>
</div>