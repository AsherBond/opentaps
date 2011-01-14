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
<html>

  <head>
    <title>${uiLabelMap.PageTitleLabelPrinting}</title>
    <link rel="stylesheet" href="/opentaps_css/opentaps.css" type="text/css" />
  </head>

  <body style="padding-left: 10px;">
    <#if prev?has_content>
    <a href="<@ofbizUrl>shipmentLabelViewer?shipmentId=${prev.shipmentId}&amp;shipmentRouteSegmentId=${prev.shipmentRouteSegmentId}&amp;shipmentPackageSeqId=${prev.shipmentPackageSeqId}&amp;navigation=Y</@ofbizUrl>" class="linktext">${uiLabelMap.CommonPrevious}</a>
    </#if>
    <#if next?has_content>
    <a href="<@ofbizUrl>shipmentLabelViewer?shipmentId=${next.shipmentId}&amp;shipmentRouteSegmentId=${next.shipmentRouteSegmentId}&amp;shipmentPackageSeqId=${next.shipmentPackageSeqId}&amp;navigation=Y</@ofbizUrl>" class="linktext">${uiLabelMap.CommonNext}</a>
    </#if>
    <a href="<@ofbizUrl>Labels</@ofbizUrl>" class="linktext">All</a><br/>
    <#if hasLabelImage>
    <#-- need to set width to 100% or label may print very large and not fit on the page -->
    <img src="<@ofbizUrl>viewShipmentPackageRouteSegLabelImage?shipmentId=${shipmentId}&amp;shipmentRouteSegmentId=${shipmentRouteSegmentId}&amp;shipmentPackageSeqId=${shipmentPackageSeqId}</@ofbizUrl>" width="100%"/>
    <p />
    <#else>
    <p>${uiLabelMap.WarehouseError_NoLabelImage}</p>
    </#if>
    
  </body>

</html>
