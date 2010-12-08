<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<#-- This file has been modified by Open Source Strategies, Inc. -->

<#-- FOP may require a library called JIMI to print certain graphical formats such as GIFs.  Jimi is a Sun library which cannot
be included in OFBIZ due to licensing incompatibility, but you can download it yourself at: http://java.sun.com/products/jimi/
and rename the ZIP file that comes with it as jimi-xxx.jar, then copy it into the same directory as fop.jar, which at this time 
is ${ofbiz.home}/framework/webapp/lib/ -->

<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
    <fo:layout-master-set>
        <#-- these margins are arbitrary, please redefine as you see fit -->
        <fo:simple-page-master master-name="main-page"
            margin-top="0in" margin-bottom="0in"
            margin-left="0in" margin-right="0in">
          <fo:region-body/>  <#-- main body -->
        </fo:simple-page-master>
  </fo:layout-master-set>

  <fo:page-sequence master-reference="main-page">
       <fo:flow flow-name="xsl-region-body">
       <#list segments as segment>
         <#if segment.carrierPartyId == "DHL">
         <fo:block break-before="page" padding-top="1.5cm" margin-left="1.5cm">
           <fo:external-graphic src="<@ofbizUrl>viewShipmentLabel?shipmentId=${segment.shipmentId}&amp;shipmentRouteSegmentId=${segment.shipmentRouteSegmentId}&amp;shipmentPackageSeqId=${segment.shipmentPackageSeqId}</@ofbizUrl>"
                content-type="content-type:${imageContentType}" content-width="196mm" content-height="112mm"></fo:external-graphic>
         </fo:block>
         <#elseif segment.carrierPartyId == "UPS">
         <fo:block break-before="page" padding-top="1.5cm" margin-left="1.5cm">
           <#-- image is scaled up 25% to fit a particular printer label -->
           <fo:external-graphic src="<@ofbizUrl>viewShipmentLabel?shipmentId=${segment.shipmentId}&amp;shipmentRouteSegmentId=${segment.shipmentRouteSegmentId}&amp;shipmentPackageSeqId=${segment.shipmentPackageSeqId}</@ofbizUrl>"
                content-type="content-type:${imageContentType}" content-width="196mm" content-height="112mm"></fo:external-graphic>
         </fo:block>
         <#else>
         <fo:block break-before="page" padding-top="1.5cm" margin-left="1.5cm">
           <fo:external-graphic src="<@ofbizUrl>viewShipmentLabel?shipmentId=${segment.shipmentId}&amp;shipmentRouteSegmentId=${segment.shipmentRouteSegmentId}&amp;shipmentPackageSeqId=${segment.shipmentPackageSeqId}</@ofbizUrl>"
                content-type="content-type:${imageContentType}" content-width="196mm" content-height="112mm"></fo:external-graphic>
         </fo:block>
         </#if>
      </#list>
      <#if segments.size() == 0>
        <fo:block>No Shipping Labels Selected</fo:block>
      </#if>
      </fo:flow>
  </fo:page-sequence>
</fo:root>
