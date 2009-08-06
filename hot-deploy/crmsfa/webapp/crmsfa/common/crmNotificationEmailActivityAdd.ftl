<#if eventType=="event.add">
    ${partyName} ${uiLabelMap.CrmNotificationEmail_Event_AddedTo} <a href="${url}">${workEffortName}</a>.
<#elseif eventType=="task.add">        
    ${partyName} ${uiLabelMap.CrmNotificationEmail_Task_AddedTo} <a href="${url}">${workEffortName}</a>.
</#if>