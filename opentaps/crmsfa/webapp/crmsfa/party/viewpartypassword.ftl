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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl" />

<div class="subSectionBlock">
  <@sectionHeader title=uiLabelMap.CrmUserLoginInformation />
  <div class="form">
    <#if partyAndUserLogin?exists >
      <form method="post" action="<@ofbizUrl>updatePartyPassword</@ofbizUrl>" name="updatepartypassword">
        <@inputHidden name="donePage" value="${donePage}" />
        <@inputHidden name="userLoginId" value="${userLoginId}" />
        <@inputHidden name="partyId" value="${partyId}" />
        <table width="90%" border="0" cellpadding="2" cellspacing="0">
          <@displayRow title=uiLabelMap.CrmUserLogin text=partyAndUserLogin.userLoginId />
          <#if passwordIsEncrypted >
            <@displayRow title=uiLabelMap.CommonPassword text=uiLabelMap.CrmPasswordEncryptedCannotDisplay />
          <#else >
            <@displayRow title=uiLabelMap.CommonPassword text=partyAndUserLogin.currentPassword />
          </#if >
          <@inputTextRow name="passwordhint" title=uiLabelMap.PartyPasswordHint default=partyAndUserLogin.passwordHint />
          <@inputTextRow name="newpassword"  title=uiLabelMap.PartyNewPassword titleClass="requiredField" password=true />
          <@inputTextRow name="confirmpassword" title=uiLabelMap.PartyNewPasswordVerify titleClass="requiredField" password=true />
          <tr>
            <td/>
            <td align="left">
              <a href="javascript:document.updatepartypassword.submit()" class="buttontext">${uiLabelMap.CrmChangePassword}</a>
              <a href="${donePage}?partyId=${partyId?if_exists}" class="buttontext">${uiLabelMap.CommonCancel}</a>
            </td>
          </tr>
        </table>
      </form>
    <#else >
      <#assign notfound = uiLabelMap.CrmParyUserLoginNotFound?interpret/>
      <p class="contenttext"><@notfound /></p>
      <a href="<@ofbizUrl>${donePage}?partyId=${partyId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
    </#if >
  </div>
</div>