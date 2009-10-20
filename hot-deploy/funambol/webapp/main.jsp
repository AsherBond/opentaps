<!--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 * 
 * Opentaps is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opentaps is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Opentaps.  If not, see <http://www.gnu.org/licenses/>.
-->
<!--
 This file was copied from Funambol DSS, it did not have a copyright notice.
 Altered by Cameron Smith - Database, Lda - www.database.co.mz, on behalf of Open Source Strategies, Inc.
-->	
<% 
   String main = request.getParameter("main"); 
   if(main == null || main.equals(""))
   {
%>
 <jsp:forward page='index.html'/>
<%
   }
%> 

<html>
 <head>
  <title>Opentaps Funambol Module</title>
  <link rel="stylesheet" href="css/home.css" type="text/css">
 </head>

 <body>
  <table border="0" cellspacing="0" cellpadding="0" width="100%">
   <tbody>
    <tr>
     <td valign="top" align="center">
      <div align="center">
	   <a href="/crmsfa/control/main">Opentaps</a> Funambol Module<br/><br/>
		
	   <a href="http://www.opensourcestrategies.com">Open Source Strategies, Inc.</a><br/>
	   Powered by:	  
	   <a href="http://www.funambol.com"><img src="imgs/funambol.gif" alt="Funambol Data Synchronization Server" width="146" height="118" border="0"></a>
     </td>
    </tr>
    <jsp:include page='/admin/version.html'/>
   </tbody>  
  </table> 
 </body>
</html>
