package org.opentaps.common.builder;

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

import freemarker.cache.CacheStorage;
import freemarker.cache.StringTemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import javolution.util.FastMap;
import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.opentaps.common.pagination.Paginator;
import org.opentaps.common.template.freemarker.FreemarkerUtil;
import org.opentaps.common.util.UtilConfig;
import org.opentaps.common.util.UtilMessage;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Manages the creation, caching, retrieval and rendering of formlet templates.
 * A formlet template is a regular freemarker template that is constructed
 * from the union of several templates.
 *
 * Rendering involves fetching the template corresponding to the paginator and
 * populating a context with data such as the pagination results, the security
 * object, the session, and so on.
 *
 * The most important template is one that is constructed from the contents
 * of a <@paginate> macro call.  This template requires access to the
 * opentaps macro libraries, so as a convenience, the libraries are
 * appended to this template.
 *
 * Stand alone templates that are not defined within <@paginate> are also possible,
 * but they are not currently implemented or supported.
 *
 * To specify what libraries get loaded as part of each formlet template,
 * specify in the ${applicationName}.properties file the following,
 *
 * opentaps.ftl.library.${libraryName} = component://path/to/library.ftl
 *
 * The libraries defined in opentaps.properties will also be loaded, so there
 * is usually no need to modify these unless you want to add an extra library
 * of macros for use by the formlets.
 */
public class FormletFactory {

    public static final String module = FormletFactory.class.getName();

    // each application has a separate Freemarker configuration (clearing this cache reloads libraries)
    protected static UtilCache configurationCache = UtilCache.createUtilCache("opentaps.ftl.configuration", 0, 0);

    // each application has a FormletCache, we need to hold references to them here
    protected static Map applicationFormletCache = FastMap.newInstance();

    // store the library files as strings in this cache (clearing this cache reloads the libraries)
    protected static UtilCache applicationLibraryCache = UtilCache.createUtilCache("opentaps.ftl.library", 0, 0);

    // freemarker configuration and loader for the formlet placeholder
    protected static Configuration formletPlaceholderConfig = null;

    // location of the placeholder ftl
    protected static final String formletPlaceholderLocation = "component://opentaps-common/webapp/common/includes/lib/formletPlaceholder.ftl";


    /*************************************************************************/
    /**                                                                     **/
    /**                       Formlet Cache System                          **/
    /**                                                                     **/
    /*************************************************************************/


    /**
     * Cache implementation that seamlessly integrates ofbiz caching with freemarker caching.  Each application
     * has a different FormletCache, and the freemarker Configuration uses this cache to store data.
     *
     * In the ofbiz caching system, each cache has an identifier.  The identifier for this class is
     * "opentaps.formlet.${applicationName}".  The cache page in webtools will show this line and allow the
     * user to clear the cache.
     *
     * Freemarker uses this cache to store templates using a special key object called a TemplateKey.
     * If the template is not there, then it will create it with the appropriate loader.  Note that
     * when this cache is cleared, it does not clear the loader or any other template configurations,
     * just the templates identified with TemplateKey.
     *
     * However, we also store the completed templates keyed to the paginatorName in this cache.  This is
     * orthogonal to how freemarker uses the cache, so it is safe.
     */
    public static class FormletCache implements CacheStorage {

        protected UtilCache formletCache;

        public FormletCache(String applicationName) {
            String cacheName = "opentaps.formlet." + applicationName;
            formletCache = UtilCache.findCache(cacheName);
            if (formletCache == null) {
                formletCache = UtilCache.createUtilCache("opentaps.formlet." + applicationName, 0, 0);
            }
        }

        public Object get(Object key) {
            return formletCache.get(key);
        }

        public void put(Object key, Object value) {
            formletCache.put(key, value);
        }

        public void remove(Object key) {
            formletCache.remove(key);
        }

        public void clear() {
            formletCache.clear();
        }
    }

    /** Gets the application's FormletCache and creates it in a thread safe way if it doesn't exist. */
    public static FormletCache getFormletCache(String applicationName) {
        synchronized (applicationFormletCache) {
            FormletCache cache = (FormletCache) applicationFormletCache.get(applicationName);
            if (cache == null) {
                cache = new FormletCache(applicationName);
                applicationFormletCache.put(applicationName, cache);
            }
            return cache;
        }
    }

