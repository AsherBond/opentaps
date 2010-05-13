<#if ! amazonProduct?exists>
  <#assign amazonProduct = {}/>
</#if>
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<script type="text/javascript">

    opentaps.addOnLoad(function() {
        opentaps.addListenerToNode(document.getElementById('nodeId'), 'change', function(){checkValidBrowseNodeValues(true)});
        opentaps.addListenerToNode(document.getElementById('itemTypeId'), 'change', function(){checkValidBrowseNodeValues(document.getElementById('itemTypeId').value == '')});
    });
    <#if ! amazonProduct.nodeId?has_content>
        opentaps.addOnLoad(function() {
            opentaps.hide(document.getElementById('itemTypeId'));
            opentaps.insertBefore(opentaps.createSpan("noItemTypeId", "${uiLabelMap.AmazonProductSelectBrowseNode?js_string}", "tabletext"), document.getElementById('itemTypeId'));
        });
    </#if>
    <#if ! amazonProduct.itemTypeId?has_content>
        opentaps.addOnLoad(function() {
            opentaps.hide(document.getElementById('usedForId'));
            opentaps.insertBefore(opentaps.createSpan("noUsedFor", "${uiLabelMap.AmazonProductSelectBrowseNodeAndItemType?js_string}", "tabletext"), document.getElementById('usedForId'));
            opentaps.hide(document.getElementById('targetAudienceId'));
            opentaps.insertBefore(opentaps.createSpan("noTargetAudience", "${uiLabelMap.AmazonProductSelectBrowseNodeAndItemType?js_string}", "tabletext"), document.getElementById('targetAudienceId'));
            opentaps.hide(document.getElementById('otherItemAttrId'));
            opentaps.insertBefore(opentaps.createSpan("noOtherItemAttrId", "${uiLabelMap.AmazonProductSelectBrowseNodeAndItemType?js_string}", "tabletext"), document.getElementById('otherItemAttrId'));
        });
    </#if>

    function checkValidBrowseNodeValues(/* boolean */ replaceItemTypes) {
        var browseNodeSelect = document.getElementById('nodeId');
        var itemTypeSelect = document.getElementById('itemTypeId');
        var itemTypeId = itemTypeSelect.value;
        if (replaceItemTypes) {
            replaceSelect('itemTypeId', {});
            opentaps.hide(document.getElementById('itemTypeId'));
            if (! document.getElementById('noItemTypeId')) opentaps.insertBefore(opentaps.createSpan("noItemTypeId", "${uiLabelMap.AmazonProductSelectBrowseNode?js_string}", "tabletext"), document.getElementById('itemTypeId'));
        }
        replaceSelect('usedForId', {});
        opentaps.hide(document.getElementById('usedForId'));
        replaceSelect('targetAudienceId', {});
        opentaps.hide(document.getElementById('targetAudienceId'));
        replaceSelect('otherItemAttrId', {});
        opentaps.hide(document.getElementById('otherItemAttrId'));
        if (browseNodeSelect.value != '') opentaps.sendRequest('getValidAmazonBrowseNodeValues', {'nodeId' : browseNodeSelect.value, 'itemTypeId' : itemTypeId}, function(data){updateBrowseNodeValues(replaceItemTypes, data)});
        if (! document.getElementById('noUsedFor')) opentaps.insertBefore(opentaps.createSpan("noUsedFor", "${uiLabelMap.AmazonProductSelectBrowseNodeAndItemType?js_string}", "tabletext"), document.getElementById('usedForId'));
        if (! document.getElementById('noTargetAudience')) opentaps.insertBefore(opentaps.createSpan("noTargetAudience", "${uiLabelMap.AmazonProductSelectBrowseNodeAndItemType?js_string}", "tabletext"), document.getElementById('targetAudienceId'));
        if (! document.getElementById('noOtherItemAttrId')) opentaps.insertBefore(opentaps.createSpan("noOtherItemAttrId", "${uiLabelMap.AmazonProductSelectBrowseNodeAndItemType?js_string}", "tabletext"), document.getElementById('otherItemAttrId'));
    }

    function updateBrowseNodeValues(/* boolean */ replaceItemTypes, /* Array */ data) {
        if (! data) return;

        if (replaceItemTypes) {
            var itemTypeOptions = [];
            for (var x = 0; x < data['itemTypes'].length; x++) {
                itemTypeOptions[x] = {'text' : data['itemTypes'][x].description, 'value' : data['itemTypes'][x].itemTypeId};
            }
            replaceSelect('itemTypeId', itemTypeOptions);
            opentaps.removeNode(document.getElementById('noItemTypeId'));
            opentaps.addListenerToNode(document.getElementById('itemTypeId'), 'change', function(){checkValidBrowseNodeValues(document.getElementById('itemTypeId').value == '')});
        } else {
            var usedForOptions = [];
            for (var x = 0; x < data['USED_FOR'].length; x++) {
                usedForOptions[x] = {'text' : data['USED_FOR'][x].description, 'value' : data['USED_FOR'][x].usedForId};
            }
            replaceSelect('usedForId', usedForOptions, true);
            opentaps.removeNode(document.getElementById('noUsedFor'));
            var targetAudOptions = [];
            for (var x = 0; x < data['TARGET_AUDIENCE'].length; x++) {
                targetAudOptions[x] = {'text' : data['TARGET_AUDIENCE'][x].description, 'value' : data['TARGET_AUDIENCE'][x].targetAudienceId};
            }
            replaceSelect('targetAudienceId', targetAudOptions, true);
            opentaps.removeNode(document.getElementById('noTargetAudience'));
            var otherItemAttrOptions = [];
            for (var x = 0; x < data['OTHER_ITEM_ATTR'].length; x++) {
                otherItemAttrOptions[x] = {'text' : data['OTHER_ITEM_ATTR'][x].description, 'value' : data['OTHER_ITEM_ATTR'][x].otherItemAttrId};
            }
            replaceSelect('otherItemAttrId', otherItemAttrOptions, true);
            opentaps.removeNode(document.getElementById('noOtherItemAttrId'));
        }
    }
    
    function replaceSelect(/* String */ selectId, /* Array */ newOptions, /* boolean */ multiple) {
        var oldSelect = document.getElementById(selectId);
        if (! oldSelect) return;
        var newSelect = document.createElement('select');
        newSelect.id = selectId;
        newSelect.name = selectId;
        newSelect.className = "inputBox";
        if (multiple) {
            newSelect.multiple = 'multiple';
            newSelect.size = 5;
            newSelect.style.height = 'auto';
        }
        if (! multiple) {
            newSelect.options[0] = new Option('', '', true, true);
        }
        for (x = 0; x < newOptions.length; x++) {
            var y = multiple ? x : x + 1 ;
            newSelect.options[y] = new Option(newOptions[x].text, newOptions[x].value, false, false);
        }
        opentaps.replaceNode(oldSelect, newSelect);
    }
