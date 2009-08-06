<#--
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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

<#if originFacility?exists && originFacility.facilityId = parameters.facilityId>
<#assign outboundLinks><a href="<@ofbizUrl>ShipmentBarCode.pdf?shipmentId=${parameters.shipmentId}</@ofbizUrl>" target="_blank" class="subMenuButton">${uiLabelMap.WarehouseBarCode}</a><a href="<@ofbizUrl>PackingSlip.pdf?shipmentId=${parameters.shipmentId}</@ofbizUrl>" target="_blank" class="subMenuButton">${uiLabelMap.ProductPackingSlip}</a></#assign>
</#if>

<@sectionHeader title=uiLabelMap.OrderShipmentInformation>
      <div class="subMenuBar"><a href="<@ofbizUrl>EditShipment?shipmentId=${parameters.shipmentId}</@ofbizUrl>" class="subMenuButton">${uiLabelMap.CommonEdit}</a>${outboundLinks?if_exists}</div>
</@sectionHeader>
