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

<#assign title>
${uiLabelMap.CrmFormLetterTemplate}
<#if mergeFormId?has_content>
  [${mergeFormId}]
</#if>
</#assign>

<@frameSectionHeader title=title />

<#if mergeFormId?has_content>
 <#assign formName="updateMergeForm" />
 <#assign formSubmit=uiLabelMap.CommonSave />
 <form method="post" action="deleteMergeForm" name="deleteMergeForm">
  <@inputHidden name="mergeFormId" value=mergeFormId />
 </form>

<script type="text/javascript">
/*<![CDATA[*/
  function assignCategory() {
    var categorySelect = document.getElementById('mergeFormCategoryName');
    var mergeFormCategorySelected = categorySelect.selectedIndex;
    if ( mergeFormCategorySelected < 0 ) return;
    var mergeFormCategoryId = categorySelect ? categorySelect.options[mergeFormCategorySelected].value : null;
    if (! mergeFormCategoryId) return false;
    
    var context = {"mergeFormId" : "${mergeFormId}", "mergeFormCategoryId" : mergeFormCategoryId };

    opentaps.hide(document.getElementById('categoryControl'));
    opentaps.sendRequest('<@ofbizUrl>assignCategoryToMergeFormJSON</@ofbizUrl>', context, replaceCategoryControl, {target: 'assignSpinner'});
  }

  function removeCategory(mergeFormCategoryId) {
    var context = {"mergeFormId" : "${mergeFormId}", "mergeFormCategoryId" : mergeFormCategoryId };
    opentaps.hide(document.getElementById('categoryControl'));
    opentaps.sendRequest('<@ofbizUrl>removeCategoryFromMergeFormJSON</@ofbizUrl>', context, replaceCategoryControl, {target: 'assignSpinner'});
  }
  
  function replaceCategoryControl(/* Array */ data) {
    var catControl = document.getElementById('categoryControl');
    if (! data ) {
      opentaps.show(catControl);
      return;
    }

    // These are the already assigned categories
    opentaps.removeChildNodes(catControl);
    for (idx=0; idx<data.mergeFormCategories.size(); idx++) {
      var id = data.mergeFormCategories[idx].mergeFormCategoryId;
      catControl.appendChild(opentaps.createSpan(null, data.mergeFormCategories[idx].mergeFormCategoryName), 'tabletext');
      var removeLink = opentaps.createAnchor(null, null, "-", "buttontext", {'onclick' : opentaps.makeFunction('removeCategory', [id])});
      catControl.appendChild(removeLink);
    }

    if (data.categories.size() > 0) {
        // These are the category that can be assigned        
        var options = new Array();
        for (idx=0; idx<data.categories.size(); idx++) {
            options.push( data.categories[idx].mergeFormCategoryId );
            options.push( data.categories[idx].mergeFormCategoryName );
        }
        catControl.appendChild( opentaps.createSelect("mergeFormCategoryName", "mergeFormCategoryName", "inputBox", options, data.categories[0].mergeFormCategoryId, null) );
        catControl.appendChild( opentaps.createAnchor(null, null, "+", "buttontext", {'onclick' : function(){assignCategory()}}) );
    }

    opentaps.show(catControl);
  } 
  
/*]]>*/
</script>
<#else >
 <#assign formName="createMergeForm" />
 <#assign formSubmit=uiLabelMap.CommonCreate />
</#if >

<div class="form">
  <form method="post" action="${formName}" name="${formName}" style="margin: 0;">
    <#if mergeFormId?exists><@inputHidden name="mergeFormId" value=mergeFormId /></#if >
    <@inputHidden name="partyId" value=userLogin.partyId />
  
    <div class="formRow">
      <span class="formLabelRequired">${uiLabelMap.OpentapsTemplateName}</span>
      <span class="formInputSpan">
        <input type="text" class="inputBox" name="mergeFormName" value="${(mergeForm.mergeFormName)?if_exists}" size="50" maxlength="100"/>
      </span>
    </div>

    <div class="formRow">
      <span class="formLabel">${uiLabelMap.CrmFormLetterTemplatePrivate}</span>
      <span class="formInputSpan">
        <input type="checkbox" name="privateForm" value="Y" <#if mergeForm?default({}).partyId?has_content>checked="checked"</#if>/>
      </span>
    </div>

    <#if mergeFormId?has_content>
    <div class="formRow">
      <span class="formLabel">${uiLabelMap.FormFieldTitle_categoryName}</span>
      <span class="formInputSpan">
        <span id="categoryControl">
          <#list mergeFormCategories as cat>
            ${cat.mergeFormCategoryName}<a class="buttontext" href="javascript:removeCategory('${cat.mergeFormCategoryId}')">-</a>
          </#list >
          <#if categories?size gt 0 >
            <@inputSelect name="mergeFormCategoryName" list=categories displayField="mergeFormCategoryName" key="mergeFormCategoryId" />
            <a class="buttontext" href="javascript:assignCategory()">+</a>
          </#if >
        </span>
        <span id="assignSpinner">&nbsp;</span>
      </span>
    </div>
    <#else>
      <#-- TODO: implement categories for new template, possibly using Javscript to set the list of initial categories -->
    </#if >

    <div class="formRow">
      <span class="formLabel">${uiLabelMap.CommonDescription}</span>
      <span class="formInputSpan">
        <input type="text" class="inputBox" name="description" size="50" value="${(mergeForm.description)?if_exists}" maxlength="255"/>
      </span>
    </div>

    <div class="formRow">
      <span class="formLabel">${uiLabelMap.PartySubject}</span>
      <span class="formInputSpan">
        <input type="text" class="inputBox" name="subject" size="50" value="${(mergeForm.subject)?if_exists}" maxlength="255"/>
      </span>
    </div>

    <div class="formRow">
      <span class="formLabelRequired">${uiLabelMap.OpentapsTemplate}</span>
      <span class="formInputSpan">
        <@htmlTextArea textAreaId="mergeFormText" value=(mergeForm.mergeFormText)?if_exists tagFileLocation="component://crmsfa/webapp/crmsfa/includes/crmsfaFormEditorTags.ftl" style="width: 100%; height: 100%"/>
      </span>
    </div>

    <div class="formRow">
      <span class="formInputSpan">
        <input type="submit" class="smallSubmit" name="submitButton" value="${formSubmit}" onClick="submitFormWithSingleClick(this)" />
        <#if mergeForm?has_content><@inputConfirm title=uiLabelMap.CommonDelete form="deleteMergeForm"/></#if >
      </span>
    </div>

    <div class="spacer">&nbsp;</div>
  </form>
</div>
