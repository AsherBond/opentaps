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

<tr>
  <@displayTitleCell title=uiLabelMap.PartyBillingAddress/>
  <#if postalAddressInfos?has_content || curContactMech?exists>
    <td> 
      <select name="contactMechId" class="selectBox">
        <#if curPostalAddress?exists>
          <option value="${curContactMech.contactMechId}" selected="selected" >
            <#if curPostalAddress.toName?exists>${curPostalAddress.toName?if_exists},</#if> ${curPostalAddress.address1?if_exists}, ${curPostalAddress.city?if_exists}<#if curPostalAddress.stateProvinceGeoId?exists>, ${curPostalAddress.stateProvinceGeoId?if_exists}</#if>
          </option>
        </#if>
        <#list postalAddressInfos as postalAddressInfo>
          <#assign contactMech = postalAddressInfo.contactMech/>
          <#assign postalAddress = postalAddressInfo.postalAddress/>

          <option value="${contactMech.contactMechId}">
            <#if postalAddress.toName?exists>${postalAddress.toName?if_exists},</#if>  ${postalAddress.address1?if_exists}, ${postalAddress.city?if_exists}<#if postalAddress.stateProvinceGeoId?exists>, ${postalAddress.stateProvinceGeoId?if_exists}</#if>
          </option>
        </#list>
      </select>
    </td>
  <#else>
    <td>${uiLabelMap.PartyNoContactInformation}.</td>
  </#if>
</tr>



