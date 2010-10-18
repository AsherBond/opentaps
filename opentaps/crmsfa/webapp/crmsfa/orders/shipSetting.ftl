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

<#-- page to select shipment address, carrier, and method -->

<form name="shipSetting" action="setShipmentOption" method="POST">

<div class="subSectionBlock">
<table class="shipSetting">

  <#-- header row -->
  <tr>
    <th>${uiLabelMap.CrmOrderItem}</th>
    <th>${uiLabelMap.CrmOrderItemQty}</th>
    <th>${uiLabelMap.OpentapsShippingAddress}</th>
    <th>${uiLabelMap.CrmOrderShippingMethod}</th>
  </tr>

  <#-- list each item -->
  <#list shoppingCart.items() as item>
  <#assign index = cart.getItemIndex(item) />
  <#assign defaultCarrierMethod = selectedCarrierMethods.get(index)?default("NA") />
  <#assign defaultContactMechId = selectedContactMechIds.get(index)?default("NA") />
  <input type="hidden" name="_rowSubmit_o_${index}" value="Y"/> <#-- triggers multi form parsing -->

  <tr>
    <#-- column for cart item details -->
    <td>
      ${item.getProductId()} - ${item.getName()}
    </td>

    <#-- column for quantity -->
    <td>
      ${item.getQuantity()}
    </td>

    <#-- column for listing the shipment options -->
    <td>
      <select name="contactMechId_o_${index}" class="inputBox">
        <option value="">${uiLabelMap.CommonNone}</option>

        <#assign i = 0 />
        <#list shippingContactMechList as contactMech>
        <#assign address = contactMech.getRelatedOne("PostalAddress") />
        <#if defaultContactMechId == address.contactMechId><#assign selected = "selected"/><#else><#assign selected = ""/></#if>

        <option value="${address.contactMechId}" ${selected}>
            <#if address.toName?has_content>${address.toName}, </#if>
            <#if address.address1?has_content>${address.address1}, </#if>
            <#if address.city?has_content>${address.city}, </#if>
            <#if address.stateProvinceGeoId?has_content>${address.stateProvinceGeoId} </#if>
            <#if address.postalCode?has_content> ${address.postalCode}</#if>
        </option>

        <#assign i = i + 1 />
        </#list>

      </select>
    </td>

    <#-- column for carrier and shipment method -->
    <td>
      <select name="carrierPartyAndShipmentMethodTypeId_o_${index}" class="inputBox">
        <option value="_NA_^NO_SHIPPING">None</option>

        <#-- list each with a composite key of carrierPartyId^shipmentMethodTypeId -->
        <#list carrierShipmentMethodList as carrierMethod>
        <#assign keyToUse = carrierMethod.partyGroup.partyId + "^" + carrierMethod.shipmentMethodType.shipmentMethodTypeId /> 
        <#if defaultCarrierMethod == keyToUse><#assign selected = "selected"/><#else><#assign selected = ""/></#if>

        <option value="${keyToUse}" ${selected}>
          ${carrierMethod.partyGroup.groupName} ${carrierMethod.shipmentMethodType.description}
        </option>

        </#list>

      </select>
    </td>
  </tr>

  </#list>

  <tr>
    <td colspan="2">&nbsp;</td>
    <td>
      <a class="buttontext" href="<@ofbizUrl>ordersEditContactMech?partyId=${shoppingCart.getOrderPartyId()}&preContactMechTypeId=POSTAL_ADDRESS&DONE_PAGE=shipmentSetting&contactMechPurposeTypeId=SHIPPING_LOCATION</@ofbizUrl>">${uiLabelMap.PartyAddNewAddress}</a>
    </td>
    <td>&nbsp;</td> <#-- need this or the box won't close properly -->
  </tr>

</table>
</div>

</form>
