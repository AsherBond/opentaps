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

<form method="post" action="<@ofbizUrl>openPicklists</@ofbizUrl>" name="findOpenPicklistForm" style="margin: 0pt;">
  <@inputHidden name="performFind" value="Y"/>
   <table class="twoColumnForm" style="border:0">
    <tr>
      <@displayTitleCell title=uiLabelMap.ProductPickList />
      <@inputTextCell name="picklistId" maxlength="20" />
    </tr>
    <tr>
      <@displayTitleCell title=uiLabelMap.CommonStatus />
      <@inputSelectCell name="statusId" list=validPicklistStatus required=false key="statusId" displayField="description" />
    </tr>
    <@inputSubmitRow title=uiLabelMap.CommonFind onClick="" />
  </table>
</form>
