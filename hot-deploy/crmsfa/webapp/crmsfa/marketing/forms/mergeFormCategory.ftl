<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div class="subSectionHeader">
    <div class="subSectionTitle">
      ${uiLabelMap.CrmFormLetterTemplateCategorie} <#if mergeFormCategoryId?has_content >[${mergeFormCategoryId}]</#if >
    </div>
</div>

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
