
<#-- Initialization -->

<#assign uiLabelMap = Static["org.opentaps.common.util.UtilMessage"].getUiLabels(locale)>
<#if ! uiLabelMap?has_content><#assign uiLabelMap = {}/></#if>
if (! uiLabelMap) var uiLabelMap = {};
if (! configProperties) var configProperties = {};

<#-- Properties -->
configProperties.bgColor = parent.bgColor ? parent.bgColor : '000099'; // Inherited from the header FTL

<#-- Labels -->

uiLabelMap.OpentapsAccept = '${uiLabelMap.OpentapsAccept?js_string?if_exists}';

