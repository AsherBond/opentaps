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
 *  
-->
 
<#-- This file has been modified from the version included with the Apache licensed OFBiz accounting application -->
<#-- This file has been modified by Open Source Strategies, Inc. -->

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

<#-- XXX NOTE:  If any new context variables are used in this file, make sure to add them to the bodyParameters map in the financials sendInvoiceEmail() service. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.fo.ftl"/>        

<#escape x as x?xml>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format"
    <#if defaultFontFamily?has_content>font-family="${defaultFontFamily}"</#if>
>
    <#-- master layout specifies the overall layout of the pages and its different sections. -->
    <fo:layout-master-set>
        <fo:simple-page-master master-name="my-page"
            margin-top="1in" margin-bottom="0in"
            margin-left="20mm" margin-right="20mm">
            <fo:region-body margin-top="3in" margin-bottom="1in"/>  <#-- main body -->
            <fo:region-before extent="3.5in"/>  <#-- a header -->
            <fo:region-after extent="1in"/>  <#-- a footer -->
        </fo:simple-page-master>
    </fo:layout-master-set>

    <#assign invoice = invoices.get(0)>

    <fo:page-sequence master-reference="my-page" initial-page-number="1" font-family="Helvetica">
        <fo:static-content flow-name="xsl-region-before">
            <@opentapsHeaderFO/>
            <fo:block white-space-collapse="false" space-after=".10in"> </fo:block> 
            <fo:table>
                <fo:table-column column-width="3.5in"/>
                <fo:table-column column-width="3in"/>
                <fo:table-body>
                    <fo:table-row >
                        <fo:table-cell font-size="10pt">
                            <#assign billingPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", invoice.billingPartyId, "compareDate", invoice.invoiceDate, "userLogin", userLogin))/>
                            <@displayAddress postalAddress=invoice.invoiceAddress partyName=billingPartyNameResult.fullName/>
                            <#if billingPartyTaxId?has_content>
                                <fo:block>Tax ID: ${billingPartyTaxId}</fo:block>
                            </#if>            
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block>
                                <fo:table>
                                    <fo:table-column column-width="1in"/>
                                    <fo:table-column column-width="1.5in"/>
                                    <fo:table-body>
                                        <fo:table-row>
                                            <fo:table-cell>
                                                <fo:block number-columns-spanned="2" font-weight="bold" wrap-option="no-wrap">Sample Invoice</fo:block>
                                            </fo:table-cell>
                                        </fo:table-row>
                                        <fo:table-row>
                                            <fo:table-cell><fo:block>${uiLabelMap.AccountingInvoice}</fo:block></fo:table-cell>
                                            <fo:table-cell><fo:block><#if invoice?has_content>${invoice.invoiceId?default("")}</#if></fo:block></fo:table-cell>
                                        </fo:table-row>
                                        <#if invoice.invoiceDate?has_content>
                                        <fo:table-row>
                                            <fo:table-cell><fo:block>${uiLabelMap.CommonDate}</fo:block></fo:table-cell>
                                            <fo:table-cell><fo:block><@displayDateFO date=invoice.invoiceDate/></fo:block></fo:table-cell>
                                        </fo:table-row>
                                        </#if>
                                        <#if invoice.dueDate?has_content>
                                        <fo:table-row>
                                            <fo:table-cell><fo:block>${uiLabelMap.AccountingDueDate}</fo:block></fo:table-cell>
                                            <fo:table-cell><fo:block><@displayDateFO date=invoice.dueDate/></fo:block></fo:table-cell>
                                        </fo:table-row>
                                        </#if>
                                        <#--fo:table-row>
                                          <fo:table-cell><fo:block>${uiLabelMap.CommonStatus}</fo:block></fo:table-cell>
                                          <fo:table-cell><fo:block font-weight="bold">${invoice.getRelatedOne("StatusItem").get("description",locale)}</fo:block></fo:table-cell>
                                        </fo:table-row-->
                                    </fo:table-body>
                                </fo:table>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
    
            <fo:block white-space-collapse="false"> </fo:block> 
                    
            <#-- list of orders -->
            <#if invoice.orderIds?has_content>
                <fo:table>
                    <fo:table-column column-width="1in"/>
                    <fo:table-column column-width="5.5in"/>
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell>
                                <fo:block font-size="10pt" font-weight="bold">${uiLabelMap.OrderOrders}:</fo:block>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block font-size ="10pt" font-weight="bold"><#list orderIds?default([]) as orderId> ${orderId} </#list></fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </fo:table-body>
                </fo:table>
            </#if>

            <#-- TODO: put shipment information here or somewhere -->
    
            <fo:block white-space-collapse="false"> </fo:block>
    
        </fo:static-content>

        <#-- this part is the footer.  Use it for standard boilerplate text. -->
        <fo:static-content flow-name="xsl-region-after">
            <fo:block border-style="solid" font-size="10pt">
              I hereby certify that the information in this invoice is true and correct and that the contents of this shipment are as stated above.
            </fo:block>
          <#-- displays page number.  "theEnd" is an id of a fo:block at the very end -->    
          <#--
          <fo:block font-size="10pt" text-align="center">${uiLabelMap.CommonPage} <fo:page-number/> ${uiLabelMap.CommonOf} <fo:page-number-citation ref-id="theEnd"/></fo:block>
          <fo:block font-size="10pt"/>
          <fo:block font-size="8pt" text-align="center">${uiLabelMap.OpentapsProductName} - www.opentaps.org</fo:block>
          -->
        </fo:static-content>

        <#-- this part is the main body which lists the terms and items -->
        <fo:flow flow-name="xsl-region-body">

          <#if invoiceTerms?has_content>
          <fo:table>
                    <fo:table-column column-width="66mm"/>
                    <fo:table-column column-width="13mm"/>
                    <fo:table-column column-width="13mm"/>
                    <fo:table-column column-width="64mm"/>
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
                                <fo:block font-weight="bold">${uiLabelMap.OrderOrderTermType}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
                                <fo:block font-weight="bold" text-align="right">${uiLabelMap.CommonValue}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
                                <fo:block font-weight="bold" text-align="right">${uiLabelMap.CommonDays}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
                                <fo:block font-weight="bold" text-align="right">${uiLabelMap.CommonDescription}</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <#list invoiceTerms as invoiceTerm>
                            <fo:table-row>
                                <#--fo:table-cell>
                                    <#if invoiceTerm.invoiceItemSeqId?default("_NA_") != "_NA_">
                                        <fo:block>${invoiceTerm.invoiceItemSeqId}</fo:block> 
                                    </#if>
                                </fo:table-cell-->
                                <fo:table-cell>
                                    <fo:block>${invoiceTerm.getRelatedOneCache("TermType").get("description")}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell>
                                    <#if invoiceTerm.termValue?exists>
                                        <fo:block text-align="right">${invoiceTerm.termValue}</fo:block>
                                    </#if>
                                </fo:table-cell>
                                <fo:table-cell>
                                    <#if invoiceTerm.termDays?exists>
                                        <fo:block text-align="right">${invoiceTerm.termDays}</fo:block>
                                    </#if>
                                </fo:table-cell>
                                <#--fo:table-cell>
                                    <#if invoiceTerm.uomId?exists>
                                        <#assign uom = invoiceTerm.getRelatedOneCache("Uom")/>
                                        <fo:block text-align="right">${uom.description?if_exists}</fo:block>
                                    </#if>
                                </fo:table-cell-->
                                <fo:table-cell>
                                    <#if invoiceTerm.textValue?exists>
                                        <fo:block text-align="right">${invoiceTerm.textValue}</fo:block>
                                    </#if>
                                </fo:table-cell>
                            </fo:table-row>
                        </#list>
                    </fo:table-body>
                </fo:table>

                <fo:block white-space-collapse="false" space-after=".25in"> </fo:block>

            </#if>

            <fo:table>
                <fo:table-column column-width="30mm"/>
                <fo:table-column column-width="65mm"/>
                <fo:table-column column-width="10mm"/>
                <fo:table-column column-width="30mm"/>
                <fo:table-column column-width="30mm"/>
                
                <fo:table-header height="14px" font-size="11pt">
                    <fo:table-row>
                        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
                            <fo:block font-weight="bold">Code</fo:block>
                        </fo:table-cell>
                        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
                            <fo:block font-weight="bold">${uiLabelMap.CommonDescription}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
                            <fo:block font-weight="bold" text-align="right">Qty</fo:block>
                        </fo:table-cell>
                        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
                            <fo:block font-weight="bold" text-align="right">UNIT VALUE</fo:block>
                        </fo:table-cell>
                        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
                            <fo:block font-weight="bold" text-align="right">SUBTOTAL</fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-header>
      
                <fo:table-body font-size="10pt">

                    <#list invoiceLines?default([]) as invoiceItem>
                      <#if invoiceItem.orderItem?exists>  <#-- only show line items which are order items -->
                        <fo:table-row height="7px">
                            <fo:table-cell number-columns-spanned="6">
                                <fo:block><#-- blank line --></fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <fo:table-row height="14px" space-start=".15in">
                            <fo:table-cell>
                                <fo:block> ${invoiceItem.productId?if_exists} </fo:block>               
                            </fo:table-cell>    
                            <fo:table-cell>
                                <fo:block> ${invoiceItem.description?if_exists} </fo:block>               
                            </fo:table-cell>       
                            <fo:table-cell>
                                <fo:block text-align="right"> <#if invoiceItem.quantity?exists>${invoiceItem.quantity?string.number}</#if> </fo:block>               
                            </fo:table-cell>
                            <fo:table-cell text-align="right">
                                <fo:block>Sample</fo:block>               
                            </fo:table-cell>
                            <fo:table-cell text-align="right">
                              <fo:block>Sample</fo:block>               
                            </fo:table-cell>
                        </fo:table-row>
                      </#if>
                    </#list>
                    
                    <#-- blank line -->
                    <fo:table-row height="7px">
                        <fo:table-cell number-columns-spanned="4"><fo:block><#-- blank line --></fo:block></fo:table-cell>
                    </fo:table-row>
                    
                    <#-- the customs and grand total -->
                    <fo:table-row>
                        <fo:table-cell number-columns-spanned="3"></fo:table-cell>
                        <fo:table-cell>
                            <fo:block font-weight="bold">Customs Value</fo:block>
                        </fo:table-cell>
                        <fo:table-cell text-align="right">
                            <fo:block font-weight="bold"><@ofbizCurrency amount=1 isoCode=invoice.currencyUomId?if_exists/></fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                        <fo:table-cell number-columns-spanned="3"></fo:table-cell>
                        <fo:table-cell>
                            <fo:block font-weight="bold">${uiLabelMap.FinancialsTotalCapital}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell text-align="right">
                            <fo:block font-weight="bold"><@ofbizCurrency amount=1 isoCode=invoice.currencyUomId?if_exists/></fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>        
            </fo:table>

         <fo:block id="theEnd"/>  <#-- marks the end of the pages and used to identify page-number at the end -->
       </fo:flow>
    </fo:page-sequence>
</fo:root>
</#escape>
