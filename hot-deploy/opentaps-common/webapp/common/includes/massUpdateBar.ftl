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

<#--
    Simple template designed for create button bar in multi-select forms.
    Expect two variables in context:
        massUpdateForm: String. This form will be submited when button pressed.
        massUpdatePane: List of Map. Data required for button creation.   
           title: Button Label as property name
           action: action for massUpdateForm
           confirm: Y or N. Optional. Template creates confirmation button with class 
                    buttonDangerous and default message if confirm=Y.
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if massUpdatePane?has_content && massUpdateForm?has_content>

<script type="text/javascript">
<!--
  function doSubmit(action, confirmation, confirmText) {
    if (confirmation && !confirm(confirmText)) {
      return;
    }
    document.${massUpdateForm}.action = action;
    document.${massUpdateForm}.submit();
  }
//-->
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
  </#list>
</#if>
