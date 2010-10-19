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

<form name="findInventory" method="post" action="<@ofbizUrl>ViewFacilityInventoryByProduct</@ofbizUrl>" id="findInventory">
  <@inputHidden name="action" value="SEARCH" />
  <table class="twoColumnForm">
    <tr>
      <@displayTitleCell title=uiLabelMap.FacilityFacility titleClass="requiredField" />
      <@inputSelectCell  name="facilityId" list=facilities required=true ; facility>
        ${facility.facilityName?if_exists} (${facility.facilityId?if_exists})
      </@inputSelectCell>
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.ProductProduct />
      <@inputLookupCell name="productId" form="findInventory" lookup="LookupProduct" default=productId />
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.ProductInternalName />
      <@inputTextCell name="internalName" size=20 default=internalName />
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.ProductProductType />
      <@inputSelectCell  name="productTypeId" list=productTypes displayField="description" default="FINISHED_GOOD"/>
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.ProductCategory />
      <@inputLookupCell name="searchInProductCategoryId" form="findInventory" lookup="LookupProductCategory" default=searchInProductCategoryId />
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.ProductSupplier />
      <@inputSelectCell  name="partyId" list=partyRoleAndPartyDetails ; party>
        ${party.groupName?if_exists} <#if party.partyId?exists && party.partyId?length &gt; 0>[${party.partyId}]</#if>
      </@inputSelectCell>
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.ProductQtyOffsetQOHBelow />
      <@inputTextCell name="offsetQOHQty" size=20 default=offsetQOHQty />
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.ProductQtyOffsetATPBelow />
      <@inputTextCell name="offsetATPQty" size=20 default=offsetATPQty />
    </tr>
    <tr>
      <@inputDateRow name="productsSoldThruTimestamp" title=uiLabelMap.ProductShowProductsSoldThruTimestamp form="findInventory" default=Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp() />
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.ProductShowProductsPerPage />
      <@inputTextCell name="VIEW_SIZE" size=20 default=50 />
    </tr>
    <@inputSubmitRow title=uiLabelMap.CommonFind />
  </table>
</form>
