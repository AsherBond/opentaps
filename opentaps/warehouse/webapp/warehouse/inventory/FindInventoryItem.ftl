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

<form method="post" action="<@ofbizUrl>findInventoryItem</@ofbizUrl>" name="findInventoryItem">
  <table>
    <@inputLookupRow title=uiLabelMap.ProductLocation name="locationSeqId" lookup="LookupFacilityLocation" form="findInventoryItem" />
    <@inputAutoCompleteProductRow name="productId" title=uiLabelMap.ProductProductId />
    <@inputTextRow name="internalName" title=uiLabelMap.ProductInternalName />
    <@inputTextRow name="serialNumber" title=uiLabelMap.ProductSerialNumber />
    <@inputLookupRow title=uiLabelMap.ProductLotId name="lotId" lookup="LookupLot" form="findInventoryItem" />
    <@inputCheckboxRow title="Show Accounting Tags" name="showAcctgTags" />
    <#list organizations as org>
      <@displayRow title=uiLabelMap.Organization text=org.name />
      <#assign tagFilters = tagFiltersPerOrg.get(org.partyId) />
      <#list tagFilters as tag>
        <@inputSelectRow title=tag.description name="${org.partyId}Tag${tag.index}" list=tag.activeTagValues key="enumId" required=true ; tagValue>
          ${tagValue.description}
        </@inputSelectRow>
      </#list>
    </#list>
    <#if organizations?size gt 1>
      <td/>
      <td><i>When at least one tag filter is selected, the inventory belonging to organizations with all tags set to Any will be ignored in the results.</i></td>
    </#if>
    <@inputHidden name="performFind" value="Y" />
    <@inputSubmitRow title=uiLabelMap.WarehouseFindInventoryItem />
  </table>
</form>