    public static boolean isFormletCached(String paginatorName, String applicationName) {
        FormletCache cache = (FormletCache) applicationFormletCache.get(applicationName);
        if (cache == null) return false;
        return (cache.get(paginatorName) != null);
    }

    // join the libraries into one big string and store on a per application basis
    private static void appendLibrary(String applicationName, String libraryString) {
        if (UtilValidate.isEmpty(libraryString)) return;
        String currentString = (String) applicationLibraryCache.get(applicationName);
        if (UtilValidate.isEmpty(currentString)) {
            applicationLibraryCache.put(applicationName, libraryString);
        } else {
            applicationLibraryCache.put(applicationName, currentString + libraryString);
        }
    }

    // gets the library string from cache and load the libraries if not yet done
    private static String getLibraryString(String applicationName) {
        synchronized(applicationLibraryCache) {
            String library = (String) applicationLibraryCache.get(applicationName);
            if (library != null) return library;

            loadLibraries(applicationName);
        }

        String library = (String) applicationLibraryCache.get(applicationName);
        if (library == null) {
            Debug.logWarning("Could not load ftl libraries for application ["+applicationName+"].  Formlets will not have access to macros.", module);
            library = "";
        }
        return library;
    }


    /** Retrieves the freemarker configuration for an application, or builds it if it does not exist yet. */
    public static Configuration getConfiguration(String applicationName) throws TemplateException {
        synchronized (configurationCache) {
            Configuration config = (Configuration) configurationCache.get(applicationName);
            if (config == null) {
                config = createConfiguration(applicationName);
                configurationCache.put(applicationName, config);
            }
            return config;
        }
    }

    // Get the placeholder formlet configuration, or build it if it does not exist yet (can't synch on null, so hopefully that doesn't matter)
    private static Configuration getFormletPlaceholderConfiguration() throws TemplateException, IOException {
        if (formletPlaceholderConfig == null) {
            // this config doesn't need an integration with ofbiz cache, so let it use freemarker's default
            formletPlaceholderConfig = FreemarkerUtil.createDefaultConfiguration();
            StringTemplateLoader loader = new StringTemplateLoader();
            loader.putTemplate("formletPlaceholder", getTemplateFromLocationAsString(formletPlaceholderLocation));
            formletPlaceholderConfig.setTemplateLoader(loader);
        }
        return formletPlaceholderConfig;
    }


    /*************************************************************************/
    /**                                                                     **/
    /**                        Formlet Writer                               **/
    /**                                                                     **/
    /*************************************************************************/


    /**
     * Writer that evaluates and prints out a special formlet template.
     * This template is used when the user loads a page afresh (e.g.,
     * whenever the paginate macro is invoked).
     *
     * The special template contains a call to a macro defined in
     * opentapsFormMacros.  This macro is responsiblefor generating
     * a placeholder box on screen for the contents of a page.
     * It will also render the spinner image to inform the user the
     * list is loading.  Then it renders an AJAX function that
     * will make a request to fetch the current page from the paginator.
     */
    public static class FormletWriter extends Writer {
        protected Writer out;
        protected String paginatorName;
        protected String applicationName;
        protected long viewSize;
        protected Locale locale;
        protected boolean rendered; // special boolean to help determine if we rendered already

        public FormletWriter(Writer out, String paginatorName, String applicationName, long viewSize, Locale locale) {
            this.out = out;
            this.paginatorName = paginatorName;
            this.applicationName = applicationName;
            this.viewSize = viewSize;
            this.locale = locale;
            rendered = false;
        }

        // we render here because the flush doesn't seem to be called when content is <#nested>
        public void write(char[] buff, int len, int offset) throws IOException {
            if (! rendered) {
                renderFormletPlaceholder(out, paginatorName, applicationName, viewSize, locale);
                rendered = true;
            }
        }

        // and if it somehow gets here, we have our rendered boolean to help
        public void flush() throws IOException {
            if (! rendered) renderFormletPlaceholder(out, paginatorName, applicationName, viewSize, locale);
            out.flush();
        }

        public void close() throws IOException {
        }
    }

    /**
     * This writer has the same output functionality as FormletWriter.
     * It has an additional responsibility of reading the nested content
     * of the <@paginate> macro and transforming it into a formlet template.
     * The idea is if the formlet is not cached yet and needs to be created,
     * this writer will be used instead of FormletWriter.
     */
    public static class FormletTemplateWriter extends FormletWriter {
        StringBuffer buff;

