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

<#if findPartyWidget?has_content>
  <#if viewPreferences?has_content && viewPreferences.MY_OR_TEAM_ACCOUNTS?has_content> 
      <@gwtWidget id=findPartyWidget class="subSectionBlock" viewPref="${viewPreferences.MY_OR_TEAM_ACCOUNTS}"/>
  <#else>
      <@gwtWidget id=findPartyWidget class="subSectionBlock"/>
  </#if>
<#else/>
<#-- This is still used by lookup screens. -->
<script type="text/javascript">
  var switchFromNameMode = true;      
  var switchFromPhoneMode = false;
  var switchFromIdMode = false;
  var switchFromAdvancedMode = false;

  function switchSearchMode(/* Element */ modeSwitch) {
    var switchToPhoneMode = (modeSwitch.options[modeSwitch.selectedIndex].value == 'phoneNumber');
    var switchToNameMode = (modeSwitch.options[modeSwitch.selectedIndex].value == 'name');
    var switchToIdMode = (modeSwitch.options[modeSwitch.selectedIndex].value == 'crmPartyId');
    var switchToAdvancedMode = (modeSwitch.options[modeSwitch.selectedIndex].value == 'advanced');
    var searchByName = document.getElementById('searchByName');
    var searchByPhone = document.getElementById('searchByPhone');
    var searchById = document.getElementById('searchById');
    var advancedSearch = document.getElementById('advancedSearch');
    var otherModeSwitch;
    var inputs;
    var hiddenSearchOption = document.getElementById('searchOption');

    if (switchToNameMode) {
        inputs = searchByName.getElementsByTagName('input');
        otherModeSwitch = document.getElementById('modeSwitchName');
        hiddenSearchOption.value = 'searchByName';
    } else if (switchToPhoneMode) {
        inputs = searchByPhone.getElementsByTagName('input');
        otherModeSwitch = document.getElementById('modeSwitchPhone');
        hiddenSearchOption.value = 'searchByPhone';        
    } else if (switchToIdMode) {
        inputs = searchById.getElementsByTagName('input');
        otherModeSwitch = document.getElementById('modeSwitchId');
        hiddenSearchOption.value = 'searchById';        
    } else if (switchToAdvancedMode) {
        inputs = advancedSearch.getElementsByTagName('input');
        otherModeSwitch = document.getElementById('modeSwitchAdvanced');
        hiddenSearchOption.value = 'advancedSearch';        
    }
    
    for (var x = 0; x < inputs.length; x++) {
      if (inputs[x].type != 'submit') inputs[x].value = '';
    }
    
    otherModeSwitch.selectedIndex = modeSwitch.selectedIndex;    
    
    if (switchFromNameMode) {
        opentaps.expandCollapse('searchByName');
    } else if (switchFromPhoneMode) {
        opentaps.expandCollapse('searchByPhone');    
    } else if (switchFromIdMode) {
        opentaps.expandCollapse('searchById');    
    } else if (switchFromAdvancedMode) {
        opentaps.expandCollapse('advancedSearch');    
    }

    if (switchToNameMode) {
        opentaps.expandCollapse('searchByName');
    } else if (switchToPhoneMode) {
        opentaps.expandCollapse('searchByPhone');
        document.getElementById('phoneCountryCode').value = '${context.get("defaultCountryCode")}';
    } else if (switchToIdMode) {
        opentaps.expandCollapse('searchById');    
    } else if (switchToAdvancedMode) {
        opentaps.expandCollapse('advancedSearch');
    }
    
    switchFromNameMode = switchToNameMode;
    switchFromPhoneMode = switchToPhoneMode;
    switchFromIdMode = switchToIdMode;
    switchFromAdvancedMode = switchToAdvancedMode;            
    
  }
</script>

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#-- Is there an advantage to this, or can we just use set directives in the relevant screens?  -Leon  -->
<#assign findAccounts = (parameters._CURRENT_VIEW_ == "findAccounts" || parameters.thisRequestUri == "LookupAccounts")/>
<#assign findLeads = (parameters._CURRENT_VIEW_ == "findLeads" || parameters.thisRequestUri == "LookupLeads")/>
<#assign findContacts = (parameters._CURRENT_VIEW_ == "findContacts" || parameters.thisRequestUri == "LookupContacts")/>
<#assign findPartners = (parameters._CURRENT_VIEW_ == "findPartners" || parameters._CURRENT_VIEW_ == "partnersMain" || parameters.thisRequestUri == "LookupPartners")/>
<#assign roleTypeId = ""/>
<#assign buttonText = uiLabelMap.CommonFind/>
<#if findAccounts>
  <#assign roleTypeId = "ACCOUNT"/>
  <#assign buttonText = uiLabelMap.CrmFindAccounts/>
<#elseif findLeads>  
  <#assign roleTypeId = "PROSPECT"/>
  <#assign buttonText = uiLabelMap.CrmFindLeads/>
