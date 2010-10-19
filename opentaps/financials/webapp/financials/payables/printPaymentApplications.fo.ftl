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

<#-- Note: this was originally part of PrintChecks.fo.ftl in Apache OFBiz, but now it is being refactored as a macro -->
<#-- This file has been modified by Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.fo.ftl"/>

<#escape x as x?xml>
<#macro printPaymentApplications payment paymentApplications height>
<#-- this seems to be the only way to force a fixed height in fop -->
<fo:table table-layout="fixed" margin-left="5pt" margin-right="5pt">
  <fo:table-column column-width="100%"/>
  <fo:table-body>
    <fo:table-row height="${height}">
      <fo:table-cell>

        <fo:table height="${height}" table-layout="fixed" margin-left="8pt" margin-right="8pt">
          <fo:table-column/>
          <fo:table-column/>
          <fo:table-column/>
          <fo:table-column/>
          <fo:table-header>
            <fo:table-row>
              <fo:table-cell padding="3pt" number-columns-spanned="2" text-align="center">
                <fo:block text-align="center">
                <#assign address = Static["com.opensourcestrategies.financials.util.UtilFinancial"].getBillingAddress(payment.partyIdTo, delegator)?if_exists>
                <#if address?has_content && address.toName?has_content>
                   <#assign toName = address.toName>
                <#else>
                   <#assign toName = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, payment.partyIdTo, false)>
                </#if>
                ${toName?default("")}
                </fo:block>
              </fo:table-cell>
              <fo:table-cell padding="3pt" number-columns-spanned="2" text-align="center">
                <fo:block text-align="center"><@displayDateFO date=payment.effectiveDate/></fo:block>
              </fo:table-cell>
            </fo:table-row>
            <fo:table-row>
              <fo:table-cell padding="3pt">
                <fo:block font-weight="bold">${uiLabelMap.CommonDate}</fo:block>
              </fo:table-cell>
              <fo:table-cell padding="3pt" number-columns-spanned="2">
                <fo:block font-weight="bold">${uiLabelMap.OpentapsReference}</fo:block>
              </fo:table-cell>
              <fo:table-cell padding="3pt">
                <fo:block font-weight="bold" text-align="right">${uiLabelMap.AccountingAmount}</fo:block>
              </fo:table-cell>
            </fo:table-row>
          </fo:table-header>
          <fo:table-body>

            <#list paymentApplications as paymentApplication>
            <#assign invoice = paymentApplication.getRelatedOne("Invoice")?if_exists>
            <#if invoice?has_content>
            <#assign terms = invoice.getRelatedByAnd("InvoiceTerm", Static["org.ofbiz.base.util.UtilMisc"].toMap("termTypeId", "PURCH_VENDOR_ID"))>
            <#if terms.size() != 0>
            <#assign term = terms?first>
            <#assign vendorCustomerId = term.description?if_exists>
          </#if>
        </#if>
        <fo:table-row>
          <fo:table-cell padding="3pt">
            <fo:block><@displayDateFO date=payment.effectiveDate/></fo:block>
          </fo:table-cell>
          <fo:table-cell padding="3pt" number-columns-spanned="2">
            <fo:block>
            <#if invoice?exists>
              ${invoice.referenceNumber?if_exists}
              ${vendorCustomerId?if_exists}
            </#if>
            ${paymentApplication.note?if_exists}
            ${paymentApplication.taxAuthGeoId?if_exists}
          </fo:block>
        </fo:table-cell>
      <fo:table-cell padding="3pt">
        <fo:block text-align="end">${paymentApplication.getBigDecimal("amountApplied").setScale(2, rounding).toString()}</fo:block>
      </fo:table-cell>
    </fo:table-row>
  </#list>

  <fo:table-row>
    <fo:table-cell padding="3pt" number-columns-spanned="3">
      <fo:block text-align="end">${uiLabelMap.FinancialsTotalAmount}</fo:block>
    </fo:table-cell>
    <fo:table-cell padding="3pt">
      <fo:block text-align="end">${payment.getBigDecimal("amount").setScale(2, rounding).toString()}</fo:block>
    </fo:table-cell>
  </fo:table-row>
</fo:table-body>
</fo:table>

</fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
</#macro>
</#escape>


