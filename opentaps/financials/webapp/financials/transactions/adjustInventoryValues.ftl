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

<script type="text/javascript">
/*<![CDATA[*/
  /**
   * Check which unit costs changed and set their row to be submitted.
   * Also perform input validation: unitCost should be a valid positive float number.
   * If not unit cost is changed or there are some invlaid input the form is not submitted.
   */
  function updateCosts(submit, listSize) {
    var invalid = 0;
    for (var idx = 0; idx < listSize; idx++) {
      input = submit.form["unitCost_o_"+idx];
      val = input.value;
      if (val != "") {
        // validate the values, unit cost must be a valid positive integer
        if(parseFloat(val) != val - 0 || parseFloat(val) < 0) {
          input.parentNode.parentNode.className = 'rowLightRed';
          invalid++;
        } else {
          input.parentNode.parentNode.className = idx % 2 == 0 ? 'rowWhite' : 'rowLightGray';
        }
      } else {
        input.parentNode.parentNode.className = idx % 2 == 0 ? 'rowWhite' : 'rowLightGray';
      }
    }
    if (invalid == 0) {
      return submitFormWithSingleClick(submit);
    } else {
      return false;
    }
  }
/*]]>*/
</script>

<@frameSection title=uiLabelMap.FinancialsAdjustInventoryValues>
  <form method="post" action="<@ofbizUrl>adjustInventoryValues</@ofbizUrl>" name="findForm">
    <@inputHidden name="performFind" value="Y"/>
    <table class="twoColumnForm">
      <@inputSelectRow title=uiLabelMap.OpentapsWarehouse name="facilityId" list=facilities displayField="facilityName" key="facilityId" required=false/>
      <@inputAutoCompleteProductRow name="productId" title=uiLabelMap.ProductProduct />
      <@inputSelectRow name="costingMethod" title=uiLabelMap.FinancialsDefaultCost list=costingMethods key="enumId" default=defaultCostingMethodId?if_exists; option>
        ${option.get("description", locale)}
      </@inputSelectRow>
      <@inputDateTimeRow name="receivedFromDate" title=uiLabelMap.FinancialsReceivedDateBetween form="findForm" default=receivedFromDate/>
      <@inputDateTimeRow name="receivedThruDate" title=uiLabelMap.CommonAnd?lower_case form="findForm" default=receivedThruDate/>
      <@inputSubmitRow title=uiLabelMap.CommonFind />
    </table>
  </form>
</@frameSection>

<form name="updateInventoryItems" method="post" action="<@ofbizUrl>updateInventoryItemValues</@ofbizUrl>">
  <@paginate name="adjustInventoryValues" list=inventoryItems>
    <#noparse>
      <@navigationHeader/>
      <table class="listTable">
        <tr class="listTableHeader">
          <@headerCell title=uiLabelMap.OpentapsWarehouse orderBy="facilityId" blockClass="tableheadtext"/>
          <@headerCell title=uiLabelMap.ProductProduct orderBy="productId" blockClass="tableheadtext"/>
          <@headerCell title=uiLabelMap.OpentapsItemID orderBy="inventoryItemId" blockClass="tableheadtext"/>
          <@headerCell title=uiLabelMap.OpentapsReceived orderBy="datetimeReceived" blockClass="tableheadtext"/>
          <@headerCell title=uiLabelMap.OpentapsQOH orderBy="quantityOnHandTotal" blockClass="tableheadtext"/>
          <@headerCell title=uiLabelMap.FinancialsUnitCost orderBy="unitCost" blockClass="tableheadtext"/>
          <@headerCell title=uiLabelMap.FinancialsNewUnitCost orderBy="" blockClass="tableheadtext"/>
          <td>
            <@inputMultiSelectAll form="updateInventoryItems"/>
          </td>
        </tr>
        <#list pageRows as invItem>
          <tr class="${tableRowClass(invItem_index)}">
            <@inputHidden name="facilityId" index=invItem_index value=invItem.facilityId?default("")/>
            <#assign facility>
              <#if invItem.facility?has_content>
                ${invItem.facility.facilityName?if_exists} (${invItem.facility.facilityId})
              </#if>
            </#assign>
            <@displayCell text=facility?if_exists/>
            <#assign product>
              <#if invItem.product?has_content>
                ${invItem.product.internalName?if_exists} (${invItem.product.productId})
              </#if>
            </#assign>
            <@displayCell text=product?if_exists/>
            <@inputHidden name="productId" index=invItem_index value=invItem.productId?default("")/>
            <#if invItem["inventoryItemValues"]?has_content>
              <@displayLinkCell href="javascript:opentaps.expandCollapse('${invItem.inventoryItemId}')" text="${invItem.inventoryItemId}"/>
            <#else>
              <@displayCell text=invItem.inventoryItemId/>
            </#if>
            <@inputHidden name="inventoryItemId" index=invItem_index value=invItem.inventoryItemId/>
            <#assign date>
              <#if invItem.datetimeReceived?has_content>${getLocalizedDate(invItem.datetimeReceived, "DATE")}</#if>
            </#assign>
            <@displayDateCell date=date/>
            <@displayCell text=invItem.quantityOnHandTotal?default(0) blockStyle="text-align:center"/>
            <@displayCell text=invItem.unitCost?default(0)/>
            <@inputTextCell name="unitCost" index=invItem_index size=10 default=invItem.displayCost/>
            <td>
              <@inputMultiCheck index=invItem_index/>
            </td>
          </tr>
          <tr class="${tableRowClass(invItem_index)}">
            <td colspan="8">
              <@flexArea targetId="${invItem.inventoryItemId}" style="border:none" controlClassOpen="hidden" controlClassClosed="hidden" state="closed" enabled=false>
                <table class="listTable" style="margin-bottom: 15px">
                  <tr class="listTableHeader" style="background-color:white">
                    <@displayCell text=uiLabelMap.FinancialsValuationDate/>
                    <@displayCell text=uiLabelMap.OpentapsUser/>
                    <@displayCell text=uiLabelMap.FinancialsUnitCostValuation blockStyle="text-align:right"/>
                  </tr>
                  <#assign inventoryItemValues = invItem["inventoryItemValues"]/>
                  <#list inventoryItemValues as inventoryItemValue>
                    <tr class="${tableRowClass(inventoryItemValue_index)}">
                      <#assign date>
                        <#if inventoryItemValue.dateTime?has_content>${getLocalizedDate(inventoryItemValue.dateTime, "DATE")}</#if>
                      </#assign>
                      <@displayDateCell date=date/>
                      <@displayCell text=inventoryItemValue.setByUserLogin?if_exists/>
                      <@displayCurrencyCell amount=inventoryItemValue.unitCost?if_exists currencyUomId=inventoryItemValue.curencyUomId?if_exists/>
                    </tr>
                  </#list>
                </table>
              </@flexArea>
            </td>
          </tr>
        </#list>
        <#if pageRows?has_content>
          <tr class="${tableRowClass(pageRows?size)}">
            <td colspan="8" style="text-align:right">
              <@inputHiddenRowCount pageRows/>
              <@inputHiddenUseRowSubmit/>
              <@inputSubmit title=uiLabelMap.FinancialsAdjustValues onClick="return updateCosts(this, ${pageRows?size})"/>
            </td>
          </tr>
        </#if>
      </table>
    </#noparse>
  </@paginate>
</form>
