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
            <fo:region-body margin-top="2.2in" margin-bottom="1in"/>  <#-- main body -->
            <fo:region-before extent="4in"/>  <#-- a header -->
            <fo:region-after extent="1in"/>  <#-- a footer -->
        </fo:simple-page-master>
    </fo:layout-master-set>

    <fo:page-sequence master-reference="my-page" initial-page-number="1">

       <#-- the region-before and -after must be declared as fo:static-content and before the fo:flow.  only 1 fo:flow per
            fo:page-sequence -->
       <fo:static-content flow-name="xsl-region-before">
            <@opentapsHeaderFO/>

            <#-- Inserts a newline.  white-space-collapse="false" specifies that the stuff inside fo:block is to repeated verbatim -->
            <fo:table>
                <fo:table-body>
                <fo:table-row height="15px">
                    <fo:table-cell number-columns-spanned="4"><fo:block><#-- blank line --></fo:block></fo:table-cell>
                </fo:table-row>
                    <fo:table-row>
                      <fo:table-cell number-columns-spanned="4">
                         <fo:block font-weight="bold" wrap-option="no-wrap" text-align="center">
                           ${uiLabelMap.FinancialsStatementFor} ${Static['org.ofbiz.party.party.PartyHelper'].getPartyName(delegator, partyId, false)} (${partyId})
                         </fo:block>
                      </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                      <fo:table-cell number-columns-spanned="4">
                         <fo:block font-weight="bold" wrap-option="no-wrap" text-align="center">
                           ${uiLabelMap.CommonFrom} <@displayDateFO date=fromDate/> ${uiLabelMap.CommonThru} <@displayDateFO date=thruDate/>
                         </fo:block>
                      </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
       </fo:static-content>

       <@opentapsFooterFO />

       <#-- this part is the main body which lists the items -->
       <fo:flow flow-name="xsl-region-body">
            <fo:table>
            <fo:table-column column-width="30mm"/>
            <fo:table-column column-width="90mm"/>
            <fo:table-column column-width="25mm"/>
            <fo:table-column column-width="25mm"/>
            
            <fo:table-header height="14px">
              <fo:table-row>
                <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
                  <fo:block font-weight="bold">${uiLabelMap.CommonDate}</fo:block>
                </fo:table-cell>
                <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
                  <fo:block font-weight="bold">${uiLabelMap.FinancialsTransaction}</fo:block>
                </fo:table-cell>
                <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
                  <fo:block font-weight="bold" text-align="center">${uiLabelMap.AccountingAmount}</fo:block>
                </fo:table-cell>
                <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
                  <fo:block font-weight="bold" text-align="center">${uiLabelMap.FinancialsStatementsBalance}</fo:block>
                </fo:table-cell>
              </fo:table-row>
            </fo:table-header>
      
            
            <fo:table-body font-size="10pt">
                <#-- blank line -->
                <fo:table-row height="7px">
                    <fo:table-cell number-columns-spanned="4"><fo:block><#-- blank line --></fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row height="14px">
                    <fo:table-cell>
                        <fo:block><#-- blank column --></fo:block>
                    </fo:table-cell>
                    <fo:table-cell>
                        <fo:block text-align="" font-weight="bold">${uiLabelMap.FinancialsStatementsBeginningBalance}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell number-columns-spanned="2">
                        <fo:block font-weight="bold" text-align="right"> <@ofbizCurrency amount=beginningBalance isoCode=orgCurrencyUomId/> </fo:block>
                    </fo:table-cell>
                </fo:table-row>
                <#assign endingBalance=0 />
                <#list transactions as transaction>
                   <fo:table-row height="14px" space-start=".15in">
                       <fo:table-cell>
                           <fo:block> <@displayDateFO date=transaction.transactionDate format="DATE"/> </fo:block>               
                       </fo:table-cell>    
                       <fo:table-cell>
                           <fo:block> 
                              <#if transaction.acctgTransTypeId="REVERSE">${uiLabelMap.FinancialsReversalOf}</#if>
                              <#if transaction.acctgTransTypeId="WRITEOFF">${uiLabelMap.FinancialsWriteoffOf}</#if>
                              <#if transaction.invoiceId?exists>
                                 <#-- display invoice type and invoice number with links -->
                                 ${transaction.invoiceType?default("")} # ${transaction.invoiceId}
                              <#elseif transaction.paymentId?exists>
                                <#-- display payment type and payment number -->
                                ${transaction.paymentType?default("")} # ${transaction.paymentId}
                              <#else>
                                <#-- not an invoice or payment?  display type of accounting transaction and details from transaction description -->
                                ${transaction.transType?default("")}: ${transaction.transDescription?default("")}
                              </#if>
                           </fo:block>               
                       </fo:table-cell>    
                       <fo:table-cell>
                           <fo:block text-align="right"> <@ofbizCurrency amount=transaction.amount isoCode=orgCurrencyUomId/> </fo:block>               
                       </fo:table-cell>       
                       <fo:table-cell text-align="right">
                           <fo:block> <@ofbizCurrency amount=transaction.runningBalance isoCode=orgCurrencyUomId/> </fo:block>               
                       </fo:table-cell>
                   </fo:table-row>
                   <#assign endingBalance=transaction.runningBalance />
                </#list>

                <#-- blank line -->
                <fo:table-row height="7px">
                    <fo:table-cell number-columns-spanned="4"><fo:block><#-- blank line --></fo:block></fo:table-cell>
                </fo:table-row>

                <#-- the grand total -->
                <fo:table-row height="14px">
                    <fo:table-cell>
                        <fo:block><#-- blank column --></fo:block>
                    </fo:table-cell>
                    <fo:table-cell>
                        <fo:block text-align="" font-weight="bold">${uiLabelMap.FinancialsStatementsEndingBalance}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell number-columns-spanned="2">
                        <fo:block font-weight="bold" text-align="right"> <@ofbizCurrency amount=endingBalance isoCode=orgCurrencyUomId/></fo:block>
                    </fo:table-cell>
                </fo:table-row>
            </fo:table-body>        
         </fo:table>

         <fo:block></fo:block>
         <fo:block id="theEnd"/>  <#-- marks the end of the pages and used to identify page-number at the end -->
       </fo:flow>
    </fo:page-sequence>
</fo:root>
</#escape>