<#elseif findContacts>
  <#assign roleTypeId = "CONTACT"/>
  <#assign buttonText = uiLabelMap.CrmFindContacts/>
<#elseif findPartners>
  <#assign roleTypeId = "PARTNER"/>
  <#assign buttonText = uiLabelMap.OpentapsFindPartners>
<#else>
  <#assign buttonText = uiLabelMap.CommonFind/>
</#if>

<div class="subSectionBlock ">
  <form method="post" action="<@ofbizUrl>${parameters._CURRENT_VIEW_}</@ofbizUrl>" name="findForm">
    <input type="hidden" name="roleTypeIdFrom" value="${roleTypeId}"/>
    <input type="hidden" id="searchOption" name="searchOption" value="${searchOption?default('searchByName')}"/>    

    <@flexArea targetId="searchByName" save=false state=searchByName?default(false)?string("open", "closed") enabled=false controlClassClosed="hidden" controlClassOpen="hidden" style="border:0px">
      <table class="twoColumnForm">
        <tr>
          <td class="titleCell"><span class="tableheadtext">${uiLabelMap.OpentapsFindBy}</span></td>
          <td>
            <select id="modeSwitchName" class="inputBox" onchange="switchSearchMode(this)">
              <option value="crmPartyId" ${searchById?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.CrmPartyId}</option>            
              <option value="name" ${searchByName?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.CommonName}</option>
              <option value="phoneNumber" ${searchByPhone?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.OpentapsPhoneNumber}</option>
              <option value="advanced" ${advancedSearch?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.OpentapsAdvancedSearch}</option>              
            </select>
          </td>
        </tr>
        <#if findAccounts>
          <@inputTextRow name="groupName" title=uiLabelMap.CrmAccountName />
        <#elseif findLeads>
          <@inputTextRow name="firstName" title=uiLabelMap.PartyFirstName />
          <@inputTextRow name="lastName" title=uiLabelMap.PartyLastName />
          <@inputTextRow name="companyName" title=uiLabelMap.CrmCompanyName />
        <#elseif findContacts>
          <@inputTextRow name="firstName" title=uiLabelMap.PartyFirstName />
          <@inputTextRow name="lastName" title=uiLabelMap.PartyLastName />
        <#elseif findPartners>
          <@inputTextRow name="groupName" title=uiLabelMap.OpentapsPartnerName />
        <#else>
          <@inputTextRow name="groupName" title=uiLabelMap.CrmAccountName />
          <@inputTextRow name="firstName" title=uiLabelMap.PartyFirstName />
          <@inputTextRow name="lastName" title=uiLabelMap.PartyLastName />
        </#if>
        
        <tr>
          <td>&nbsp;</td>
          <td>
            <@inputSubmit title=buttonText />&nbsp;
          </td>
        </tr>
      </table>
    </@flexArea>  

    <@flexArea targetId="searchByPhone" save=false state=searchByPhone?default(false)?string("open", "closed") enabled=false controlClassClosed="hidden" controlClassOpen="hidden" style="border:0px">
      <table class="twoColumnForm">
        <tr>
          <td class="titleCell"><span class="tableheadtext">${uiLabelMap.OpentapsFindBy}</span></td>
          <td>
            <select id="modeSwitchPhone" class="inputBox" onchange="switchSearchMode(this)">
              <option value="crmPartyId" ${searchById?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.CrmPartyId}</option>            
              <option value="name" ${searchByName?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.CommonName}</option>
              <option value="phoneNumber" ${searchByPhone?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.OpentapsPhoneNumber}</option>
              <option value="advanced" ${advancedSearch?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.OpentapsAdvancedSearch}</option>
            </select>
          </td>
        </tr>
        <tr>
          <td class="titleCell"><span class="tableheadtext">${uiLabelMap.OpentapsPhoneNumber}</span></td>
          <td>
            <input class="inputBox" id="phoneCountryCode" name="phoneCountryCode" size="6" maxlength="10" value="${parameters.phoneCountryCode?default(context.get('defaultCountryCode'))}"/>
            <input class="inputBox" id="phoneAreaCode" name="phoneAreaCode" size="6" maxlength="10" value="${parameters.phoneAreaCode?if_exists}"/>
            <input class="inputBox" id="phoneNumber" name="phoneNumber" size="10" maxlength="20" value="${parameters.phoneNumber?if_exists}"/>
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>
              <span class="tabletext">[${uiLabelMap.PartyCountryCode}]&nbsp;</span>
              <span class="tabletext">[${uiLabelMap.PartyAreaCode}]&nbsp;</span>
              <span class="tabletext">[${uiLabelMap.PartyContactNumber}]</span>                            
          </td>        
        </tr>        
        <tr>
          <td>&nbsp;</td>
          <td>
            <@inputSubmit title=buttonText/>
          </td>
        </tr>
      </table>
    </@flexArea>
    
    <@flexArea targetId="searchById" save=false state=searchById?default(false)?string("open", "closed") enabled=false controlClassClosed="hidden" controlClassOpen="hidden" style="border:0px">
      <table class="twoColumnForm">
        <tr>
          <td class="titleCell"><span class="tableheadtext">${uiLabelMap.OpentapsFindBy}</span></td>
          <td>
            <select id="modeSwitchId" class="inputBox" onchange="switchSearchMode(this)">
              <option value="crmPartyId" ${searchById?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.CrmPartyId}</option>            
              <option value="name" ${searchByName?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.CommonName}</option>
              <option value="phoneNumber" ${searchByPhone?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.OpentapsPhoneNumber}</option>
              <option value="advanced" ${advancedSearch?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.OpentapsAdvancedSearch}</option>              
            </select>
          </td>
        </tr>
        <#if findAccounts>
          <@inputTextRow name="lookupPartyId" title=uiLabelMap.CrmAccountId />         
        <#elseif findLeads>
          <@inputTextRow name="lookupPartyId" title=uiLabelMap.CrmLeadId />        
        <#elseif findContacts>
          <@inputTextRow name="lookupPartyId" title=uiLabelMap.CrmContactId />
        <#elseif findPartners>
          <@inputTextRow name="lookupPartyId" title=uiLabelMap.OpentapsPartnerId />        
        <#else>
          <@inputTextRow name="lookupPartyId" title=uiLabelMap.CrmPartyId />        
        </#if>
        
        <tr>
          <td>&nbsp;</td>
          <td>
            <@inputSubmit title=buttonText />&nbsp;
          </td>
        </tr>
      </table>
    </@flexArea>
    
    <@flexArea targetId="advancedSearch" save=false state=advancedSearch?default(false)?string("open", "closed") enabled=false controlClassClosed="hidden" controlClassOpen="hidden" style="border:0px">
      <table class="twoColumnForm">
        <tr>
          <td class="titleCell"><span class="tableheadtext">${uiLabelMap.OpentapsFindBy}</span></td>
          <td>
            <select id="modeSwitchAdvanced" class="inputBox" onchange="switchSearchMode(this)">
              <option value="crmPartyId" ${searchById?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.CrmPartyId}</option>            
              <option value="name" ${searchByName?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.CommonName}</option>
              <option value="phoneNumber" ${searchByPhone?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.OpentapsPhoneNumber}</option>
              <option value="advanced" ${advancedSearch?default(false)?string("selected=\"selected\"", "")}>${uiLabelMap.OpentapsAdvancedSearch}</option>              
            </select>
          </td>
        </tr>

        <#-- search by party classification -->        
        <@inputSelectRow name="partyClassificationGroupId" title=uiLabelMap.CrmPartyClassification list=partyClassificationGroups displayField="description" required=false />        
          
        <#-- search by address fields -->  
        <@inputTextRow name="generalAddress" title=uiLabelMap.OpentapsAddress />        
        <@inputTextRow name="generalCity" title=uiLabelMap.PartyCity /> 
        <@inputStateCountryRow title=uiLabelMap.OpentapsCountryState titleClass="tableheadtext" required=false/>
        <@inputTextRow name="generalPostalCode" title=uiLabelMap.PartyZipCode />
                  
        <tr>
          <td>&nbsp;</td>
          <td>
            <@inputSubmit title=buttonText />&nbsp;
          </td>
        </tr>
      </table>
    </@flexArea>  
      
      

  </form>
