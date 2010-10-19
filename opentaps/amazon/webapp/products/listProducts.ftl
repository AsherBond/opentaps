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

