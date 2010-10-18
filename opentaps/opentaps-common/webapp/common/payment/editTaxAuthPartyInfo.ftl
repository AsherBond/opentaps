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
<!-- begin editpartytaxauthinfo.ftl -->
  <#if !taxAuthPartyInfo?exists>
      <form method="post" action="<@ofbizUrl>createPartyTaxAuthInfo?DONE_PAGE=${donePage}</@ofbizUrl>" name="editpartytaxauthinfo">
      <#assign submitLabel = uiLabelMap.CommonCreate />
  <#else>
      <form method="post" action="<@ofbizUrl>updatePartyTaxAuthInfo?DONE_PAGE=${donePage}</@ofbizUrl>" name="editpartytaxauthinfo">
      <#assign submitLabel = uiLabelMap.CommonSave />
  </#if>
  <@inputHidden name="partyId" value=partyId />
  <table class="twoColumnForm" style="border:0">
      <tr>
          <@displayTitleCell title=uiLabelMap.AccountingTaxAuthority titleClass="requiredField" />
          <#if !taxAuthPartyInfo?exists>
            <@inputSelectTaxAuthorityCell list=taxAuthorities required=false defaultGeoId=taxAuthPartyInfo?if_exists.taxAuthGeoId?if_exists defaultPartyId=taxAuthPartyInfo?if_exists.taxAuthPartyId?if_exists />
          <#else>
            <@inputHidden name="taxAuthGeoId" value=taxAuthPartyInfo.taxAuthGeoId />
            <@inputHidden name="taxAuthPartyId" value=taxAuthPartyInfo.taxAuthPartyId />
            <@displayCell text=taxAuthPartyInfo.groupName />
          </#if>
      </tr>
      <tr>
          <@displayTitleCell title=uiLabelMap.CommonFromDateTime />
          <#if !taxAuthPartyInfo?exists>
            <@inputDateTimeCell name="fromDate" form="editpartytaxauthinfo"  default=taxAuthPartyInfo?if_exists.fromDate?if_exists/>
          <#else>
            <@inputHidden name="fromDate" value=taxAuthPartyInfo.fromDate />
            <@displayDateCell date=taxAuthPartyInfo.fromDate />
          </#if>
      </tr>
      <tr>
          <@displayTitleCell title=uiLabelMap.CommonThruDateTime />
          <@inputDateTimeCell name="thruDate" form="editpartytaxauthinfo" default=taxAuthPartyInfo?if_exists.thruDate?if_exists/>
      </tr>
      <tr>
          <@displayTitleCell title=uiLabelMap.OpentapsTaxAuthPartyId />
          <@inputTextCell name="partyTaxId" default=taxAuthPartyInfo?if_exists.partyTaxId?if_exists/>
      </tr>
      <tr>
          <@displayTitleCell title=uiLabelMap.PartyTaxIsExempt titleClass="requiredField" />
          <@inputIndicatorCell name="isExempt" default=taxAuthPartyInfo?if_exists.isExempt?if_exists />
      </tr>
    <tr><td colspan="2">&nbsp;</td></tr>
    <tr>
      <td>&nbsp;</td>
      <td>
        <a href="javascript:document.editpartytaxauthinfo.submit()" class="subMenuButton">${submitLabel}</a>
        <@inputConfirm title=uiLabelMap.CommonCancel href="${donePage}?partyId=${partyId}"/>
      </td>
    </tr>
  </table>
  </form>

<!-- end editpartytaxauthinfo.ftl -->

