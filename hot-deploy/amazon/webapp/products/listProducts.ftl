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

<@paginate name="amazonProducts" list=amazonProducts>
  <#noparse>
    <@navigationHeader/>
        <table class="listTable" style="max-width:100%">
            <tr class="listTableHeader">
                <@displayCell text=uiLabelMap.AmazonProductListID blockStyle="max-width:75px"/>
                <@displayCell text=uiLabelMap.AmazonProductListTitle/>
                <@displayCell text=uiLabelMap.AmazonProductListLaunchDate blockClass="fieldDateTime"/>
                <@displayCell text=uiLabelMap.AmazonProductListDiscontinueDate blockClass="fieldDateTime"/>
                <@displayCell text=uiLabelMap.AmazonProductListReleaseDate blockClass="fieldDateTime"/>
            </tr>
            <#list pageRows as product>
            <tr class="${tableRowClass(product_index)}">
                <@displayCell text=product.productId/>
                <@displayLinkCell text=product.internalName href="viewProduct?productId=${product.productId}"/>
                <@displayCell text=product.introductionDate/>
                <@displayCell text=product.salesDiscontinuationDate/>
                <@displayCell text=product.releaseDate/>
            </tr>
            </#list>
        </table>
  </#noparse>
</@paginate>