        public FormletTemplateWriter(Writer out, String paginatorName, String applicationName, long viewSize, Locale locale) {
            super(out, paginatorName, applicationName, viewSize, locale);
            buff = new StringBuffer();
        }

        public void write(char[] buff, int len, int offset) throws IOException {
            this.buff.append(buff, len, offset);

            if (! rendered) {
                // when the content is wrapped in <#nested>, it gets passed as a single write and flush is never called, so we need to do this here
                FormletFactory.createAndCacheFormletTemplate(this.buff, paginatorName, applicationName);
                renderFormletPlaceholder(out, paginatorName, applicationName, viewSize, locale);
                rendered = true;
            }
        }
    }

    /**
     * Determines which writer to return to the paginate transform.  If the formlet
     * corresponding to the paginator does not exist yet, then a FormletTemplateWriter
     * is returned.  Otherwise a FormletWriter is returned.
     */
    public static Writer getFormletWriter(Writer out, Paginator paginator, String applicationName, Locale locale) {
        String paginatorName = paginator.getPaginatorName();
        long viewSize = paginator.getViewSize();
        
        if (isFormletCached(paginatorName, applicationName)) {
            return new FormletWriter(out, paginatorName, applicationName, viewSize, locale);
        } else {
            return new FormletTemplateWriter(out, paginatorName, applicationName, viewSize, locale);
        }
    }


    /*************************************************************************/
    /**                                                                     **/
    /**                       Formlet Templates                             **/
    /**                                                                     **/
    /*************************************************************************/


    // used by getConfiguration() to build the basic formlet configuration for an application
    private static Configuration createConfiguration(String applicationName) throws TemplateException {
        CacheStorage formletCache = getFormletCache(applicationName);
        Configuration config = FreeMarkerWorker.getDefaultOfbizConfig();
        config.setCacheStorage(formletCache);
        return config;
    }

    // get a template at a given component:// location as a string
    private static String getTemplateFromLocationAsString(String location) throws IOException {
        URL url = FlexibleLocation.resolveLocation(location);
        if (url == null) throw new IOException("Template not found at location " + location);
        StringBuffer buff = new StringBuffer();
        BufferedReader reader = new BufferedReader( new InputStreamReader(url.openStream()) );
        String line;
        while ((line = reader.readLine()) != null) buff.append(line).append('\n');
        return buff.toString();
    }

    // load up the application libraries as strings
    private static void loadLibraries(String applicationName) {
        Map libraryLocations = UtilConfig.getPropertyMap(applicationName, "opentaps.ftl.library");
        for (Iterator iter = libraryLocations.keySet().iterator(); iter.hasNext(); ) {
            String libraryName = (String) iter.next();
            String location = (String) libraryLocations.get(libraryName);
            if (! location.startsWith("component://")) {
                Debug.logWarning("Template library named ["+libraryName+"] must use component:// notation.  It is instead: " + libraryName, module);
                continue;
            }

            // read the string from the location
            String libraryString = null;
            try {
                libraryString = getTemplateFromLocationAsString(location);
            } catch (IOException e) {
                Debug.logError(e, "Failed to read template library due to IO Error.  Location is " + location, module);
                continue;
            }

            appendLibrary(applicationName, libraryString);
        }
    }

    /**
     * Creates and caches a formlet template based on the contents of a <@paginate> macro.
     * It is meant to be called from FormletTemplateWriter when it has buffered the nested
     * content.
     */
    public static void createAndCacheFormletTemplate(StringBuffer buff, String paginatorName, String applicationName) {
        FormletCache formletCache = getFormletCache(applicationName);

        // lock cache in case multiple concurrent requests try to build the same formlet
        synchronized (formletCache) {

            // once the lock is released, this will cause subsequent thread to avoid rebuilding the template
            Template formlet = (Template) formletCache.get(paginatorName);
            if (formlet != null) return;

            try {
                // get the freemarker configuration for this application
                Configuration config = getConfiguration(applicationName);

                // make a formlet string which is the union of the libraries with the formlet
                String formletString = getLibraryString(applicationName) + buff.toString();

                // now create the template and store it in FormletCache with key paginatorName
                Template template = new Template(paginatorName, new StringReader(formletString), config);
                formletCache.put(paginatorName, template);
            } catch (IOException e) {
                Debug.logWarning(e, "Failed to create Formlet Template in application ["+applicationName+"] from pagination named ["+paginatorName+"].  Error is: " + e.getMessage(), module);
            } catch (TemplateException e) {
                Debug.logWarning(e, "Failed to create Confgiuration for formlets in application ["+applicationName+"].  Formlets will not work.  Error is: " + e.getMessage(), module);
            }
        }
    }

