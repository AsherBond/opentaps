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

<#compress>

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta http-equiv="Content-Script-Type" content="text/javascript"/>
        <meta http-equiv="Content-Style-Type" content="text/css"/>
        <title>${uiLabelMap.Example}</title>
        <link rel="stylesheet" href="https://localhost:8443/opentaps_css/opentaps.css" type="text/css"/>
    </head>
<body>

<div class="tabletext" style="margin-bottom: 30px;"/>
    <#assign sitemap = catalogMap.ProductCatalog.store>
    <#recurse sitemap>

	<#macro catalog>
		<p><h2>${.node.@name?html} (${.node.@id})</h2></p>
		<#recurse>
	</#macro>
	
    <#macro category>
        <p><b><#if .node.@sequence?has_content> ${.node.@sequence} - </#if><a href="${categoryLink}${.node.@id}" title="Number of Products ${.node.@numberOfProducts?if_exists}">${.node.@name?html}</a></b>
	        <ul class="bulletList">
            <#recurse>
	        </ul>
        </p>
    </#macro>

    <#macro product>
        <li><#if .node.@sequence?has_content> ${.node.@sequence} - </#if> <a href="${productLink}${.node.@id}">${.node.@name?html}</li>
    </#macro>

</div>

</body>
</html>    

</#compress>
