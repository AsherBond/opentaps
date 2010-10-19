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

<fo:table padding-before="0.5in" font-size="9pt">
    <fo:table-column column-width="1.5in"/>
    <fo:table-column column-width="1.5in"/>
    <fo:table-column column-width="2.5in"/>
    <fo:table-column column-width="1in"/>
    <fo:table-body>
        <#if order.payments?has_content>
            <fo:table-row>
                <fo:table-cell number-columns-spanned="3">
                    <fo:block font-weight="bold">${uiLabelMap.OpentapsPaymentsReceived}:</fo:block>
                </fo:table-cell>
            </fo:table-row>
            <fo:table-row>
                <fo:table-cell padding-before="0.2in">
                    <fo:block font-weight="bold">${uiLabelMap.CommonDate}</fo:block>
                </fo:table-cell>
                <fo:table-cell padding-before="0.2in">
                    <fo:block font-weight="bold">${uiLabelMap.FinancialsPaymentMethod}</fo:block>
                </fo:table-cell>
                <fo:table-cell padding-before="0.2in">
                    <fo:block font-weight="bold">${uiLabelMap.FormFieldTitle_paymentRefNum}</fo:block>
                </fo:table-cell>
                <fo:table-cell padding-before="0.2in">
                    <fo:block font-weight="bold">${uiLabelMap.AccountingAmount}</fo:block>    
                </fo:table-cell>
            </fo:table-row>
            <#list order.payments as payment>
              <fo:table-row>
                <fo:table-cell>
                  <fo:block>${payment.effectiveDate?if_exists?date}</fo:block>
                </fo:table-cell>
                <fo:table-cell>
                  <fo:block>
                    <#if payment.paymentMethod?has_content && payment.paymentMethod.isCreditCard()>
                      <#if payment.creditCard?has_content>
                        <#assign cc=payment.creditCard/>
                        ${cc.cardType} ${cc.cardNumberStripped} ${cc.expireDate}
                      </#if>
                    <#else/>
                      ${payment.paymentMethodType.get("description", locale)?if_exists}
                    </#if>
                  </fo:block>
                </fo:table-cell>
                <fo:table-cell>
                  <fo:block>${payment.paymentRefNum?if_exists}</fo:block>
                </fo:table-cell>
                <fo:table-cell text-align="right">
                  <fo:block><@ofbizCurrency amount=payment.amount isoCode=payment.currencyUomId/></fo:block>    
                </fo:table-cell>
              </fo:table-row>
            </#list>
              <fo:table-row>
                <fo:table-cell></fo:table-cell>
                <fo:table-cell></fo:table-cell>
                <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.CrmOrderTotalPayments}</fo:block></fo:table-cell>
                <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=order.paymentTotal isoCode=order.currencyUom/></fo:block></fo:table-cell>
              </fo:table-row>
        <#else>
          <fo:table-row>
            <fo:table-cell number-columns-spanned="3">
              <fo:block font-weight="bold">${uiLabelMap.OpentapsPaymentsReceived}:</fo:block>
            </fo:table-cell>
            <fo:table-cell text-align="right"><fo:block>${uiLabelMap.CommonNone}</fo:block></fo:table-cell>
          </fo:table-row>
        </#if>
    </fo:table-body>
</fo:table>
<fo:table padding-before="0.5in" font-size="9pt">
    <fo:table-column column-width="1.5in"/>
    <fo:table-column column-width="1.5in"/>
    <fo:table-column column-width="2.5in"/>
    <fo:table-column column-width="1in"/>
    <fo:table-body>
        <fo:table-row padding-before="0.25in">
            <fo:table-cell></fo:table-cell>
            <fo:table-cell></fo:table-cell>
            <fo:table-cell><fo:block font-weight="bold" text-align="right">${uiLabelMap.CrmOrderRemainingTotal}</fo:block></fo:table-cell>
            <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=order.openAmount isoCode=order.currencyUom/></fo:block></fo:table-cell>
        </fo:table-row>
    </fo:table-body>
</fo:table>

