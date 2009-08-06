<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
<#if surveyWrapper?has_content>
<form method="POST" action="<@ofbizUrl>${surveyAction?default('/createResponse')}</@ofbizUrl>">
<#-- note this must point to the exact file system location not just the component name in ofbiz-component.xml -->
  ${surveyWrapper.render("/hot-deploy/crmsfa/templates/survey/genericsurvey.ftl").toString()}
</form>
<#else>
<h2>${uiLabelMap.CrmSurveyDoesNotExist}</h2>
</#if>
