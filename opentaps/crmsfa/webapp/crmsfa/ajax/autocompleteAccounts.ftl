<#-- code to generate the dropdown autocomplete list goes here -->

<#if requestAttributes.accounts?exists>
<ul>
  <#list requestAttributes.accounts as account>
  <li accountPartyId="${account.partyId}">${account.groupName}</li>
  </#list>
</ul>
</#if>
