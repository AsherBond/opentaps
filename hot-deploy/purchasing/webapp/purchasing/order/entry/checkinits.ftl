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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#-- This file has been modified by Open Source Strategies, Inc. -->

<#assign shoppingCartOrderType = "">
<#assign shoppingCartProductStore = "NA">
<#assign shoppingCartChannelType = "">
<#if shoppingCart?exists>
  <#assign shoppingCartOrderType = shoppingCart.getOrderType()>
  <#assign shoppingCartProductStore = shoppingCart.getProductStoreId()?default("NA")>
  <#assign shoppingCartChannelType = shoppingCart.getChannelType()?default("")>
<#else>
<#-- allow the order type to be set in parameter, so only the appropriate section (Sales or Purchase Order) shows up -->
  <#if parameters.orderTypeId?has_content>
    <#assign shoppingCartOrderType = parameters.orderTypeId>
  </#if>
</#if>

<!-- Purchase Order Entry -->
<#if security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session)>
  <#if shoppingCartOrderType != "SALES_ORDER">
<table width="100%" border="0" align="center" cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td>
      <table width="100%" border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead">
              ${uiLabelMap.OrderPurchaseOrder}<#if shoppingCart?exists>&nbsp;${uiLabelMap.OrderInProgress}</#if>
            </div>
          </td>
          <td valign="middle" align="right">
            <a href="/partymgr/control/findparty?externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.PartyFindParty}</a>
            <a href="javascript:document.poentryform.submit();" class="buttontext">${uiLabelMap.CommonContinue}</a>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td>
      <form method="post" name="poentryform" action="<@ofbizUrl>initorderentry</@ofbizUrl>">
      <input type='hidden' name='finalizeMode' value='type'/>
      <input type='hidden' name='orderMode' value='PURCHASE_ORDER'/>
      <table width="100%" border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <#if partyId?exists>
          <#assign thisPartyId = partyId>
        <#else>
          <#assign thisPartyId = requestParameters.partyId?if_exists>
        </#if>
        <input type="hidden" name="billToCustomerPartyId" value="${parameters.organizationPartyId}"/>
        <tr>
          <td>&nbsp;</td>
          <td align='right' valign='middle' nowrap><div class='tableheadtext'>${uiLabelMap.PartySupplier}</div></td>
          <td>&nbsp;</td>
          <td valign='middle'>
            <div class='tabletext' valign='top'>
              <@inputAutoCompleteSupplier name="supplierPartyId" />
            </div>
          </td>
        </tr>
      </table>
      </form>
    </td>
  </tr>
</table>
  </#if>
</#if>
