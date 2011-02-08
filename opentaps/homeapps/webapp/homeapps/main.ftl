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
<#--/* @author: Michele Orru' (michele.orru@integratingweb.com) */-->

<style type="text/css">
.gray-panel-header {
    background: gray;
    color: white;
    font:bold 11px tahoma,arial,verdana,sans-serif;
    padding:5px 2px 4px 20px;
    border:1px gray;
    line-height:15px;
}

.rss-frame-section {
    width: 245px;
    margin-left: 0px;
    margin-right: auto;
    margin-top: 20px;
}

.rss-tabletext, .rss-tabletext a:link,.rss-tabletext a:visited {
font-size: 10px;
text-decoration: none;
font-family: Verdana, Arial, Helvetica, sans-serif;
text-decoration: none;
color: black;
}

.rss-tabletext a:hover {
text-decoration: underline;
}

.rss-frame-section-body
{
background-color:#FFFFFF;
padding:4px;
border: 1px solid #999999;
}
</style>

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
        <#assign errorMessage = requestAttributes._ERROR_MESSAGE_?if_exists/>
      </#if>
      <#if !errorMessageList?has_content>
        <#assign errorMessageList = requestAttributes._ERROR_MESSAGE_LIST_?if_exists/>
      </#if>
      <#if !eventMessage?has_content>
        <#assign eventMessage = requestAttributes._EVENT_MESSAGE_?if_exists/>
      </#if>
      <#if !eventMessageList?has_content>
        <#assign eventMessageList = requestAttributes._EVENT_MESSAGE_LIST_?if_exists/>
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


      <#-- handles service error messages -->

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
    <br/><br/>
    <@include location="component://opentaps-common/webapp/common/includes/latestnews.ftl"/>
  </div>


  <#if apps?exists>
    <div id="row">
      <#assign appIndex = 0 />
      <#list apps as app>
        <#if (!app.hide?exists || app.hide != "Y") && app.linkUrl?has_content>
          <#assign appIndex = appIndex + 1 />
          <div id="button" class="${app.applicationId}" onmouseover="javascript:writeAppDetails('${app.shortName!app.applicationId}','${app.applicationName!app.applicationId}','${app.description!app.applicationId}')">
            <#if app.imageUrl?has_content>
              <a href="${app.linkUrl}<#if externalKeyParam?exists>?${externalKeyParam}</#if>">
                <img src="${app.imageUrl}" onmouseover="this.src='${app.imageHoverUrl!app.imageUrl}'" onmouseout="this.src='${app.imageUrl}'" />
              </a>
            </#if>
            <div id="label" style="margin-left: 34px;" for="${app.applicationId}">
              <a style="color: black;" href="${app.linkUrl}<#if externalKeyParam?exists>?${externalKeyParam}</#if>" >
                ${app.shortName!app.applicationId}
              </a>
            </div>
          </div>

          <#if !app_has_next>
            </div> <#-- close row-->
          <#elseif appIndex % 4 == 0>
            </div> <#-- close row-->
            <div id="row" style="margin-top: 3px;" > <#-- create a new row-->
          </#if>
        </#if>
      </#list>
    </#if>
  </div> <#-- end of the latest row-->

</div>  <#-- end of wrapper-->

