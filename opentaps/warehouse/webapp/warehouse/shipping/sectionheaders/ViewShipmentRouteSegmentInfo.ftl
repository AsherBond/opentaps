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

<@sectionHeader title=uiLabelMap.ProductRouteSegments>
      <div class="subMenuBar">
        <select name="viewLabelId" id="viewLabelId" onChange="opentaps.changeLocation(null, 'viewLabelId');">
            <option value="">View Label ... </option>

            <#if shipmentRouteSegmentDatas?has_content> 
            <#list shipmentRouteSegmentDatas as shipmentRouteSegmentData>
                <#assign shipmentRouteSegment = shipmentRouteSegmentData.shipmentRouteSegment>
                <#assign shipmentPackageRouteSegs = shipmentRouteSegmentData.shipmentPackageRouteSegs?if_exists>
                <#list shipmentPackageRouteSegs as shipmentPackageRouteSeg>
                    <#if shipmentPackageRouteSeg.trackingCode?has_content>
                        <option value="<@ofbizUrl>viewShipmentPackageRouteSegLabelImage?shipmentId=${parameters.shipmentId}&shipmentRouteSegmentId=${shipmentRouteSegment.shipmentRouteSegmentId}&shipmentPackageSeqId=${shipmentPackageRouteSeg.shipmentPackageSeqId}</@ofbizUrl>">View Label ${shipmentRouteSegment.shipmentRouteSegmentId}</option>
                    </#if>
                </#list>
            </#list>
            </#if>
        </select>

        <a href="<@ofbizUrl>EditShipmentRouteSegments?shipmentId=${parameters.shipmentId}</@ofbizUrl>" class="subMenuButton">${uiLabelMap.CommonEdit}</a>
      </div>
</@sectionHeader>