</div>

<#if requestParameters.searchOption?has_content>
    <script type="text/javascript">
         if ('${requestParameters.searchOption}' == 'searchByName') {
              switchFromNameMode = true;      
              switchFromPhoneMode = false;
              switchFromIdMode = false;
              switchFromAdvancedMode = false;
              document.getElementById('searchOption').value = 'searchByName';          
          } else if ('${requestParameters.searchOption}' == 'searchByPhone') {
              switchFromNameMode = false;      
              switchFromPhoneMode = true;
              switchFromIdMode = false;
              switchFromAdvancedMode = false;  
              document.getElementById('searchOption').value = 'searchByPhone';
          } else if ('${requestParameters.searchOption}' == 'searchById') {
              switchFromNameMode = false;      
              switchFromPhoneMode = false;
              switchFromIdMode = true;
              switchFromAdvancedMode = false;  
              document.getElementById('searchOption').value = 'searchById';          
          } else if ('${requestParameters.searchOption}' == 'advancedSearch') {
              switchFromNameMode = false;      
              switchFromPhoneMode = false;
              switchFromIdMode = false;
              switchFromAdvancedMode = true;
              document.getElementById('searchOption').value = 'advancedSearch';                      
          }
    </script>
</#if>

</#if> <#-- if findPartyWidget?has_content -->
