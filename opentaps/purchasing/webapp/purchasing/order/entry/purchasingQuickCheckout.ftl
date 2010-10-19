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

<#-- page to select shipment address, orderterm -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
 <script type="text/javascript">
  function updateAgreement(oldValue) {
    var newValue = document.getElementById('newAgreementId').value;
     if (confirm('${StringUtil.wrapString(uiLabelMap.PurchOrderConfirmChangeAgreement)}')) {
      document.orderOptionForm.agreementId.value = newValue;
      document.orderOptionForm.optionType.value = 'updateAgreement';
      document.orderOptionForm.submit();
     } else {
        //do not change value
        document.getElementById('agreementId').value = oldValue;
     }
  }

  function changeTermType() {
    document.getElementById('newTermValue').value = "";
    document.getElementById('newTermDays').value = "";
    document.getElementById('newTextValue').value = "";
  }

  function updateOrderTerm(termTypeId, indexOfLine) {
    var termValue = document.getElementById('termValue_o_' + indexOfLine).value;
    var termDays = document.getElementById('termDays_o_' + indexOfLine).value;
    var textValue = document.getElementById('textValue_o_' + indexOfLine).value;
    document.orderOptionForm.termTypeId.value = termTypeId;
    document.orderOptionForm.termValue.value = termValue;
    document.orderOptionForm.termDays.value = termDays;
    document.orderOptionForm.textValue.value = textValue;
    document.orderOptionForm.optionType.value = 'updateTerm';
    document.orderOptionForm.submit();

  }

  function removeOrderTerm(termTypeId) {
    document.orderOptionForm.optionType.value = 'removeTerm';
    document.orderOptionForm.termTypeId.value = termTypeId;
    document.orderOptionForm.submit();
  }

  function addOrderTerm() {
   var termTypeId = document.getElementById('newTermTypeId').value;
    var termValue = document.getElementById('newTermValue').value;
    var termDays = document.getElementById('newTermDays').value;
    var textValue = document.getElementById('newTextValue').value;
    document.orderOptionForm.termTypeId.value = termTypeId;
    document.orderOptionForm.termValue.value = termValue;
    document.orderOptionForm.termDays.value = termDays;
    document.orderOptionForm.textValue.value = textValue;
    document.orderOptionForm.optionType.value = 'addTerm';
    document.orderOptionForm.submit();
  }

  function setShippingDestination() {
   document.facilityForm.submit();
  }

  function addNewPostalAddress() {
     var url = "<@ofbizUrl>ordersEditContactMech?partyId=${cart.billToCustomerPartyId}&contactMechPurposeTypeId=SHIPPING_LOCATION&preContactMechTypeId=POSTAL_ADDRESS&DONE_PAGE=purchasingQuickCheckout&forCart=true&onlyForOrder=Y</@ofbizUrl>";
     location.href= url;
  }

  </script>
  <form method="post" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>" name="orderOptionForm">
     <input type="hidden" id="agreementId" name="agreementId" />
     <input type="hidden" id="termTypeId" name="termTypeId"/>
     <input type="hidden" id="termValue" name="termValue"/>
     <input type="hidden" id="termDays" name="termDays"/>
     <input type="hidden" id="textValue" name="textValue"/>
     <input type="hidden" id="optionType" name="optionType"/>
     <input type="hidden" name="finalizeMode" value="init"/>
     <input type="hidden" name="finalizeReqTermInfo" value="true"/>
  </form>

  <#assign extraOptions>
    <a class="subMenuButton" href="<@ofbizUrl>createOrderMainScreen</@ofbizUrl>">${uiLabelMap.OpentapsOrderReturnToOrder}</a>
    <a id="continueLink" class="subMenuButton" href="javascript:setShippingDestination();">${uiLabelMap.PurchOrderReviewOrder}</a>
  </#assign>

  <@frameSectionTitleBar title=uiLabelMap.PurchOrderOptionAndShipToSettings titleClass="sectionHeaderTitle" titleId="sectionHeaderTitle_order" extra=extraOptions />

  <@frameSectionHeader title=uiLabelMap.PurchOrderTerms/>
  <div class="form">
    <table class="listTable">
      <tr class="listTableHeader">
        <td>${uiLabelMap.OrderOrderTermType}</td>
        <td>${uiLabelMap.OrderOrderTermValue}</td>
        <td>${uiLabelMap.OrderOrderTermDays}</td>
        <td>${uiLabelMap.CommonDescription}</td>
        <td> </td>
        <td> </td>
      </tr>

      <#assign orderTermCount = 0 />
      <#list orderTerms as orderTerm>
        <#if orderTerm.termTypeId?has_content>
        <#assign termType = cart.getShippingContactMechId()?default("") />
        <tr class="${tableRowClass(orderTerm_index)}">
          <@displayCell text=orderTerm.getRelatedOneCache("TermType").get("description",locale) />
          <@inputTextCell name="termValue" size="5" default=orderTerm.termValue maxlength="60" index=orderTerm_index  />
          <@inputTextCell name="termDays" size="5" default=orderTerm.termDays maxlength="60" index=orderTerm_index />
          <@inputTextCell name="textValue" size="20" default=orderTerm.textValue maxlength="200" index=orderTerm_index />
          <@inputButtonCell title="${uiLabelMap.CommonUpdate}" onClick="javascript:updateOrderTerm('${orderTerm.termTypeId}', ${orderTerm_index})"/>
          <@inputButtonCell title="${uiLabelMap.CommonRemove}" onClick="javascript:removeOrderTerm('${orderTerm.termTypeId}')"/>
          <#assign orderTermCount = orderTermCount + 1 />
        </tr>
        </#if> 
      </#list>
      <@inputHiddenUseRowSubmit />
      <@inputHiddenRowCount list=orderTerms />
      <tr class="${tableRowClass(orderTermCount)}">
        <td>
          <select id="newTermTypeId" name="newTermTypeId" class="selectBox" onchange="javascript:changeTermType()">
            <#if termTypes?has_content>
              <option value=""/>
              <#list termTypes as termType>
                <option value="${termType.termTypeId}" <#if termTypeId?default("")==termType.termTypeId> selected</#if>>${termType.get("description",locale)}</option>
              </#list>
            </#if>
          </select>
        </td>
        <td>
          <input type="text" class="inputBox" size="5" maxlength="60" name="newTermValue" id="newTermValue"/>
        </td>
        <td>
          <input type="text" class="inputBox" size="5" maxlength="60" name="newTermDays" id="newTermDays"/>
        </td>
        <td>
          <input type="text" class="inputBox" size="20" maxlength="200" name="newTextValue" id="newTextValue"/>
        </td>
        <td colspan=2><@inputButton title="${uiLabelMap.CommonAdd}" onClick="javascript:addOrderTerm()"/></td>
      </tr>
    </table>

    <br/>

    <#assign agreementId = cart.getAgreementId()?default("") />
    <table width="100%" border="0" cellpadding="1" cellspacing="0">
      <tr>
        <td width="35%">
          <div class="tabletext">${uiLabelMap.PurchOrderSetOrderTermsFromAgreement} :  </div>
        </td>
        <td>
          <div class="tabletext" valign='top'>
            <select id="newAgreementId" class="selectBox">
              <#if agreements?has_content>
                <option value=""/>
                <#list agreements as agreement>
                  <option value="${agreement.agreementId}" <#if agreement.agreementId?default("")==agreementId> selected</#if>>${agreement.get("description",locale)}</option>
                </#list>
              </#if>
            </select>
            <input type="button" value="${uiLabelMap.CommonSet}" class="smallSubmit" onclick="javascript:updateAgreement('${agreementId}')"  />
          </div>
        </td>
      </tr>
    </table>
  </div>

  <br/>

  <@frameSection title=uiLabelMap.PurchShipToSettings>
    <table width="100%" border="0" cellpadding="1" cellspacing="0">
      <form method="post" name="facilityForm" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>">
        <input type="hidden" name="finalizeMode" value="init"/>
        <input type="hidden" name="finalizeReqTermInfo" value="false"/>
        <input type="hidden" name="optionType" value="ShippingOption"/>
        <input type="hidden" name="maySplit" value="false"/>
        <input type="hidden" name="giftMessage" value=""/>
        <input type="hidden" name="isGift" value="false"/>
        <input type="hidden" name="shippingMethod" value="NO_SHIPPING"/>
        <input type="hidden" name="carrierPartyId" value="_NA_"/>
        <tr>
          <td width="18%">
            <div class="tableheadtext">${uiLabelMap.PurchOrderShipTo} : </div>
          </td>
          <td>
            <select name="contactMechId" class="selectBox">
              <#assign shipGroupContactMechId = cart.getShippingContactMechId()?default("") />
              <#list facilityMaps as facilityMap>
                <#assign facility = facilityMap.facility?if_exists/>
                <#assign facilityContactMechList = facilityMap.facilityContactMechList/>
                <#if facilityContactMechList?has_content>
                  <#list facilityContactMechList as facilityContactMech>
                    <#if facilityContactMech.postalAddress?exists>
                      <#assign address = facilityContactMech.postalAddress/>
                      <#assign contactMech = facilityContactMech.contactMech/>
                      <option value="${contactMech.contactMechId}"  <#if contactMech.contactMechId==shipGroupContactMechId> selected</#if>>
                        <#if facility?has_content>${facility.facilityName?if_exists}<#else><#if usingSingleAddress>${uiLabelMap.PurchOrderSingleUseAddress}<#else>${address.toName?if_exists}</#if></#if> ${uiLabelMap.CommonAt} ${address.address1} - ${address.city?if_exists} ${address.countryGeoId?if_exists}
                      </option>
                    </#if>
                  </#list>
                </#if>
              </#list>
              <option value="_NA_">${uiLabelMap.PurchNoShippingAddress}</option>
            </select>
          </td>
        </tr>
        <tr>
          <td>
          </td>
          <td>
            <a class="buttontext" href="javascript:addNewPostalAddress()">${uiLabelMap.PurchOrderOtherAddress}</a>
          </td>
        </tr>
        <tr><td colspan="2">&nbsp;</td></tr>
        <tr>
          <td>
            <div class="tableheadtext">${uiLabelMap.PurchOrderShippingInstructions} : </div>
          </td>
          <td>
            <div class="tabletext" style="clear:both">
              <textarea name="shippingInstructions" rows="2" cols="50">${cart.getShippingInstructions()?if_exists}</textarea>
            </div>
          </td>
        </tr>
      </form>
    </table>
  </@frameSection>
