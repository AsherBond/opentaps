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
<br/>
<#if "Y" == parameters.performFind!>
  <form method="post" action="<@ofbizUrl>approveSupplierRequirements</@ofbizUrl>" name="ApproveRequirements">
    <table class="listTable">
      <tr class="listTableHeader">
        <td>${uiLabelMap.WorkEffortRequirementId}</td>
        <td>${uiLabelMap.WorkEffortRequirementType}</td>
        <td>${uiLabelMap.Facility}</td>
        <td>${uiLabelMap.ProductProduct}</td>
        <td>${uiLabelMap.WorkEffortRequiredDate}</td>
        <td>${uiLabelMap.CommonStatus}</td>
        <td>${uiLabelMap.ProductSupplier}</td>
        <td>${uiLabelMap.CommonQuantity}</td>
        <td><@inputMultiSelectAll form="ApproveRequirements"/></td>
      </tr>
      <#assign hasEditable = false />
      <#list requirements as requirement>
        <#assign type = requirement.getRelatedOneCache("RequirementType")/>
        <#assign status = requirement.getRelatedOneCache("StatusItem")/>
        <tr class="${tableRowClass(requirement_index)}">
          <@inputHidden name="requirementId" value=requirement.requirementId index=requirement_index />
          <@inputHidden name="partyId" value=requirement.partyId! index=requirement_index />
          <@inputHidden name="roleTypeId" value=requirement.roleTypeId! index=requirement_index />
          <@inputHidden name="fromDate" value=requirement.fromDate! index=requirement_index />
          <@displayLinkCell href="EditRequirement?requirementId=${requirement.requirementId}" text="${requirement.requirementId}" />                      
          <@displayCell text=type.get("description",locale)! />
          <@displayCell text=requirement.facilityId! />
          <@displayCell text=requirement.productId! />
          <@displayDateCell date=requirement.requiredByDate! />
          <@displayCell text=status.get("description",locale)! />
          <#if "REQ_PROPOSED" == requirement.statusId || "REQ_CREATED" == requirement.statusId || "REQ_APPROVED" == requirement.statusId>
            <#assign hasEditable = true />
            <#if "PRODUCT_REQUIREMENT" == requirement.requirementTypeId>
              <@inputAutoCompleteSupplierCell name="newPartyId" default=requirement.partyId index=requirement_index />
            <#else>
              <@displayCell text=requirement.partyId! />
            </#if>
            <@inputTextCell name="quantity" size="5" default=requirement.quantity maxlength="10" index=requirement_index />
            <@inputMultiCheckCell index=requirement_index />
          <#else>
            <@displayCell text=requirement.partyId! />
            <@displayCell text=requirement.quantity />
            <td>&nbsp;</td>
          </#if>
        </tr>
        </#list>
        <@inputHiddenUseRowSubmit />
        <@inputHiddenRowCount list=requirements />
    </table>
  </form>

  <#if hasEditable>
    <#if massUpdatePane?has_content && massUpdateForm?has_content>

<script type="text/javascript">
/*<![CDATA[*/
  function doSubmit(action, confirmation, confirmText) {
    if (confirmation && !confirm(confirmText)) {
      return;
    }
    document.${massUpdateForm}.action = action;
    document.${massUpdateForm}.submit();
  }
/*]]>*/
</script>

      <#list massUpdatePane as operationDef>
        <#if operationDef.confirm?exists>
          <#if operationDef.confirm == 'Y'>
            <@displayLink href="javascript:doSubmit('${operationDef.action}', true, '${uiLabelMap.OpentapsAreYouSure}')" text=uiLabelMap.get(operationDef.title) class="buttonDangerous"/>
          <#else>
            <@displayLink href="javascript:doSubmit('${operationDef.action}', false, '')" text=uiLabelMap.get(operationDef.title) class="buttontext"/>
          </#if>
        <#else>
          <@displayLink href="javascript:doSubmit('${operationDef.action}', false, '')" text=uiLabelMap.get(operationDef.title) class="buttontext"/>
        </#if>
        <span class="tabletext">&nbsp;</span>
      </#list>
    </#if>

  </#if> <#-- if hasEditable -->
</#if> <#-- if perform find -->
