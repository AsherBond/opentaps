<#--
 * Copyright (C) 2009 Open Source Strategies, Inc.
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
<#--/* @author: Michele Orru' (michele.orru@integratingweb.com) */-->


    <div id="wrapper">
        	<div id="form">
        <#-- if user is authenticated -->
        <#if userLogin?exists>
            <h2>${uiLabelMap.CommonWelcome} <br />${firstName} ${lastName}</h2>
                <br />
                <form id="logout" method="post" action="<@ofbizUrl>logout${previousParams?if_exists}</@ofbizUrl>">
                   <input class="decorativeSubmit" style="width:65px; margin-left:1px;" type="submit"  value="${uiLabelMap.CommonLogout}" />
                </form>

        <#-- if user IS NOT authenticated, shows login form -->
        <#else>
            <h2>${uiLabelMap.OpentapsLoginGreeting}</h2>



        <#-- handles service error messages -->

        <#if requestAttributes.errorMessageList?has_content><#assign errorMessageList=requestAttributes.errorMessageList></#if>
        <#if requestAttributes.eventMessageList?has_content><#assign eventMessageList=requestAttributes.eventMessageList></#if>
        <#if requestAttributes.serviceValidationException?exists><#assign serviceValidationException = requestAttributes.serviceValidationException></#if>
        <#if requestAttributes.uiLabelMap?has_content><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>

        <#if !errorMessage?has_content>
          <#assign errorMessage = requestAttributes._ERROR_MESSAGE_?if_exists>
        </#if>
        <#if !errorMessageList?has_content>
          <#assign errorMessageList = requestAttributes._ERROR_MESSAGE_LIST_?if_exists>
        </#if>
        <#if !eventMessage?has_content>
          <#assign eventMessage = requestAttributes._EVENT_MESSAGE_?if_exists>
        </#if>
        <#if !eventMessageList?has_content>
          <#assign eventMessageList = requestAttributes._EVENT_MESSAGE_LIST_?if_exists>
        </#if>

        <#-- display the error messages -->
        <#if (errorMessage?has_content || errorMessageList?has_content)>
          <div id="errorDiv" class="serviceError">
            <p>${uiLabelMap.CommonFollowingErrorsOccurred}:</p>
            <#if errorMessage?has_content && errorMessage != "null">
              <p>${errorMessage}</p>
            </#if>
            <#if errorMessageList?has_content>
              <#list errorMessageList as errorMsg>
                <#if errorMsg?exists && errorMsg != "null">
                    <p>${errorMsg}</p>
                </#if>
              </#list>
            </#if>
          </div>
        </#if>

        <#-- display the event messages -->
        <#if (eventMessage?has_content || eventMessageList?has_content)>
        <div id="errorDiv" class="serviceError">
            <p>${uiLabelMap.CommonFollowingOccurred}:</p>
            <#if eventMessage?has_content && eventMessage != "null">
              <p>${eventMessage}</p>
            </#if>
            <#if eventMessageList?has_content && eventMessageList != "null">
              <#list eventMessageList as eventMsg>
                <#if errorMsg?exists && errorMsg != "null">
                    <p>${eventMsg}</p>
                </#if>
              </#list>
            </#if>
          </div>
        </#if>

        <#-- / handles service error messages -->


                <form id="login" method="post" action="<@ofbizUrl>login${previousParams?if_exists}</@ofbizUrl>">
            <p class="top">
              <label for="username">${uiLabelMap.CommonUsername}</label>
              <input class="inputLogin" type="text" id="username" name="USERNAME" size="50"/>
            </p>
            <p>
              <label for="password">${uiLabelMap.CommonPassword}</label>
              <input class="inputLogin" type="password" id="password" name="PASSWORD" size="50"/>
            </p>

            <p>
              <input class="decorativeSubmit" type="submit"  value="${uiLabelMap.CommonLogin}" />
              </p>
            </form>
            <h3><a href="javascript:forgotPasswd()">${uiLabelMap.CommonForgotYourPassword}?</a></h3>

           <form id="forgotpasswd" method="post" action="<@ofbizUrl>forgotpassword${previousParams?if_exists}</@ofbizUrl>">
            <p class="top">
                <label for="username">${uiLabelMap.CommonUsername}</label>
                <input class="inputLogin" type="text" id="username" name="USERNAME" size="50"/> <br />
            </p>
            <p>
                <input type="submit" name="EMAIL_PASSWORD" class="decorativeSubmit" value="${uiLabelMap.CommonEmailPassword}"/>
            </p>

           </form>

        </#if>
        </div>



<#if apps?exists>
         <div id="row">
        <#assign appIndex = 0 />
        <#list apps as app>
            <#assign appIndex = appIndex + 1 />
            <div id="button" class="${app.applicationId}" onmouseover="javascript:writeAppDetails('${app.applicationId?upper_case}','${app.name}','${app.description}')">

                <a href="${app.linkUrl}<#if externalKeyParam?exists>?${externalKeyParam}</#if>">
                   <img src="${app.imageUrl}" onmouseover="this.src='${app.imageHoverUrl}'" onmouseout="this.src='${app.imageUrl}'" />
                </a>
            </div>
                <#if !app_has_next>
                   </div> <#-- close row-->
                <#elseif appIndex % 4 == 0>
                   </div> <#-- close row-->
                   <div id="row"> <#-- create a new row-->
                </#if>
        </#list>
    </#if>
         </div> <#-- end of the latest row-->

</div>  <#-- end of wrapper-->


