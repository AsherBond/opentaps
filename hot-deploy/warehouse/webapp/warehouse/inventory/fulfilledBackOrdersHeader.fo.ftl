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
                <fo:table>
                  <fo:table-column column-width="1.5in"/>
                  <fo:table-column column-width="1.5in"/>

                  <fo:table-body>
                                
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.ProductPurchaseOrder} #</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block>${orderId}</fo:block></fo:table-cell>
                    </fo:table-row>
                  </fo:table-body>
                </fo:table>
</#escape>
