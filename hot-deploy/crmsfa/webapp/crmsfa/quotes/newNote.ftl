<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<#-- This file has been modified by Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if security.hasEntityPermission("ORDERMGR", "_VIEW", session)>  

  <@frameSection title=uiLabelMap.OrderAddNote>

    <form method="post" action="<@ofbizUrl>CreateQuoteNote</@ofbizUrl>" name="createnoteform">
      <table class="twoColumnForm">
        <@inputHidden name="internalNote" value="N"/>
        <@inputHidden name="quoteId" value=quoteId />
        <@inputTextareaRow title=uiLabelMap.OrderNote name="note" cols=70 />
        <tr>
          <td/>
          <td>
            <@inputSubmit title=uiLabelMap.CommonCreate />
            <@displayLink text=uiLabelMap.CommonCancel href="ViewQuote?quoteId=${quoteId}" class="buttontext" />
          </td>
        </tr>
      </table>
    </form>

  </@frameSection>

<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
