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
<#-- Copyright (c) 2005-2006 Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>


<#if parameters.partyId?has_content && templates?has_content>

<#assign partyId = parameters.partyId/>

<div class="screenlet">
    <div class="screenlet-header"><div class="boxhead">${uiLabelMap.CrmFormLetters}</div></div>
    <div class="screenlet-body">
        <form name="generateFormLetterForm" method="post" action="<@ofbizUrl>generateFormLetter</@ofbizUrl>">
            <@inputHidden name="targetPartyId" value="${partyId}"/>
            <@inputHidden name="partyId" value="${userLogin.partyId}"/>
            <table>
                <tr>
                    <td>
                        <span class="tabletext">${uiLabelMap.CrmFormLetterTemplate}</span><br>
                        <select name="mergeFormId" class="inputBox" style="width: 97%;">
                        <#list templates?default([]) as template>
                        <option value="${template.mergeFormId}">${template.mergeFormName?if_exists}</option>
                        </#list>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td>
                        <span class="tabletext">${uiLabelMap.OpentapsOutput}</span><br>
                        <#assign reportTypes = {}/>
                        <#assign reportTypes = reportTypes + {"application/pdf" : uiLabelMap.OpentapsContentType_ApplicationPDF}/>
                        <#assign reportTypes = reportTypes + {"text/html" : uiLabelMap.OpentapsContentType_TextHtml}/>
                        <#assign reportTypes = reportTypes + {"text/plain" : uiLabelMap.OpentapsContentType_TextPlain}/>
                        <#assign reportTypes = reportTypes + {"application/ms-word" : uiLabelMap.OpentapsContentType_ApplicationMSWord}/>
                        <#assign reportTypes = reportTypes + {"text/xml" : uiLabelMap.OpentapsContentType_TextXML}/>
                        <@inputSelectHash name="reportType" hash=reportTypes/>
                    </td>
                </tr>
                <@inputSubmitCell title=uiLabelMap.OpentapsGenerateFormLetter onClick=""/>
            </table>
        </form>
    </div>
</div>
</#if>
