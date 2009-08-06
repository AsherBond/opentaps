<#if eventType=="event.remove">
    ${partyName} ${uiLabelMap.CrmNotificationEmail_Event_RemovedFrom} <a href="${url}">${workEffortName}</a>.
<#elseif eventType=="task.remove">        
    ${partyName} ${uiLabelMap.CrmNotificationEmail_Task_RemovedFrom} <a href="${url}">${workEffortName}</a>.
</#if>