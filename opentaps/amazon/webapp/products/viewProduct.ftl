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
    <@sectionHeader title="${uiLabelMap.ProductProduct} ${uiLabelMap.OrderNbr}${amazonProduct.productId}">
        <div class="subMenuBar">
            <@displayLink href="updateProduct?productId=${amazonProduct.productId}" text="${uiLabelMap.CommonEdit}" class="subMenuButton"/>
        </div>
    </@sectionHeader>
    <div class="form">
        <table width="100%">
            <@displayRow title="${uiLabelMap.AmazonProductListID}" text=amazonProduct.productId/>
            <#list productIds?default([]) as productId>
                <@displayRow title=productId.goodIdentificationTypeId text=productId.idValue/>
            </#list>
            <@displayRow title="${uiLabelMap.AmazonOrderTitle}" text=amazonProduct.internalName/>
            <#if status?has_content>
                <@displayRow title="${uiLabelMap.AmazonOrderStatus}" text=status.get("description", locale)/>
            </#if>
            <@displayRow title="${uiLabelMap.AmazonProductListLaunchDate}" text=amazonProduct.introductionDate/>
            <@displayRow title="${uiLabelMap.AmazonProductListDiscontinueDate}" text=amazonProduct.salesDiscontinuationDate/>
            <@displayRow title="${uiLabelMap.AmazonProductListReleaseDate}" text=amazonProduct.releaseDate/>
            <@displayRow title="${uiLabelMap.AmazonProductTaxCode}" text=amazonProduct.productTaxCode/>
            <#if node?has_content>
                <@displayRow title="${uiLabelMap.AmazonProductBrowseNode}" text=node.get("description", locale)/>
            </#if>
            <@displayRow title="${uiLabelMap.AmazonProductItemType}" text=amazonProduct.itemTypeId/>
            <@displayRow title="${uiLabelMap.CommonPriority}" text=amazonProduct.priority/>
            <@displayRow title="${uiLabelMap.AmazonProductBrowseExclusion}" text=amazonProduct.browseExclusion/>
            <@displayRow title="${uiLabelMap.AmazonProductRecommendationExclusion}" text=amazonProduct.recommendationExclusion/>
            <@displayRow title="${uiLabelMap.AmazonProductCost}" text=amazonProduct.cost/>
            <@displayRow title="${uiLabelMap.Currency}" text=amazonProduct.currencyUom/>
            <@displayRow title="${uiLabelMap.AmazonProductTier}" text=amazonProduct.tier/>
            <@displayRow title="${uiLabelMap.AmazonProductPurchasingCategory}" text=amazonProduct.purchasingCategory/>
            <@displayRow title="${uiLabelMap.AmazonProductPurchasingSubCategory}" text=amazonProduct.purchasingSubCategory/>
            <@displayRow title="${uiLabelMap.AmazonProductPackagingType}" text=amazonProduct.packagingType/>
            <@displayRow title="${uiLabelMap.AmazonProductUnderlAvailability}" text=amazonProduct.underlyingAvailability/>
            <@displayRow title="${uiLabelMap.AmazonProductReplenishmentCategory}" text=amazonProduct.replenishmentCategory/>
            <@displayRow title="${uiLabelMap.AmazonProductDropShipStatus}" text=amazonProduct.dropShipStatus/>
            <@displayRow title="${uiLabelMap.AmazonProductOoSMessage}" text=amazonProduct.outOfStockWebsiteMessage/>
            <@displayRow title="${uiLabelMap.AmazonProductRegisteredParameter}" text=amazonProduct.registeredParameter/>
        </table>
    </div>
</div>