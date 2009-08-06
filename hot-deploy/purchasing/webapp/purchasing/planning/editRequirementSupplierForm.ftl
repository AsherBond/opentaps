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

