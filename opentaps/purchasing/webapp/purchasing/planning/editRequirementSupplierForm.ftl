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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<form action="<@ofbizUrl>updateRequirementSupplier</@ofbizUrl>" method="post" name="editRequirementSupplier">
  <@inputHidden name="requirementId" value=parameters.requirementId?if_exists />
  <@inputHidden name="partyId" value=parameters.partyId?if_exists />
  <@inputHidden name="fromDate" value=parameters.fromDate?if_exists />
  <@inputHidden name="thruDate" value=parameters.thruDate?if_exists />
  <@inputHidden name="roleTypeId" value=parameters.roleTypeId?if_exists />
  <@inputHidden name="_rowCount" value="1" />
  <table class="twoColumnForm">
    <@inputAutoCompleteSupplierRow title=uiLabelMap.ProductSupplier name="newPartyId" titleClass="requiredField" />
    <@inputSubmitRow title=uiLabelMap.CommonSubmit />
    
  </table>
</form>

