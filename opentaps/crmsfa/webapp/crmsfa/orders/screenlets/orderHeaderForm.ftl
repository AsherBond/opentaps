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

<script type="text/javascript">
    function showExtraControls() {
        var partyIdElement = document.getElementById('updateOrderHeaderInfoPartyId');
        if (${(shoppingCart.size() == 0)?string} || partyIdElement.value == '' || '${shoppingCart.getPartyId()?if_exists}' == partyIdElement.value) {
            partyIdElement.form.submit();
        } else {
            var mainSubmit = document.getElementById('mainSubmit');
            mainSubmit.className = 'hidden';
            opentaps.expandCollapse('recalcPrices');
        }
    }
</script>

<#if shoppingCart?has_content>

  <@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

  <form name="updateOrderHeaderInfo" action="<@ofbizUrl>updateOrderHeaderInfo</@ofbizUrl>" method="POST">

    <@frameSection title=uiLabelMap.OpentapsOrderSettings>
      <div id="orderEntryHeaderForm">

        <div class="orderEntryHeaderFormRow">
          <span class="tableheadtext orderEntryHeaderFormLabel">
            ${uiLabelMap.CrmStore}:
          </span>
          <#if shoppingCart.size() == 0>
            <span class="orderEntryHeaderFormInput">
              <select name="productStoreId" class="selectBox">
                <#list productStores as store>
                  <#if store.productStoreId == productStore.productStoreId><#assign selected="selected"/><#else><#assign selected = ""/></#if>
                  <option ${selected} value="${store.productStoreId}">${store.storeName}</option>
                </#list>
              </select>
            </span>
          <#else>
            <span class="tabletext orderEntryHeaderFormInput"> ${productStore.storeName?if_exists}</span>
          </#if>
        </div>

        <div class="orderEntryHeaderFormRow">
          <span class="tableheadtext orderEntryHeaderFormLabel">
            ${uiLabelMap.CrmSalesChannel}:
          </span>
          <span class="orderEntryHeaderFormInput">
            <select name="salesChannelEnumId" class="selectBox">
              <option></option>
              <#list salesChannels as channel>
                <#if shoppingCart.getChannelType()?default("") == channel.enumId><#assign selected="selected"/><#else><#assign selected=""/></#if>
                <option ${selected} value="${channel.enumId}">${channel.description}</option>
              </#list>
            </select>
          </span>
        </div>

        <div class="orderEntryHeaderFormRow">
          <span class="tableheadtext orderEntryHeaderFormLabel">
            ${uiLabelMap.OrderOrderName}:
          </span>
          <span class="orderEntryHeaderFormInput">
            <input class="inputBox" type="text" name="orderName" value="${shoppingCart.getOrderName()?default("")}" maxlength="200"></input>
          </span>
        </div>

        <div class="orderEntryHeaderFormRow">
          <span class="requiredField orderEntryHeaderFormLabel">
            ${uiLabelMap.ProductCustomer}:
          </span>
          <@inputAutoCompleteClient name="partyId" id="updateOrderHeaderInfoPartyId" size=13 />
          <#if partyReader?exists && shoppingCart.getPartyId()?exists>
            <#-- show a URL for viewaccount/viewcontact based on type of party -->
            <span class="tabletext orderEntryHeaderFormInput">
              <a href="${Static["com.opensourcestrategies.crmsfa.party.PartyHelper"].createViewPageURL(shoppingCart.getPartyId(), delegator, externalLoginKey)}" class="linktext">${partyReader.getPartyName()}</a>
            </span>
          </#if>
        </div>

        <#if partyReader?exists && partyReader.hasClassification("DONOTSHIP_CUSTOMERS")>
          <script type="text/javascript">
                window.onload = function() {
                  alert('${uiLabelMap.CrmErrorPartyCannotOrder}');
                }
          </script>
        </#if>

        <#if orderPartySupplementalData?exists && orderPartySupplementalData.importantNote?has_content>
          <div class="orderEntryHeaderFormRow" style="text-align:left">
            <div>
              <span class="tableheadtext">
                ${uiLabelMap.CrmImportantNote}:
              </span>
            </div>
            <p class="requiredField" style="padding-left: 10px; padding-right:14px">
              ${orderPartySupplementalData.importantNote?default("")}
            </p>
          </div>
        </#if>

        <div class="orderEntryHeaderFormRow">
          <span class="tableheadtext orderEntryHeaderFormLabel">
            ${uiLabelMap.OpentapsPONumber}:
          </span>
          <span class="orderEntryHeaderFormInput">
            <input class="inputBox" type="text" name="poNumber" value="${shoppingCart.getPoNumber()?default("")}" maxlength="20"></input>
          </span>
        </div>

        <#if trackingCodes.size() != 0>
          <div class="orderEntryHeaderFormRow">
            <span class="tableheadtext orderEntryHeaderFormLabel">
              ${uiLabelMap.CrmTrackingCode}:
            </span>
            <#assign defaultCode = session.getAttribute("trackingCodeId")?default("")/>
            <span class="orderEntryHeaderFormInput">
              <select name="trackingCodeId" class="selectBox">
                <option value=""></option>
                <#list trackingCodes as trackingCode>
                  <#if defaultCode == trackingCode.trackingCodeId><#assign selected="selected"/><#else><#assign selected = ""/></#if>
                  <option ${selected} value="${trackingCode.trackingCodeId}">${trackingCode.trackingCodeId} ${trackingCode.description?if_exists}</option>
                </#list>
              </select>
            </span>
          </div>
        </#if>

        <#assign salesReps = shoppingCart.getAdditionalPartyRoleMap().get("COMMISSION_AGENT")?default([])/>
        <div class="orderEntryHeaderFormRow">
          <span class="tableheadtext orderEntryHeaderFormLabel">${uiLabelMap.CrmCommissionRep}:</span>
          <span class="tabletext orderEntryHeaderFormInput">
            <input type="text" size="10" maxlength="20" name="salesRepPartyId" id="salesRepPartyId" value="${(salesReps?first)!}" class="inputBox" style="width: 90px;"/>
            <a href="javascript:call_fieldlookup2(document.updateOrderHeaderInfo.salesRepPartyId,'LookupTeamMembers');"><img src="/images/fieldlookup.gif" alt="Lookup" border="0" height="14" width="15"></a>
            <#if errorField?has_content><@displayError name=errorField index=index /></#if>
          </span>
          <#--list salesReps as salesRep><span class="tabletext orderEntryHeaderFormInput">${salesRep}</span><br/></#list-->
        </div>

        <div class="orderEntryHeaderSubmitRow">
          <a class="subMenuButton" id="mainSubmit" href="javascript:showExtraControls();">${uiLabelMap.CommonUpdate}</a>
          &nbsp;
          <@inputConfirm title=uiLabelMap.OpentapsCancelOrder form="cancelOrderForm"/>
        </div>

        <div class="orderEntryHeaderSubmitRow">
          <@flexArea title="${uiLabelMap.CrmOrderUpdatePricesForNewCustomer}" targetId="recalcPrices" style="border:none; padding:0; margin-right:0" controlClassOpen="tableheadtext" controlClassClosed="hidden" state="closed" enabled=false>
            <a class="subMenuButton" href="javascript:document.forms['updateOrderHeaderInfo'].action='<@ofbizUrl>updateOrderHeaderInfoAndRecalculatePrices</@ofbizUrl>';document.forms['updateOrderHeaderInfo'].submit();">${uiLabelMap.CommonYes}</a>
            &nbsp;
            <a class="subMenuButton" href="javascript:document.forms['updateOrderHeaderInfo'].submit();">${uiLabelMap.CommonNo}</a>
          </@flexArea>
        </div>

      </div>
    </@frameSection>
  </form>

<form name="cancelOrderForm" method="POST" action="destroyCart">
</form>

</#if>
