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

<#if mergeFormCategoryId?has_content>
  <#assign sectionTitle = "${uiLabelMap.CrmFormLetterTemplateCategorie} [${mergeFormCategoryId}]"/>
<#else>
  <#assign sectionTitle = uiLabelMap.CrmFormLetterTemplateCategorie/>
</#if>
<@frameSectionHeader title="${sectionTitle}"/>

<#if mergeFormCategoryId?has_content>
 <#assign formName="updateMergeFormCategory" />
 <#assign formSubmit=uiLabelMap.CommonSave />
 <form method="post" action="deleteMergeFormCategory" name="deleteMergeFormCategory">
  <@inputHidden name="mergeFormCategoryId" value=mergeFormCategoryId />
 </form>
<#else>
 <#assign formName="createMergeFormCategory" />
 <#assign formSubmit=uiLabelMap.CommonCreate />
</#if>

<div class="form">
  <form method="post" action="${formName}" name="${formName}" style="margin: 0;">
    <#if mergeFormCategoryId?exists><@inputHidden name="mergeFormCategoryId" value=mergeFormCategoryId /></#if >
    <@inputHidden name="partyId" value=userLogin.partyId />
  
    <div class="formRow">
      <span class="formLabelRequired">${uiLabelMap.FormFieldTitle_categoryName}</span>
      <span class="formInputSpan">
        <input type="text" class="inputBox" name="mergeFormCategoryName" value="${(mergeFormCategory.mergeFormCategoryName)?if_exists}" size="50" maxlength="100"/>
      </span>
    </div>

    <div class="formRow">
      <span class="formInputSpan">
        <input type="submit" class="smallSubmit" name="submitButton" value="${formSubmit}" onClick="submitFormWithSingleClick(this)" />
        <#if mergeFormCategory?has_content><@inputConfirm title=uiLabelMap.CommonDelete form="deleteMergeFormCategory" /></#if >
      </span>
    </div>

    <div class="spacer">&nbsp;</div>
  </form>
</div>
