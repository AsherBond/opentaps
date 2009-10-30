
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<script type="text/javascript">
  var dateString = '${getLocalizedDate(Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp())}';
  var dateFormat = '${StringUtil.wrapString(Static["org.ofbiz.base.util.UtilDateTime"].getJsDateTimeFormat(Static["org.ofbiz.base.util.UtilDateTime"].getDateTimeFormat(locale)))}';
  var formatedDate = opentaps.formatDate(opentaps.parseDate(dateString, dateFormat), dateFormat);
</script>

<div class="subSectionBlock">
    <table class="twoColumnForm">
        <@displayRow text="${userTimeZone}" title="My TimeZone"/>
        <@displayRow text="${defaultTimeZone}" title="Default TimeZone"/>
        <@displayDateRow date=baseTimestamp format="DATE_TIME" title="Base timestamp"/>
        <@displayRow text="${getLocalizedDate(baseTimestamp)}" title="After TZ is applied"/>
    </table>
</div>

<div class="subSectionBlock">
    <form name="testCalendarForm" method="post" action="<@ofbizUrl>testDateTimeInput</@ofbizUrl>">
    <table class="twoColumnForm">
        <@inputTextRow name="sampleTimestamp" title="Test input"/>
        <@inputSubmitRow title="Submit"/>
    </table>
    </form>
</div>

<div class="subSectionBlock">
    <form name="testCalendarForm1" method="post" action="<@ofbizUrl>testDateTimeInput</@ofbizUrl>">
    <table class="twoColumnForm">
        <@inputDateRow name="sampleTimestamp" title="Test Date macro"/>
        <@inputSubmitRow title="Submit"/>
    </table>
    </form>
</div>

<div class="subSectionBlock">
    <form name="testCalendarForm2" method="post" action="<@ofbizUrl>testDateTimeInput</@ofbizUrl>">
    <table class="twoColumnForm">
        <@inputDateTimeRow name="sampleTimestamp" title="Test DateTime macro"/>
        <@inputSubmitRow title="Submit"/>
    </table>
    </form>
</div>