<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if order?has_content>
  <table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
    <tr>
      <td width="100%">
        <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
          <tr>
            <td valign="middle" align="left">
              <div class="boxhead">&nbsp;${uiLabelMap.OrderAddToOrder}</div>
            </td>
          </tr>
        </table>
      </td>
    </tr>
    <tr>
      <td width="100%">
        <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
          <tr>
            <td>
              <form method="post" action="<@ofbizUrl>appendItemToOrder?${paramString}</@ofbizUrl>" name="appendItemForm" style="margin: 0;">
                <@inputHidden name="correspondingPoId" value=order.primaryPoNumber?if_exists />
                <table border="0">
                  <tr>
                    <@displayTitleCell title=uiLabelMap.ProductProduct />
                    <@inputAutoCompleteProductCell name="productId" />
                    <#-- accounting tags -->
                    <td rowspan="6" valign="top">
                      <table border="0" cellpadding="0" cellspacing="0" width="100%">
                        <#if tagTypes?has_content>
                          <@accountingTagsSelectRows tags=tagTypes />
                        </#if>
                      </table>
                    </td>
                  </tr>
                  <#-- Catalog is only for sales order -->
                  <#if order.isSalesOrder() >
                    <#assign defaultProdCatalogId = Static["org.ofbiz.product.catalog.CatalogWorker"].getCurrentCatalogId(request)?default("")/>
                    <@inputSelectRow title=uiLabelMap.ProductCatalog name="prodCatalogId" list=catalogs key="prodCatalogId" displayField="catalogName" />
                  </#if>
                  <tr>
                    <@displayTitleCell title=uiLabelMap.OrderPrice />
                    <td>
                      <@inputText name="basePrice" size=6 />
                      <input type="checkbox" name="overridePrice" value="Y"/>
                      <@display text=uiLabelMap.OrderOverridePrice />
                    </td>
                  </tr>
                  <@inputTextRow title=uiLabelMap.OrderQuantity name="quantity" size=6 default="1" />
                  <@inputSelectRow title=uiLabelMap.OrderShipGroup name="shipGroupSeqId" list=order.shipGroups key="shipGroupSeqId" displayField="shipGroupSeqId" default="00001" />
                  <@inputTextareaRow title=uiLabelMap.CommonComments name="comments" rows="5" cols="40" />
                  <tr>
                    <td>&nbsp;</td>
                    <td>
                      <input type="checkbox" name="recalcOrder" value="Y"/>
                      <@display text=uiLabelMap.OpentapsOrderRecalcOrder />
                    </td>
                  </tr>
                  <tr>
                    <td colspan="2">&nbsp;</td>
                  </tr>
                  <@inputSubmitRow title=uiLabelMap.OrderAddToOrder />
                </table>
              </form>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</#if> <#-- order?has_content -->
