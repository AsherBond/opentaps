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