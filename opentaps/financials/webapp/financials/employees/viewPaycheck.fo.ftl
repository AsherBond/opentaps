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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.fo.ftl" />        

<#-- This file has been modified by Open Source Strategies, Inc. -->
        
<#-- 
Generates PDF of multiple checks in two styles: one check per page, multiple checks per page 
Note that this must be customized to fit specific check layouts. The layout here is copied
by hand from a real template using a ruler.
-->
<#escape x as x?xml>

<#macro printItems paycheck witholdings height>
<#-- this seems to be the only way to force a fixed height in fop -->
<fo:table table-layout="fixed">
  <fo:table-column column-width="100%"/>
  <fo:table-body>
    <fo:table-row height="${height}">
      <fo:table-cell>

        <fo:table height="${height}" table-layout="fixed" margin-left="5pt" margin-right="5pt">
          <fo:table-column column-width="proportional-column-width(45)"/>
          <fo:table-column column-width="proportional-column-width(40)"/>
          <fo:table-column column-width="proportional-column-width(15)"/>
          <fo:table-header>
            <fo:table-row>
              <fo:table-cell padding="2px" number-columns-spanned="2" text-align="center">
                <fo:block text-align="center">
                  ${paycheckPartyName}
                </fo:block>
              </fo:table-cell>
              <fo:table-cell padding="2px" number-columns-spanned="2" text-align="center">
                <fo:block text-align="center"><@displayDateFO date=paycheck.effectiveDate/></fo:block>
              </fo:table-cell>
            </fo:table-row>
            <fo:table-row>
              <fo:table-cell padding="2px"><fo:block font-weight="bold"></fo:block></fo:table-cell>
              <fo:table-cell padding="2px"><fo:block/></fo:table-cell>
              <fo:table-cell padding="2px"><fo:block font-weight="bold" text-align="right">${uiLabelMap.AccountingAmount}</fo:block></fo:table-cell>
            </fo:table-row>
          </fo:table-header>
          <fo:table-body>

              <fo:table-row>
                <fo:table-cell padding="2px" font-weight="bold"><fo:block>${uiLabelMap.FinancialsGrossPay}</fo:block></fo:table-cell>
                <fo:table-cell padding="2px" number-columns-spanned="2">
                    <fo:block text-align="end" font-weight="bold">${paycheck.amount?string.currency}</fo:block>
                </fo:table-cell>
              </fo:table-row>

            <#list witholdings as item>
                <fo:table-row>
                    <fo:table-cell padding="2px">
                        <fo:block>${item.typeDescription?if_exists}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell padding="2px">
                        <fo:block>${item.witholdingPartyName?if_exists}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell padding="2px">
                        <fo:block text-align="end">${item.amount?string.currency}</fo:block>
                    </fo:table-cell>
                </fo:table-row>
            </#list>

              <fo:table-row>
                <fo:table-cell padding="2px">
                  <fo:block font-weight="bold">${uiLabelMap.FinancialsNetPay}</fo:block>
                </fo:table-cell>
                <fo:table-cell padding="2px" number-columns-spanned="2">
                  <fo:block text-align="end" font-weight="bold">${netAmount?string.currency}</fo:block>
                </fo:table-cell>
              </fo:table-row>
            </fo:table-body>
        </fo:table>

    </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
</#macro>

<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format"
    <#if defaultFontFamily?has_content>font-family="${defaultFontFamily}"</#if>
>

  <fo:layout-master-set>

    <#-- define the margins of the check layout here -->
    <fo:simple-page-master master-name="checks" page-height="27.9cm" page-width="21.6cm">
      <fo:region-body margin-top="1.6cm"/>
    </fo:simple-page-master>

  </fo:layout-master-set>

  <fo:page-sequence master-reference="checks">
    <fo:flow flow-name="xsl-region-body">
      <#if !security.hasEntityPermission("ACCOUNTING", "_PRINT_CHECKS", session)>
      <fo:block padding="20pt">${uiLabelMap.AccountingPrintChecksPermissionError}</fo:block>
      <#else>

        <fo:block font-size="10pt" break-before="page"> <#-- this produces a page break if this block cannot fit on the current page -->
          <#if paycheck.statusId != "PMNT_CANCELLED" && paycheck.statusId != "PMNT_VOID">
    
            <#-- the check: note that the format is fairly precise -->
    
            <#-- this seems to be the only way to force a fixed height in fop -->
            <fo:table table-layout="fixed">
              <fo:table-column column-width="100%"/>
              <fo:table-body>
              <fo:table-row height="7.8cm">
              <fo:table-cell>
              
            <fo:table table-layout="fixed">
              <fo:table-column column-width="18.3cm"/>
              <fo:table-column/>
              <fo:table-body>
                <fo:table-row>
                  <fo:table-cell/>
                  <fo:table-cell>
                    <fo:block><@displayDateFO date=paycheck.effectiveDate/></fo:block>
                  </fo:table-cell>
                </fo:table-row>
    
                <#-- Party name and numerical amount row -->
                <fo:table-row>
                  <fo:table-cell padding-before="0.8cm">
                    <fo:block margin-left="3.0cm">
                      <#assign toPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", paycheck.partyIdTo, "compareDate", paycheck.effectiveDate, "userLogin", userLogin))/>
                      ${toPartyNameResult.fullName?default("Name Not Found")}
                    </fo:block>
                  </fo:table-cell>
                  <fo:table-cell padding-before="0.8cm">
                    <fo:block>**${paycheck.getBigDecimal("amount").setScale(2, rounding).toString()}</fo:block>
                  </fo:table-cell>
                </fo:table-row>
    
                <#-- Spelled out amount row -->
                <fo:table-row>
                  <fo:table-cell number-columns-spanned="2">
                    <#assign amount = Static["org.ofbiz.base.util.UtilNumber"].formatRuleBasedAmount(paycheck.getDouble("amount"), "%dollars-and-hundredths", locale).toUpperCase()>
                    <fo:block padding-before="0.6cm" margin-left="1.3cm">${amount}<#list 1..(100-amount.length()) as x>*</#list></fo:block>
                  </fo:table-cell>
                </fo:table-row>
    
                <#-- Print the billing address in the envelope-window area -->
                <fo:table-row height="2.0cm">
                  <fo:table-cell padding-before="0.5cm" number-columns-spanned="2">
                      <fo:block margin-left="3.0cm">
                      <@displayAddress postalAddress=billingAddress />
                          </fo:block> 
                  </fo:table-cell>
                </fo:table-row>
    
                <#-- memo line -->
                <#if paycheck.comments?has_content>
                <fo:table-row>
                  <fo:table-cell number-columns-spanned="2">
                    <fo:block margin-left="3.0cm">
                    <#compress>${paycheck.comments}</#compress>
                    </fo:block>
                  </fo:table-cell>
                </fo:table-row>
                </#if>
    
              </fo:table-body>
            </fo:table>
    
            </fo:table-cell>
            </fo:table-row>
            </fo:table-body>
            </fo:table>
    
            <#-- paycheck applications (twice: both blocks are exactly the same) -->
            <@printItems paycheck=paycheck witholdings=witholdings height="9.4cm" />
            <@printItems paycheck=paycheck witholdings=witholdings height="8.2cm" />
    
          <#else>
            ${uiLabelMap.OpentapsError_PaymentCannotPrint}           
          </#if>
        </fo:block>
      </#if> <#-- security if -->
    </fo:flow>
  </fo:page-sequence>
</fo:root>
</#escape>
