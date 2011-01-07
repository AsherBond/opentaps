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

<#assign nowTimestamp = Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()>

  <br />
  <div class="footer">
      <div>
        <a href="http://jigsaw.w3.org/css-validator/"><img style="border:0;width:88px;height:31px" src="<@ofbizContentUrl>/images/vcss.gif</@ofbizContentUrl>" alt="Valid CSS!"/></a>
        <a href="http://validator.w3.org/check?uri=referer"><img style="border:0;width:88px;height:31px" src="<@ofbizContentUrl>/images/valid-xhtml10.png</@ofbizContentUrl>" alt="Valid XHTML 1.0!"/></a>
      </div>
      <br />
      <div class="tabletext">
        <a href="http://ofbiz.apache.org">${uiLabelMap.EcommerceAboutUs}</a>
        <div class="tabletext">${uiLabelMap.CommonCopyright} (c) 2001-${nowTimestamp?string("yyyy")} The Apache Software Foundation - <a href="http://www.apache.org" class="tabletext">www.apache.org</a></div>
        <div class="tabletext">${uiLabelMap.CommonPoweredBy} <a href="http://ofbiz.apache.org" class="tabletext">Apache OFBiz</a></div>
        
      </div>
      <div class="tabletext">
        <a href="http://www.opentaps.org">${uiLabelMap.OpentapsProductName}</a> ${uiLabelMap.OpentapsReleaseVersion}.<br />
        Opentaps is a trademark of <a href="http://www.opentaps.org">Open Source Strategies, Inc.</a>
      </div>
      <br />
      <div class="tabletext"><a href="<@ofbizUrl>policies</@ofbizUrl>">${uiLabelMap.EcommerceSeeStorePoliciesHere}</a></div>
  </div>
