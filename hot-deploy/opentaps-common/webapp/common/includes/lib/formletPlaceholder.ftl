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

<#-- 
This file stores the definition of the formlet placeholder.

When a pagination macro is encountered, what really happens is that
this file gets evaluated instead of the nested contents of the
pagination macro.  It is a placeholder that creates an AJAX request
to fetch the actual list of data after the user receives the entire
screen on her web browser.

Note that the div with id paginate_${paginatorName} will get replaced.        
-->

<div id="paginate_${paginatorName}">
  <script type="text/javascript">
    opentaps.addOnLoad(function(){opentaps.getCurrentPage('${paginatorName}','${opentapsApplicationName}')});
  </script>
</div>