    /** Used by FormletWriter to render the placeholder. */
    public static void renderFormletPlaceholder(Writer out, String paginatorName, String applicationName, long viewSize, Locale locale) {
        try {
            try {
                Configuration config = getFormletPlaceholderConfiguration();

                // grab the template and render it
                Template template = config.getTemplate("formletPlaceholder");
                Map context = FastMap.newInstance();
                context.put("paginatorName", paginatorName);
                context.put("opentapsApplicationName", applicationName);
                context.put("viewSize", viewSize);
                context.put("locale", locale.toString());
                template.process(context, out);
            } catch (TemplateException e) {
                Debug.logError(e, "Failed to render formlet placehold template.  Pagination is disabled.  Error is: " + e.getMessage(), module);
                out.write(UtilMessage.getInternalError(locale));
                return;
            }
        } catch (IOException ioe) {
            Debug.logError(ioe, "IO Error while writing formlet placeholder: " + ioe.getMessage(), module);
        }
    }

    /**
     * Retrieves the formlet from the cache, builds its context and renders it to the given writer.
     * This method will also construct the page context available to the FTL file.  To see what gets put
     * in this context, see the private buildFormletContext() method in this class.
     */
    public static void renderFormlet(HttpServletRequest request, Writer out, Paginator paginator, Locale locale) throws TemplateException, IOException {
        FormletCache cache = getFormletCache(paginator.getApplicationName());
        Template template = (Template) cache.get(paginator.getPaginatorName());

        // if template is null, means cache was reset while still paging through list using AJAX, so we have to reload
        if (template == null) {
            Debug.logInfo("Cannot render template for paginator ["+paginator.getPaginatorName()+"] because cache was cleared.  Just reload the page.", module);
            // TODO: unsure what to do here, probably render a message to user telling them to reload page
            return;
        }

        Map context = buildFormletContext(request, paginator, locale);
        template.setSetting("locale", locale.toString());
        template.process(context, out);
    }

    private static Map buildFormletContext(HttpServletRequest request, Paginator paginator, Locale locale) {
        Map context = FastMap.newInstance();

        // basic request and session objects
        HttpSession session = request.getSession();
        context.put("request", request);
        context.put("session", session);
        context.put("userLogin", session.getAttribute("userLogin"));
        context.put("security", request.getAttribute("security"));

        // this allows use of Static[]
        BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
        TemplateHashModel staticModels = wrapper.getStaticModels();
        context.put("Static", staticModels);

        // Add custom transforms
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            context.put("import", loader.loadClass("org.opentaps.common.template.freemarker.transform.ImportTransform").newInstance());
            context.put("include", loader.loadClass("org.opentaps.common.template.freemarker.transform.IncludeTransform").newInstance());
        } catch (Exception e) {
            Debug.logInfo(e.getMessage(), module);
        }

        // add the pageRows results
        List pageRows = (List) request.getAttribute("pageRows");
        context.put("pageRows", pageRows);
        context.put("pageSize", pageRows.size());

        // add information from the paginator
        context.put("paginatorName", paginator.getPaginatorName());
        context.put("viewSize", paginator.getViewSize());
        context.put("viewAll", paginator.getViewAll());
        context.put("pageNumber", paginator.getPageNumber());
        context.put("totalPages", paginator.getTotalPages());
        context.put("orderBy", paginator.getOrderByString());
        context.put("opentapsApplicationName", paginator.getApplicationName());
        context.put("parameters", paginator.getParams());    
        context.put("renderExcelButton", new Boolean(paginator.getRenderExcelButton()));

        // and some miscellaneous stuff
        context.put("uiLabelMap", UtilMessage.getUiLabels(locale));

        // apparently to prevent some problems with freemarker
        context.remove("null");
        
        // Add the 'parameters' map
        Map requestAttributes = UtilMisc.toMap("thisRequestUri", request.getAttribute("thisRequestUri"));
        context.put("requestAttributes", requestAttributes);
        
        return context;
    }
}