</script>

<@inputDateTimeRow title="${uiLabelMap.AmazonProductListReleaseDate}" name="releaseDate" default=amazonProduct.releaseDate form="updateProductForm"/>
<@inputTextRow title="${uiLabelMap.AmazonProductTaxCode}" name="productTaxCode" default=amazonProduct.productTaxCode size="50" maxlength="50"/>
<@inputTextRow title="${uiLabelMap.CommonPriority}" name="priority" default=amazonProduct.priority size="2" maxlength="20"/>
<@inputIndicatorRow title="${uiLabelMap.AmazonProductBrowseExclusion}" name="browseExclusion" default=amazonProduct.browseExclusion required=false/>
<@inputIndicatorRow title="${uiLabelMap.AmazonProductRecommendationExclusion}" name="recommendationExclusion" default=amazonProduct.recommendationExclusion required=false/>
<@inputTextRow title="${uiLabelMap.AmazonProductTier}" name="tier" default=amazonProduct.tier size="5"/>
<@inputTextRow title="${uiLabelMap.AmazonProductPurchasingCategory}" name="purchasingCategory" default=amazonProduct.purchasingCategory size="50" maxlength="50"/>
<@inputTextRow title="${uiLabelMap.AmazonProductPurchasingSubCategory}" name="purchasingSubCategory" default=amazonProduct.purchasingSubCategory size="50" maxlength="50"/>
<@inputTextRow title="${uiLabelMap.AmazonProductPackagingType}" name="packagingType" default=amazonProduct.packagingType size="50" maxlength="50"/>
<tr>
    <@displayCell text="${uiLabelMap.AmazonProductUnderlAvailability}" blockClass="titleCellTop" class="tableheadtext"/>
    <#assign underlAvail = ["backordered", "manufacturer-out-of-stock", "pre-ordered", "2-3-days", "1-2-weeks", "4-6-weeks"]/>
    <td>
        <select name="underlyingAvailability" class="inputBox">
        <option value=""></option>
        <#list underlAvail as item>
            <#if item == amazonProduct.underlyingAvailability?default("")>
            <option value="${item}" selected>${item}</option>
            <#else>
            <option value="${item}">${item}</option>
            </#if>
        </#list>
        </select>
    </td>
