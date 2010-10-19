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
<script type="text/javascript">
function removePartyAttribute(/*String*/ partyAttributeIndex) {
  var form = document.getElementById("updateCustomField_" + partyAttributeIndex);
  form.action = "removeAccountCustomField";
  form.submit();
}
</script>
<!-- only display the custom fields section when use has create or view permission-->
<#if hasAccountCustCreatePermission || hasAccountCustViewPermission>
<@frameSection title=uiLabelMap.CrmCustomFields>
      <table class="twoColumn">
        <tbody>
          <tr>
            <td/>
            <td><span class="tableheadtext"><b>${uiLabelMap.CrmCustomFieldName}</b></span></td>
            <td><span class="tableheadtext"><b>${uiLabelMap.CrmCustomFieldValue}</b></span></td>
            <td/>
          </tr>
          <#if hasAccountCustViewPermission && partyAttributes?exists>
               <#list partyAttributes as partyAttribute>

              <form method="post" action="<@ofbizUrl>updateAccountCustomField</@ofbizUrl>" name="updateCustomField_${partyAttribute_index}" id="updateCustomField_${partyAttribute_index}">
              <tr>
                <td/>
                <@inputHidden name="partyId" value="${partyAttribute.partyId}"/>
                <@inputHidden name="attrName" value="${partyAttribute.attrName}"/>
                <@displayCell text="${partyAttribute.attrName}" />
                <@inputTextCell name="attrValue" default="${partyAttribute.attrValue}" ignoreParameters=true/>
                <td>
                <#if hasAccountCustUpdatePermission || userLogin.userLoginId==partyAttribute.createdByUserLoginId?default("")>
                <a href="javascript:document.updateCustomField_${partyAttribute_index}.submit();"><img src="<@ofbizContentUrl>/images/dojo/src/widget/templates/buttons/save.gif</@ofbizContentUrl>" width="18" height="18" border="0" alt="${uiLabelMap.CommonSave}"/></a>
                </#if>
                <#if hasAccountCustDeletePermission || userLogin.userLoginId==partyAttribute.createdByUserLoginId?default("")>
                   <a href="javascript:removePartyAttribute(${partyAttribute_index});"><img src="<@ofbizContentUrl>/opentaps_images/buttons/glass_buttons_red_X.png</@ofbizContentUrl>" width="18" height="18" border="0" alt="${uiLabelMap.CommonSave}"/></a>
                </#if>                           
                </td>            
              </tr>
              </form>
              </#list>          
          </#if>
          <#if hasAccountCustCreatePermission>
          <form method="post" action="<@ofbizUrl>createAccountCustomField</@ofbizUrl>" name="addCustomField">
          <tr>
            <td/>
            <@inputHidden name="partyId" value="${partySummary.partyId}"/>
            <@inputTextCell name="attrName" />
            <@inputTextCell name="attrValue"/>
            <td><a href="javascript:document.addCustomField.submit();"><img src="/opentaps_images/buttons/glass_buttons_lumen_desi_01_plus_24x24.png" border="0" alt="${uiLabelMap.CommonAdd}"/></a></td>            
          </tr>
          </form>
          </#if>
        </tbody>
      </table>
</@frameSection>
</#if>

