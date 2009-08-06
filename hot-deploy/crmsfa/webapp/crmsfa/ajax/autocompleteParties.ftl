<#-- code to generate the dropdown autocomplete list goes here -->

<#if requestAttributes.autocompleteCandidates?exists>
<ul>
  <#list requestAttributes.autocompleteCandidates as party>
  <li entityId="${party.partyId}"><#compress>${party.groupName?if_exists} ${party.firstName?if_exists} ${party.lastName?if_exists}</#compress> (${party.partyId})</li>
  </#list>
</ul>
</#if>
