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

<#-- Shows the cart items in the main create order screen. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl" />
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsSurveyMacros.ftl" />

  <#if shoppingCartSize gt 0>
    <form method="post" action="<@ofbizUrl>modifycart</@ofbizUrl>" name="cartform" style="margin: 0;">
      <input type="hidden" name="removeSelected" value="true"/>
      <table cellspacing="2" cellpadding="1" border="0">
        <tr>
          <td>&nbsp;</td>
          <td colspan="2">
            <div class="tabletext">
              <b>${uiLabelMap.ProductProduct}</b>
              <#-- TODO: Figure out where this is set ... nowhere in OFBIZ apparently -->
              <#if showOrderGiftWrap?default("false") == "true">
                  <select class="selectBox" name="GWALL" onchange="javascript:gwAll(this);">
                    <option value="">${uiLabelMap.OrderGiftWrapAllItems}</option>
                    <option value="NO^">${uiLabelMap.OrderNoGiftWrap}</option>
                    <#if allgiftWraps?has_content>
                      <#list allgiftWraps as option>
                        <option value="${option.productFeatureId?default("")}">${option.description?default("")} : <@ofbizCurrency amount=option.defaultAmount?default(0) isoCode=currencyUomId rounding=2/></option>
                      </#list>
                    </#if>
                  </select>
              </#if>
            </div>
          </td>
          <td align="center"><div class="tabletext"><b>${uiLabelMap.OrderQuantity}</b></div></td>
          <td align="right"><div class="tabletext"><b>${uiLabelMap.CommonUnitPrice}</b></div></td>
          <td align="right"><div class="tabletext"><b>${uiLabelMap.OrderAdjustments}</b></div></td>
          <td align="right"><div class="tabletext"><b>${uiLabelMap.OrderItemTotal}</b></div></td>
          <td align="right"><div class="tabletext"><b>${uiLabelMap.CommonRemove}?</b></div></td>
        </tr>

        <#assign itemsFromList = false/>
        <#list shoppingCart.items() as cartLine>
          <#assign cartLineIndex = shoppingCart.getItemIndex(cartLine)/>
          <#assign lineOptionalFeatures = cartLine.getOptionalProductFeatures()/>
          <tr><td colspan="8"><hr class="sepbar"></td></tr>
          <tr valign="top">
            <td>&nbsp;</td>
            <td>
              <table border="0">
                <tr>
                  <td colspan="2">
                    <div class="tabletext">
                      <#if cartLine.getProductId()?exists>
                        <#-- product item -->
                        <a href="<@ofbizUrl>product?product_id=${cartLine.getProductId()}</@ofbizUrl>" class="linktext">${cartLine.getProductId()}</a>:
                        <input size="50" class="inputBox" type="text" name="description_${cartLineIndex}" value="${cartLine.getName()?default("")?string}" <#if cartLine.getIsPromo()>readonly="readonly"</#if>/>
                      <#else>
                        <#-- this is a non-product item -->
                        <b>${cartLine.getItemTypeDescription()?if_exists}</b> : ${cartLine.getName()?if_exists}
                      </#if>
                      <#-- display the item's features -->
                      <#assign features = []/>
                      <#assign warnings = []/>
                      <#assign nonWarningFeatures = []/>
                      <#if shoppingCart.getPartyId()?has_content && cartLine.getFeaturesForSupplier(dispatcher,shoppingCart.getPartyId())?has_content>
                        <#assign features = cartLine.getFeaturesForSupplier(dispatcher, shoppingCart.getPartyId())/>
                      <#elseif cartLine.getStandardFeatureList()?has_content>
                        <#assign features = cartLine.getStandardFeatureList()/>
                      </#if>

                      <#-- separate warning from nonwarning features -->
                      <#if features?has_content>
                        <#list features as feature>
                          <#if feature.productFeatureTypeId == "WARNING">
                            <#assign warnings = warnings + [feature.description]/>
                          <#else>
                            <#assign nonWarningFeatures = nonWarningFeatures + [feature.description]/>
                          </#if>
                        </#list>
                      </#if>

                      <#if nonWarningFeatures?has_content>
                        <br/><i>${uiLabelMap.ProductFeatures}: <#list nonWarningFeatures as feature>${feature?default("")} </#list></i>
                      </#if>
                      <#-- print the survey based on a macro defined externally (see top of file @imports) -->
                      <#if cartLine.getAttribute("surveyResponses")?has_content>
                        <div style="margin-left: 10px; margin-bottom: 1em">
                          <#list cartLine.getAttribute("surveyResponses") as surveyResponseId>
                            <@displaySurveyResponse surveyResponseId=surveyResponseId/>
                          </#list>
                        </div>
                      </#if>
                    </div>
                  </td>
                </tr>

                <#if cartLine.getRequirementId()?has_content>
                  <tr>
                    <td colspan="2" align="left">
                      <div class="tabletext"><b>${uiLabelMap.OrderRequirementId}</b>: ${cartLine.getRequirementId()?if_exists}</div>
                    </td>
                  </tr>
                </#if>
                <#if cartLine.getQuoteId()?has_content>
                  <#if cartLine.getQuoteItemSeqId()?has_content>
                    <tr>
                      <td colspan="2" align="left">
                        <div class="tabletext"><b>${uiLabelMap.OrderOrderQuoteId}</b>: ${cartLine.getQuoteId()?if_exists} - ${cartLine.getQuoteItemSeqId()?if_exists}</div>
                      </td>
                    </tr>
                  </#if>
                </#if>
                <#if cartLine.getItemComment()?has_content>
                  <tr>
                    <td align="left"><div class="tableheadtext">${uiLabelMap.CommonComment} : </div></td>
                    <td align="left"><div class="tabletext">${cartLine.getItemComment()?if_exists}</div></td>
                  </tr>
                </#if>
                <#if cartLine.getDesiredDeliveryDate()?has_content>
                  <tr>
                    <td align="left"><div class="tableheadtext">${uiLabelMap.OrderDesiredDeliveryDate}: </div></td>
                    <td align="left"><div class="tabletext">${getLocalizedDate(cartLine.getDesiredDeliveryDate)}</div></td>
                  </tr>
                </#if>
                <#-- inventory summary -->
                <#if cartLine.getProductId()?exists>
                  <#assign productId = cartLine.getProductId()/>
                  <#assign product = cartLine.getProduct()/>
                  <#assign isPhysical = Static["org.opentaps.common.product.UtilProduct"].isPhysical(product)/>
                  <#if isPhysical>
                    <tr>
                      <td colspan="2" align="left">
                        <div class="tabletext">
                          <b>${uiLabelMap.ProductInventory}</b>:
                          ${uiLabelMap.ProductAtp} = ${availableToPromiseMap.get(productId)}, ${uiLabelMap.ProductQoh} = ${quantityOnHandMap.get(productId)}
                          <#if product.productTypeId == "MARKETING_PKG_AUTO">
                            ${uiLabelMap.ProductMarketingPackageATP} = ${mktgPkgATPMap.get(productId)}, ${uiLabelMap.ProductMarketingPackageQOH} = ${mktgPkgQOHMap.get(productId)}
                          </#if>
                        </div>
                      </td>
                    </tr>
                  </#if>
                </#if>

                <#-- ship before/after date -->
                <#if !cartLine.getIsPromo()>
                <tr>
                  <td colspan="2">
                    <table border="0" cellpadding="0" cellspacing="0" width="100%">
                      <tr>
                        <td align="left">
                          <div class="tabletext">${uiLabelMap.OrderShipAfterDate}
                            <@inputDate name="shipAfterDate_${cartLineIndex}" default=cartLine.getShipAfterDate()/>
                          </div>
                        </td>
                        <td>&nbsp;</td>
                        <td align="left">
                          <div class="tabletext">${uiLabelMap.OrderShipBeforeDate}
                            <@inputDate name="shipBeforeDate_${cartLineIndex}" default=cartLine.getShipBeforeDate()/>
                          </div>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
                </#if>

                <#if !cartLine.getIsPromo()>
                <#-- accounting tags -->
                <tr>
                  <td colspan="2">
                    <table border="0" cellpadding="0" cellspacing="0" width="100%">
                      <#if tagTypes?has_content>
                        <@accountingTagsSelectRowsForCart tags=tagTypes item=cartLine index=cartLineIndex />
                      </#if>
                    </table>
                  </td>
                </tr>
                </#if>
              </table>

              <#if (cartLine.getIsPromo() && cartLine.getAlternativeOptionProductIds()?has_content)>
                <#-- Show alternate gifts if there are any... -->
                <div class="tableheadtext">${uiLabelMap.OrderChooseFollowingForGift}:</div>
                <#list cartLine.getAlternativeOptionProductIds() as alternativeOptionProductId>
                  <#assign alternativeOptionProduct = delegator.findByPrimaryKeyCache("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId", alternativeOptionProductId))/>
                  <#assign alternativeOptionName = Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(alternativeOptionProduct, "PRODUCT_NAME", locale, dispatcher)?if_exists/>
                  <div class="tabletext"><a href="<@ofbizUrl>setDesiredAlternateGwpProductId?alternateGwpProductId=${alternativeOptionProductId}&alternateGwpLine=${cartLineIndex}</@ofbizUrl>" class="linktext">Select: ${alternativeOptionName?default(alternativeOptionProductId)}</a></div>
                </#list>
              </#if>
            </td>

            <#-- gift wrap option -->
            <#assign showNoGiftWrapOptions = false>
            <td nowrap="nowrap" align="right">
              <#assign giftWrapOption = lineOptionalFeatures.GIFT_WRAP?if_exists>
              <#assign selectedOption = cartLine.getAdditionalProductFeatureAndAppl("GIFT_WRAP")?if_exists>
              <#if giftWrapOption?has_content>
                <select class="selectBox" name="option^GIFT_WRAP_${cartLineIndex}" onchange="javascript:document.cartform.submit()">
                  <option value="NO^">${uiLabelMap.OrderNoGiftWrap}</option>
                  <#list giftWrapOption as option>
                    <option value="${option.productFeatureId}" <#if ((selectedOption.productFeatureId)?exists && selectedOption.productFeatureId == option.productFeatureId)>SELECTED</#if>>${option.description} : <@ofbizCurrency amount=option.amount?default(0) isoCode=currencyUomId rounding=2/></option>
                  </#list>
                </select>
              <#elseif showNoGiftWrapOptions>
                <select class="selectBox" name="option^GIFT_WRAP_${cartLineIndex}" onchange="javascript:document.cartform.submit()">
                  <option value="">${uiLabelMap.OrderNoGiftWrap}</option>
                </select>
              <#else>
                &nbsp;
              </#if>
            </td>
            <#-- end gift wrap option -->
            <td nowrap="nowrap" align="center">
              <div class="tabletext">
                <#if cartLine.getIsPromo() || cartLine.getShoppingListId()?exists>
                    ${cartLine.getQuantity()?string.number}
                <#else>
                    <input size="6" class="inputBox" type="text" name="update_${cartLineIndex}" value="${cartLine.getQuantity()?string.number}"/>
                </#if>
              </div>
            </td>
            <td nowrap="nowrap" align="right">
              <div class="tabletext">
                <#if cartLine.getIsPromo() || (shoppingCart.getOrderType() == "SALES_ORDER" && !security.hasEntityPermission("ORDERMGR", "_SALES_PRICEMOD", session))>
                  <@ofbizCurrency amount=cartLine.getDisplayPrice() isoCode=currencyUomId/>
                <#else>
                  <input size="6" class="inputBox" type="text" name="price_${cartLineIndex}" value="${cartLine.getBasePrice()}"/>
                </#if>
              </div>
            </td>
            <td nowrap="nowrap" align="right"><div class="tabletext"><@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=currencyUomId rounding=2/></div></td>
            <td nowrap="nowrap" align="right"><div class="tabletext"><@ofbizCurrency amount=cartLine.getDisplayItemSubTotal() isoCode=currencyUomId rounding=2/></div></td>
            <td nowrap="nowrap" align="center"><div class="tabletext"><#if !cartLine.getIsPromo()><input type="checkbox" name="selectedItem" value="${cartLineIndex}" onclick="javascript:checkToggle(this);"><#else>&nbsp;</#if></div></td>
          </tr>

        <#-- display product warnings -->
        <#if warnings?has_content>
            <#list warnings as warning>
                <tr><td class="productWarning" colspan="8">${uiLabelMap.CrmProductWarning} : ${warning?if_exists}</td></tr>
            </#list>
        </#if>

        </#list>

        <#if shoppingCart.getAdjustments()?has_content>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
              <tr>
                <td colspan="4" nowrap="nowrap" align="right"><div class="tabletext">${uiLabelMap.OrderSubTotal}:</div></td>
                <td nowrap="nowrap" align="right"><div class="tabletext"><@ofbizCurrency amount=shoppingCart.getSubTotal() isoCode=currencyUomId rounding=2/></div></td>
                <td>&nbsp;</td>
              </tr>
            <#list shoppingCart.getAdjustments() as cartAdjustment>
              <#assign adjustmentType = cartAdjustment.getRelatedOneCache("OrderAdjustmentType")>
              <tr>
                <td colspan="4" nowrap="nowrap" align="right">
                  <div class="tabletext">
                    <i>${uiLabelMap.OrderAdjustment}</i> - ${adjustmentType.get("description",locale)?if_exists}
                    <#if cartAdjustment.productPromoId?has_content><a href="<@ofbizUrl>showPromotionDetails?productPromoId=${cartAdjustment.productPromoId}</@ofbizUrl>" class="linktext">${uiLabelMap.CommonDetails}</a></#if>:
                  </div>
                </td>
                <td nowrap="nowrap" align="right"><div class="tabletext"><@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(cartAdjustment, shoppingCart.getSubTotal()) isoCode=currencyUomId rounding=2/></div></td>
                <td>&nbsp;</td>
              </tr>

            </#list>
        </#if>

        <tr>
          <td colspan="6" align="right" valign="bottom">
            <div class="tabletext"><b>${uiLabelMap.OrderCartTotal}:</b></div>
          </td>
          <td align="right" valign="bottom">
            <hr class="sepbar"/>
            <div class="tabletext"><b><@ofbizCurrency amount=shoppingCart.getGrandTotal() isoCode=currencyUomId rounding=2/></b></div>
          </td>
        </tr>
        <tr>
          <td colspan="8">&nbsp;</td>
        </tr>
      </table>
    </form>
  <#else>
    <div class="tabletext">${uiLabelMap.OrderNoOrderItemsToDisplay}</div>
  </#if>
