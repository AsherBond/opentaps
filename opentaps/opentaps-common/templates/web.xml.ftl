<?xml version="1.0"?>
<!DOCTYPE web-app PUBLIC "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN" "http://java.sun.com/dtd/web-app_2_3.dtd">
<!--
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
-->
<!--
 *  Copyright (c) 2001-2004, 2003 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
-->

<web-app>

    <filter>
        <filter-name>GzipFilter</filter-name>
        <display-name>GzipFilter</display-name>
        <filter-class>org.opentaps.common.event.GzipFilter</filter-class>
    </filter>
    <filter>
        <filter-name>PrivateCacheEnabler</filter-name>
        <display-name>CacheEnabler</display-name>
        <filter-class>org.opentaps.common.event.CacheEnabler</filter-class>
        <init-param>
          <param-name>Cache-Control</param-name>
          <param-value>private, max-age=15552000</param-value>
        </init-param>
    </filter>
    <filter>
        <filter-name>PublicCacheEnabler</filter-name>
        <display-name>CacheEnabler</display-name>
        <filter-class>org.opentaps.common.event.CacheEnabler</filter-class>
        <init-param>
          <param-name>Cache-Control</param-name>
          <param-value>public, max-age=15552000</param-value>
        </init-param>
    </filter>

    <filter-mapping>
        <filter-name>GzipFilter</filter-name>
        <url-pattern>*.js</url-pattern>
    </filter-mapping>
    <filter-mapping>
        <filter-name>GzipFilter</filter-name>
        <url-pattern>*.css</url-pattern>
    </filter-mapping>
    <filter-mapping>
        <filter-name>PrivateCacheEnabler</filter-name>
        <url-pattern>*.js</url-pattern>
    </filter-mapping>
    <filter-mapping>
        <filter-name>PrivateCacheEnabler</filter-name>
        <url-pattern>*.css</url-pattern>
    </filter-mapping>
    <filter-mapping>
        <filter-name>PublicCacheEnabler</filter-name>
        <url-pattern>*.gif</url-pattern>
    </filter-mapping>
    <filter-mapping>
        <filter-name>PublicCacheEnabler</filter-name>
        <url-pattern>*.jpg</url-pattern>
    </filter-mapping>
    <filter-mapping>
        <filter-name>PublicCacheEnabler</filter-name>
        <url-pattern>*.png</url-pattern>
    </filter-mapping>
    <filter-mapping>
        <filter-name>PublicCacheEnabler</filter-name>
        <url-pattern>*.html</url-pattern>
    </filter-mapping>
    <filter-mapping>
        <filter-name>PublicCacheEnabler</filter-name>
        <url-pattern>*.htm</url-pattern>
    </filter-mapping>

    <session-config>
        <session-timeout>60</session-timeout> <!-- in minutes -->
    </session-config>

</web-app>
