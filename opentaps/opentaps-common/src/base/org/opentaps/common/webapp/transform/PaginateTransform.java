/*
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
 */

package org.opentaps.common.webapp.transform;

import freemarker.core.Environment;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.TemplateTransformModel;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.collections.MapStack;
import org.opentaps.common.builder.FormletFactory;
import org.opentaps.common.builder.ListBuilderException;
import org.opentaps.common.pagination.Paginator;
import org.opentaps.common.pagination.PaginatorFactory;
import org.opentaps.common.template.freemarker.FreemarkerUtil;
import org.opentaps.common.util.UtilMessage;

import javax.servlet.http.HttpSession;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;

/**
 * Handles <@paginate> transforms.
 */
public class PaginateTransform implements TemplateTransformModel {

    public static final String module = PaginateTransform.class.getName();

    public Writer getWriter(final Writer out, Map args) {
        Environment env = Environment.getCurrentEnvironment();
        Locale locale = env.getLocale();

        try {
            // grab the session or abort
            Map context = FreemarkerUtil.getMap(env, "context");
            HttpSession session = (HttpSession) context.get("session");
            if (session == null) {
                Debug.logError("Paginate macro error:  No session available.  User session probably expired.", module);
                return UtilMessage.getErrorWriter(out, locale);
            }

            // get the list object, whatever it might be, and if it is null, then don't do anything
            Object list = FreemarkerUtil.getObject(env, "list");
            if (isNull(list)) return new NullWriter();

            // get the paginator name, which is guaranteed by the wrapper macro
            String paginatorName = FreemarkerUtil.getString(env, "name");
            if (paginatorName.indexOf('.') != -1) {
                Debug.logError("Paginate macro error: Must not use periods in name argument for <@paginate>.  The name given is " + paginatorName, module);
                return UtilMessage.getErrorWriter(out, locale);
            }

            // and the settings
            boolean rememberPage = FreemarkerUtil.getBoolean(env, "rememberPage", true);
            boolean rememberOrderBy = FreemarkerUtil.getBoolean(env, "rememberOrderBy", true);
            boolean renderExcelButton = FreemarkerUtil.getBoolean(env, "renderExcelButton", true);

            // and the application name, which should be in the context
            String applicationName = (String) context.get("opentapsApplicationName");

            // and also the list of parameters that will be passed on to the formlet
            Map params = FreemarkerUtil.getMap(env, "params");

            Paginator paginator = PaginatorFactory.getPaginator(session, paginatorName, applicationName);
            if (paginator == null) {
                // create a fresh new paginator, because this is the first visit for the session
                paginator = PaginatorFactory.createPaginatorForTransform(paginatorName, list, context, params, rememberPage, rememberOrderBy, renderExcelButton);
            } else {
                // user has come back to the page, so update paginator if required (cached lists might be rebuilt, for instance)
                PaginatorFactory.updatePaginatorForTransform(paginator, list, context, params, rememberPage, rememberOrderBy, renderExcelButton);
            }

            return FormletFactory.getFormletWriter(out, paginator, applicationName, locale);
        } catch (ListBuilderException e) {
            return UtilMessage.getErrorWriter(out, e, locale, module);
        } catch (TemplateModelException e) {
            return UtilMessage.getErrorWriter(out, e, locale, module);
        }
    }

    // this is a hack, if we're passed a "" string from the internal macro then we know nothing has been passed
    private static boolean isNull(Object list) throws TemplateModelException {
        if (list == null) return true;
        if (list instanceof TemplateSequenceModel) {
            if (((TemplateSequenceModel) list).size() == 0) return true;
        }
        return false;
    }

    public class NullWriter extends Writer {
        public void write(char[] buff, int len, int off) {}
        public void flush() {}
        public void close() {}
    }
}
