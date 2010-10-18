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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<form method="post" name="agreementForm" action="<@ofbizUrl>setOrderCurrencyAgreementShipDates</@ofbizUrl>">
<div class="screenlet">
  <div class="screenlet-header">
      <div class="boxtop">
          <div class="boxhead-right" align="right">
              <@inputSubmit title="${uiLabelMap.CommonContinue}"/>
          </div>
          <div class="boxhead-left">
              &nbsp;${uiLabelMap.OrderOrderEntryCurrencyAgreementShipDates}
          </div>
          <div class="boxhead-fill">&nbsp;</div>
      </div>
  </div>
  <div class="screenlet-body">
    <table>
      <tr><td colspan="4">&nbsp;</td></tr>

      <#if agreements?exists>
      <@inputHidden name='hasAgreements' value='Y'/>
      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='top' nowrap>
          <div class='tableheadtext'>
            ${uiLabelMap.OrderSelectAgreement}
          </div>
        </td>
        <td>&nbsp;</td>
        <td valign='middle'>
          <div class='tabletext' valign='top'>
            <@inputSelect name="agreementId" list=agreements key="agreementId" required=false ; option>
              ${option.agreementId} - ${option.description?if_exists}
            </@inputSelect>
          </div>
        </td>
      </tr>

      <#else><@inputHidden name='hasAgreements' value='N'/>
      </#if>

      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='middle' class='tableheadtext' nowrap>
           ${uiLabelMap.OrderOrderName}
        </td>
        <td>&nbsp;</td>
        <td align='left'>
          <@inputText size='60' maxlength='100' name='orderName'/>
        </td>
      </tr>
      
      <#if cart.getOrderType() != "PURCHASE_ORDER">
      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='middle' class='tableheadtext' nowrap>
          ${uiLabelMap.OrderPONumber}
        </td>
        <td>&nbsp;</td>
        <td align='left'>
          <@inputText name="correspondingPoId" size="15"/>
        </td>
      </tr>                                                           
      </#if>

      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='middle' nowrap>
          <div class='tableheadtext'>
            <#if agreements?exists>${uiLabelMap.OrderSelectCurrencyOr}
            <#else>${uiLabelMap.OrderSelectCurrency}
            </#if>
          </div>
        </td>
        <td>&nbsp;</td>
        <td valign='middle'>
          <div class='tabletext' valign='top'>
            <@inputCurrencySelect/>
          </div>
        </td>
      </tr>

      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='top' nowrap>
          <div class='tableheadtext'>
            ${uiLabelMap.OrderShipAfterDateDefault}
          </div>
        </td>
        <td>&nbsp;</td>
        <@inputDateCell name="shipAfterDate"/>
      </tr>

      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='top' nowrap>
          <div class='tableheadtext'>
            ${uiLabelMap.OrderShipBeforeDateDefault}
          </div>
        </td>
        <td>&nbsp;</td>
        <@inputDateCell name="shipBeforeDate"/>
        </td>
      </tr>
    </table>
  </div>
</div>
</form>
