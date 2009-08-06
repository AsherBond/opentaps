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


<#if shoppingCart.getOrderType() == "SALES_ORDER">
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.OrderPromotionCouponCodes}</div>
    </div>
    <div class="screenlet-body">
      <div class="tabletext">
        <form method="post" action="<@ofbizUrl>addpromocode</@ofbizUrl>" name="addpromocodeform" style="margin: 0;">
          <input type="text" class="inputBox" size="15" name="productPromoCodeId" value="">
          <input type="submit" class="smallSubmit" value="${uiLabelMap.OrderAddCode}">
          <#assign productPromoCodeIds = (shoppingCart.getProductPromoCodesEntered())?if_exists>
          <#if productPromoCodeIds?has_content>
            <div>
              ${uiLabelMap.OrderEnteredPromoCodes}:
              <#list productPromoCodeIds as productPromoCodeId>
                ${productPromoCodeId}
              </#list>
            </div>
          </#if>
        </form>
      </div>
    </div>
</div>
</#if>
