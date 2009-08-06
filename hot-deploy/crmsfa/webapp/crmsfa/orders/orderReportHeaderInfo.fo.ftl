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

<#escape x as x?xml>
                  <fo:table font-size="11pt">
                    <fo:table-column column-width="1.5in"/>
                    <fo:table-column column-width="1.5in"/>
                    <fo:table-body>
                    <fo:table-row>
                      <fo:table-cell number-columns-spanned="2">
                        <fo:block font-weight="bold">
                            ${order.type.get("description",locale)} ${uiLabelMap.OrderOrder}
                        </fo:block>
                      </fo:table-cell>
                    </fo:table-row>
                    
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.OrderDateOrdered}</fo:block></fo:table-cell>
                      <#assign dateFormat = Static["java.text.DateFormat"].LONG/>
                      <#assign orderDate = Static["java.text.DateFormat"].getDateInstance(dateFormat,locale).format(order.orderDate)/>
                      <fo:table-cell><fo:block>${orderDate}</fo:block></fo:table-cell>
                    </fo:table-row>
                                  
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.OrderOrder} #</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block>${order.orderId}</fo:block></fo:table-cell>
                    </fo:table-row>

                    <#if order.primaryPoNumber?has_content>
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.OpentapsPONumber}</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block>${order.primaryPoNumber}</fo:block></fo:table-cell>
                    </fo:table-row>
                    </#if>

                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.OrderCurrentStatus}</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block font-weight="bold">${order.status.get("description",locale)}</fo:block></fo:table-cell>
                    </fo:table-row>
                  </fo:table-body>
                </fo:table>
</#escape>
