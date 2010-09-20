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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.fo.ftl"/>

<#escape x as x?xml>

        <#assign fromPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", returnHeader.fromPartyId, "compareDate", returnHeader.entryDate, "userLogin", userLogin))/>
        <#assign toPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", returnHeader.toPartyId, "compareDate", returnHeader.entryDate, "userLogin", userLogin))/>
        
        <fo:block><fo:leader/></fo:block>
        <fo:table>
          <fo:table-column column-width="3.50in"/>
          <fo:table-column column-width="1.00in"/>
          <fo:table-column column-width="2.75in"/>
          <fo:table-body>
          <fo:table-row>

            <fo:table-cell>
            <fo:table border-style="solid" border-width="0.2pt" height="1in">
              <fo:table-column column-width="3.50in"/>
              <fo:table-body>
                <fo:table-row><fo:table-cell border-style="solid" border-width="0.2pt" padding="1mm"><fo:block font-weight="bold" font-size="10pt">${uiLabelMap.CrmReturnFrom}</fo:block></fo:table-cell></fo:table-row>
                <fo:table-row><fo:table-cell padding="1mm">
                  <fo:block white-space-collapse="false" >
                    <#if fromPartyNameResult.fullName?has_content>
                      ${fromPartyNameResult.fullName}
                    <#else>
                      <#if postalAddressFrom?exists>
                        <#if (postalAddressFrom.toName)?has_content>
                          ${postalAddressFrom.toName}
                        </#if>
                        <#if (postalAddressFrom.attnName)?has_content>
                          ${postalAddressFrom.attnName}
                        </#if>
                      </#if>
                    </#if>
                    <@displayAddress postalAddress=postalAddressFrom/>
                  </fo:block>
                </fo:table-cell></fo:table-row>
              </fo:table-body>
            </fo:table>
            </fo:table-cell>

            <fo:table-cell/>

            <fo:table-cell>
            <fo:table border-style="solid" border-width="0.2pt" height="1in">
              <fo:table-column column-width="2.75in"/>
              <fo:table-body>
                <fo:table-row><fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt"><fo:block font-weight="bold" font-size="10pt">${uiLabelMap.CrmReturnTo}</fo:block></fo:table-cell></fo:table-row>
                <fo:table-row><fo:table-cell padding="1mm">
                  <fo:block white-space-collapse="flase">
                    <#if toPartyNameResult.fullName?has_content>
                      ${toPartyNameResult.fullName}
                    <#else>
                      <#if postalAddressTo?exists>
                        <#if (postalAddressTo.toName)?has_content>
                          ${postalAddressTo.toName}
                        </#if>
                        <#if (postalAddressTo.attnName)?has_content>
                          ${postalAddressTo.attnName}
                        </#if>
                      </#if>
                    </#if>
                    <@displayAddress postalAddress=postalAddressTo/>
                  </fo:block>
                </fo:table-cell></fo:table-row>
              </fo:table-body>
            </fo:table>
            </fo:table-cell>
              
          </fo:table-row>
          </fo:table-body>
          </fo:table>

          <fo:block space-after="10pt"/>
</#escape>
