<%--
 * Copyright (c) Open Source Strategies, Inc.
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
--%>
<%--
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
--%>
<%-- This file may have been based on one or more Apache OFBIZ files but has been modified --%>
<%-- This file has been modified by Open Source Strategies, Inc. --%>
<%@ page import="org.ofbiz.base.util.*" %>
<html>
<head>
<title>ERROR</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
</head>

<% String errorMsg = (String) request.getAttribute("_ERROR_MESSAGE_"); %>

<body bgcolor="#FFFFFF">
<div align="center">
  <br/>
  <table width="100%" border="1" height="200">
    <tr>
      <td>
        <table width="100%" border="0" height="200">
          <tr bgcolor="#CC6666"> 
            <td height="45"> 
              <div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="4" color="#FFFFFF"><b>:ERROR MESSAGE:</b></font></div>
            </td>
          </tr>
          <tr> 
            <td>
              <div align="left"><span style="font: 10pt Courier"><%=UtilFormatOut.replaceString(errorMsg, "\n", "<br/>")%></span></div>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</div>
<div align="center"></div>
</body>
</html>
