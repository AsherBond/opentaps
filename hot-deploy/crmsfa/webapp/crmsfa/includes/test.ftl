<#-- test ftl accessed at /crmsfa/control/test.  The bsh for this is webapp/crmsfa/WEB-INF/actions/includes/test.bsh -->
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div class="subSectionBlock">

    
<@paginate name="statusItems" list=statusListBuilder>
<#noparse>
  <@navigationHeader title="Demo of Entity List Builder" />
  <table class="listTable">
    <#list pageRows as row>
      <tr class="${tableRowClass(row_index)}"><td width="50%">${row.description}</td><td width="50%">${row.getRelatedOneCache("StatusType").description}</td></tr>
    </#list>
  </table>
</#noparse>
</@paginate>


</div>
<div class="subSectionBlock">


<@paginate name="supplierProduct" list=productListBuilder>
<#noparse>
  <@navigationHeader title="Demo of Page Builder" />
  <table class="listTable">
    <tr class="listTableHeader">
        <@headerCell title=uiLabelMap.ProductSupplier orderBy="partyId,productId" />
        <@headerCell title=uiLabelMap.ProductProduct orderBy="productId,partyId" />
        <td>${uiLabelMap.ProductInternalName}</td>
        <@headerCell title=uiLabelMap.ProductMinimumOrderQuantity orderBy="minimumOrderQuantity,partyId,productId" />
        <@headerCell title=uiLabelMap.ProductLastPrice orderBy="lastPrice,partyId,productId" blockClass="textright"/>
    </tr>
    <#list pageRows as row>
      <tr class="${tableRowClass(row_index)}">
          <@displayCell text=row.supplierName />
          <@displayCell text=row.productId />
          <@displayCell text=row.internalName />
          <@displayCell text=row.minimumOrderQuantity />
          <@displayCurrencyCell amount=row.lastPrice />
      </tr>
    </#list>
  </table>
</#noparse>
</@paginate>


</div>
<div class="subSectionBlock">


<@paginate name="glAccounts" list=glAccountBuilder>
<#noparse>
  <@navigationHeader title="Demo of BSH Closures" />
  <table class="listTable">
    <tr class="listTableHeader">
        <@headerCell title="GL Account Code" orderBy="accountCode" />
        <@headerCell title="Name" orderBy="accountName" />
        <@headerCell title="Posted Balance" orderBy="accountPostedBalance" blockClass="textright" />
    </tr>
    <#list pageRows as row>
      <tr class="${tableRowClass(row_index)}">
          <@displayCell text=row.accountCode />
          <@displayCell text=row.description />
          <@displayCurrencyCell amount=row.accountPostedBalance />
      </tr>
    </#list>
  </table>
</#noparse>
</@paginate>


</div>


<div class="subSectionBlock">
<input type="button" value="Clear CRMSFA Formlet Cache" class="smallSubmit"
       onClick="javascript:opentaps.sendRequest('/webtools/control/FindUtilCacheClear', {'UTIL_CACHE_NAME':'opentaps.formlet.crmsfa','externalLoginKey':'${externalLoginKey}'})" >
</div>