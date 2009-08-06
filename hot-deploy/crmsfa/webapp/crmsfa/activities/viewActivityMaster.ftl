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

<#-- This basically renders a special screen for emails vs other activities -->

<#-- TODO: re-factor all of ActivitiesScreens.xml#viewActivity into this FTL from the screen widget -->

<#if (workEffort?has_content) && (workEffort.workEffortPurposeTypeId?has_content) && (workEffort.workEffortPurposeTypeId == "WEPT_TASK_EMAIL")>
    ${screens.render("component://crmsfa/widget/crmsfa/screens/activities/ActivitiesScreens.xml#viewEmailActivity")}
<#else>
    ${screens.render("component://crmsfa/widget/crmsfa/screens/activities/ActivitiesScreens.xml#viewActivity")}
</#if>