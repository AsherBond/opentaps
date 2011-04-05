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

  <script type="text/javascript">
  /*<![CDATA[*/
  // Use JS Print Setup FireFox plugin for print web page without
  // without printer prompting
  function printDoc() {
    if(navigator.userAgent.toUpperCase().indexOf("FIREFOX") >= 0) {
      try {
        jsPrintSetup.setOption('orientation', jsPrintSetup.kPortraitOrientation);
		// set top margins in millimeters
		jsPrintSetup.setOption('marginTop', 10);
		jsPrintSetup.setOption('marginBottom', 10);
		jsPrintSetup.setOption('marginLeft', 10);
		jsPrintSetup.setOption('marginRight', 10);
		// set page header
		jsPrintSetup.setOption('headerStrLeft', '');
		jsPrintSetup.setOption('headerStrCenter', '');
		jsPrintSetup.setOption('headerStrRight', '');
		// set empty page footer
		jsPrintSetup.setOption('footerStrLeft', '');
		jsPrintSetup.setOption('footerStrCenter', '');
		jsPrintSetup.setOption('footerStrRight', '');
		// Suppress print dialog
		jsPrintSetup.setSilentPrint(true);/** Set silent printing */
		// Do Print
		jsPrintSetup.print();
		// Restore print dialog
		jsPrintSetup.setSilentPrint(false); /** Set silent printing back to false */
      }
      catch(e){
          window.print();
      }
    } else {
      window.print();
    }
  }
  /*]]>*/
  </script>

    <#if prev?has_content>
    <a href="<@ofbizUrl>shipmentLabelViewer?shipmentId=${prev.shipmentId}&amp;shipmentRouteSegmentId=${prev.shipmentRouteSegmentId}&amp;shipmentPackageSeqId=${prev.shipmentPackageSeqId}&amp;navigation=Y</@ofbizUrl>" class="linktext">${uiLabelMap.CommonPrevious}</a>
    </#if>
    <#if next?has_content>
    <a href="<@ofbizUrl>shipmentLabelViewer?shipmentId=${next.shipmentId}&amp;shipmentRouteSegmentId=${next.shipmentRouteSegmentId}&amp;shipmentPackageSeqId=${next.shipmentPackageSeqId}&amp;navigation=Y</@ofbizUrl>" class="linktext">${uiLabelMap.CommonNext}</a>
    </#if>
    <a href="<@ofbizUrl>Labels</@ofbizUrl>" class="linktext">All</a>
    <a href="javascript:printDoc()" class="linktext">Print</a><br/>
    <#if hasLabelImage>
    <#-- need to set width to 100% or label may print very large and not fit on the page -->
    <img src="<@ofbizUrl>viewShipmentPackageRouteSegLabelImage?shipmentId=${shipmentId}&amp;shipmentRouteSegmentId=${shipmentRouteSegmentId}&amp;shipmentPackageSeqId=${shipmentPackageSeqId}</@ofbizUrl>" width="100%"/>
    <p />
    <#else>
    <p>${uiLabelMap.WarehouseError_NoLabelImage}</p>
    </#if>
    
  </body>

</html>