</tr>
<tr>
    <@displayCell text="${uiLabelMap.AmazonProductReplenishmentCategory}" blockClass="titleCellTop" class="tableheadtext"/>
    <#assign replCat = ["basic-replenishment", "limited-replenishment", "manufacturer-out-of-stock", "new-product", "non-replenishable", "non-stockupable", "obsolete", "planned-replenishment"]/>
    <td>
        <select name="replenishmentCategory" class="inputBox">
        <option value=""></option>
        <#list replCat as item>
            <#if item == amazonProduct.replenishmentCategory?default("")>
            <option value="${item}" selected>${item}</option>
            <#else>
            <option value="${item}">${item}</option>
            </#if>
        </#list>
        </select>
    </td>
</tr>
<tr>
    <@displayCell text="${uiLabelMap.AmazonProductDropShipStatus}" blockClass="titleCellTop" class="tableheadtext"/>
    <#assign dropShip = ["drop-ship-disabled", "drop-ship-disabled-by-buyer", "drop-ship-enabled", "drop-ship-enabled-no-auto-pricing", "drop-ship-only"]/>
    <td>
        <select name="dropShipStatus" class="inputBox">
        <option value=""></option>
        <#list dropShip as item>
            <#if item == amazonProduct.dropShipStatus?default("")>
            <option value="${item}" selected>${item}</option>
            <#else>
            <option value="${item}">${item}</option>
            </#if>
        </#list>
        </select>
    </td>
</tr>
<tr>
    <@displayCell text="${uiLabelMap.AmazonProductOoSMessage}" blockClass="titleCellTop" class="tableheadtext"/>
    <#assign outOfStockMsg = ["email-me-when-available", "out-of-stock", "pre-order-ute", "underlying-availability"]/>
    <td>
        <select name="outOfStockWebsiteMessage" class="inputBox">
        <option value=""></option>
        <#list outOfStockMsg as item>
            <#if item == amazonProduct.outOfStockWebsiteMessage?default("")>
            <option value="${item}" selected>${item}</option>
            <#else>
            <option value="${item}">${item}</option>
            </#if>
        </#list>
        </select>
    </td>
</tr>
<tr>
    <@displayCell text="${uiLabelMap.AmazonProductRegisteredParameter}" blockClass="titleCellTop" class="tableheadtext"/>
    <#assign regParam = ["PrivateLabel", "Specialized", "NonConsumer", "PreConfigured"]/>
    <td>
        <select name="registeredParameter" class="inputBox">
        <option value=""></option>
        <#list regParam as item>
            <#if item == amazonProduct.registeredParameter?default("")>
            <option value="${item}" selected>${item}</option>
            <#else>
            <option value="${item}">${item}</option>
            </#if>
        </#list>
        </select>
    </td>
</tr>

<#assign itemTypeId = amazonProduct.itemTypeId?default("")/>
<@inputSelectRow title=uiLabelMap.AmazonProductBrowseNode name="nodeId" list=browseNodes?sort_by("description") key="nodeId" displayField="description" default=amazonProduct?default({}).nodeId required=false/>
<@inputSelectRow title=uiLabelMap.AmazonProductItemType name="itemTypeId" list=itemTypes?default([])?sort_by("description") key="itemTypeId" displayField="description" default=itemTypeId required=false/>
<#if itemTypeId?has_content>
  <@inputMultiSelectRow title=uiLabelMap.AmazonProductUsedFor name="usedForId" list=usedFor?default([])?sort_by("usedForId") key="usedForId" displayField="description" default=productUsedForIds/>
  <@inputMultiSelectRow title=uiLabelMap.AmazonProductTargetAudience name="targetAudienceId" list=targetAudience?default([])?sort_by("targetAudienceId") key="targetAudienceId" displayField="description" default=productTargetAudienceIds/>
  <@inputMultiSelectRow title=uiLabelMap.AmazonProductOtherItemAttr name="otherItemAttrId" list=otherItemAttributes?default([])?sort_by("otherItemAttrId") key="otherItemAttrId" displayField="description" default=productOtherItemAttrIds/>
<#else>
  <@inputSelectRow title=uiLabelMap.AmazonProductUsedFor name="usedForId" list=[]/>
  <@inputSelectRow title=uiLabelMap.AmazonProductTargetAudience name="targetAudienceId" list=[]/>
  <@inputSelectRow title=uiLabelMap.AmazonProductOtherItemAttr name="otherItemAttrId" list=[]/>
</#if>
<#list 1..productFeedMaxBulletPoints as x>
  <#assign value = (bulletPoints?default([])?size < x)?string("", bulletPoints?default([])[x - 1]?default({}).description?default(""))/>
  <@inputTextRow title=uiLabelMap.AmazonProductBulletPoint name="bulletPoint" default=value/>
</#list>
<#list 1..productFeedMaxSearchTerms as x>
  <#assign value = (searchTerms?default([])?size < x)?string("", searchTerms?default([])[x - 1]?default({}).description?default(""))/>
  <@inputTextRow title=uiLabelMap.AmazonProductSearchTerm name="searchTerm" default=value/>
</#list>
