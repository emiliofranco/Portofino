/*
 * Copyright (C) 2005-2013 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.manydesigns.portofino.servlets;

import com.manydesigns.elements.ElementsProperties;
import com.manydesigns.elements.ElementsThreadLocals;
import com.manydesigns.elements.configuration.BeanLookup;
import com.manydesigns.portofino.PortofinoBaseProperties;
import com.manydesigns.portofino.di.Injections;
import com.manydesigns.portofino.i18n.ResourceBundleManager;
import com.manydesigns.portofino.menu.MenuBuilder;
import com.manydesigns.portofino.modules.ApplicationListener;
import com.manydesigns.portofino.modules.BaseModule;
import com.manydesigns.portofino.modules.Module;
import com.manydesigns.portofino.modules.ModuleRegistry;
import net.sourceforge.stripes.util.ResolverUtil;
import org.apache.commons.configuration.*;
import org.apache.commons.configuration.interpol.ConfigurationInterpolator;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;


/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla       - alessio.stalla@manydesigns.com
*/
public class PortofinoListener
        implements ServletContextListener, HttpSessionListener {
    public static final String copyright =
            "Copyright (c) 2005-2013, ManyDesigns srl";

    //**************************************************************************
    // Constants
    //**************************************************************************

    public static final String SEPARATOR =
            "----------------------------------------" +
                    "----------------------------------------";
    
    public static final String PORTOFINO_MESSAGES_FILE_NAME = "portofino-messages.properties";

    //**************************************************************************
    // Fields
    //**************************************************************************

    protected Configuration elementsConfiguration;
    protected FileConfiguration configuration;

    protected File applicationDirectory;

    protected ServletContext servletContext;
    protected ServerInfo serverInfo;

    protected ModuleRegistry moduleRegistry;

    protected List<ApplicationListener> applicationListeners;

    //**************************************************************************
    // Logging
    //**************************************************************************

    public static final Logger logger =
            LoggerFactory.getLogger(PortofinoListener.class);

    //**************************************************************************
    // ServletContextListener implementation
    //**************************************************************************

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            ElementsThreadLocals.setupDefaultElementsContext();
            ServletContext servletContext = servletContextEvent.getServletContext();
            ElementsThreadLocals.setServletContext(servletContext);
            init(servletContextEvent);
        } catch (Throwable e) {
            logger.error("Could not start ManyDesigns Portofino", e);
            throw new Error(e);
        } finally {
            ElementsThreadLocals.removeElementsContext();
        }
    }

    private void init(ServletContextEvent servletContextEvent) {
        // clear the Mapping Diagnostic Context for logging
        MDC.clear();

        servletContext = servletContextEvent.getServletContext();

        serverInfo = new ServerInfo(servletContext);
        servletContext.setAttribute(BaseModule.SERVLET_CONTEXT, servletContext);
        servletContext.setAttribute(BaseModule.SERVER_INFO, serverInfo);

        setupCommonsConfiguration();

        elementsConfiguration = ElementsProperties.getConfiguration();
        servletContext.setAttribute(
                BaseModule.ELEMENTS_CONFIGURATION, elementsConfiguration);

        try {
            loadConfiguration();
        } catch (ConfigurationException e) {
            logger.error("Could not load configuration", e);
            throw new Error(e);
        }
        servletContext.setAttribute(BaseModule.APPLICATION_DIRECTORY, applicationDirectory);
        servletContext.setAttribute(BaseModule.PORTOFINO_CONFIGURATION, configuration);

        logger.debug("Setting blobs directory");
        File appBlobsDir;
        if(configuration.containsKey(PortofinoBaseProperties.BLOBS_DIR_PATH)) {
            appBlobsDir = new File(configuration.getString(PortofinoBaseProperties.BLOBS_DIR_PATH));
        } else {
            File appDir = (File) servletContext.getAttribute(BaseModule.APPLICATION_DIRECTORY);
            appBlobsDir = new File(appDir, "blobs");
        }
        String blobsDirPath = appBlobsDir.getAbsolutePath();
        elementsConfiguration.setProperty(ElementsProperties.BLOBS_DIR, blobsDirPath);
        logger.info("Blobs directory: " + blobsDirPath);

        ClassLoader classLoader = getClass().getClassLoader();
        logger.debug("Registering application class loader");
        servletContext.setAttribute(BaseModule.CLASS_LOADER, classLoader);

        logger.debug("Installing I18n ResourceBundleManager");
        ResourceBundleManager resourceBundleManager = new ResourceBundleManager();
        try {
            Enumeration<URL> messagesSearchPaths = classLoader.getResources(PORTOFINO_MESSAGES_FILE_NAME);
            while (messagesSearchPaths.hasMoreElements()) {
                resourceBundleManager.addSearchPath(messagesSearchPaths.nextElement().toString());
            }
            File appMessages = new File(applicationDirectory, PORTOFINO_MESSAGES_FILE_NAME);
            resourceBundleManager.addSearchPath(appMessages.getAbsolutePath());
        } catch (IOException e) {
            logger.warn("Could not initialize resource bundle manager", e);
        }
        servletContext.setAttribute(BaseModule.RESOURCE_BUNDLE_MANAGER, resourceBundleManager);
        applicationListeners = new ArrayList<ApplicationListener>();
        servletContext.setAttribute(BaseModule.APP_LISTENERS, applicationListeners);

        logger.info("Servlet API version is " + serverInfo.getServletApiVersion());
        if (serverInfo.getServletApiMajor() < 3) {
            String msg = "Servlet API version must be >= 3.0.";
            logger.error(msg);
            throw new InternalError(msg);
        }

        logger.debug("Installing standard menu builders");
        MenuBuilder adminMenuBuilder = new MenuBuilder();
        servletContext.setAttribute(BaseModule.ADMIN_MENU, adminMenuBuilder);

        logger.info("Loading modules...");
        moduleRegistry = new ModuleRegistry(configuration);
        discoverModules(moduleRegistry);
        servletContext.setAttribute(BaseModule.MODULE_REGISTRY, moduleRegistry);
        moduleRegistry.migrateAndInit(servletContext);
        logger.info("All modules loaded.");

        String encoding = configuration.getString(
                PortofinoBaseProperties.URL_ENCODING, PortofinoBaseProperties.URL_ENCODING_DEFAULT);
        logger.info("URL character encoding is set to " + encoding + ". Make sure the web server uses the same encoding to parse URLs.");
        if(!Charset.isSupported(encoding)) {
            logger.error("The encoding is not supported by the JVM!");
        }
        if(!"UTF-8".equals(encoding)) {
            logger.warn("URL encoding is not UTF-8, but the Stripes framework always generates UTF-8 encoded URLs. URLs with non-ASCII characters may not work.");
        }

        logger.info("Invoking application listeners...");
        for(ApplicationListener listener : applicationListeners) {
            Injections.inject(listener, servletContext, null);
            listener.applicationStarting();
        }

        String lineSeparator = System.getProperty("line.separator", "\n");
        logger.info(lineSeparator + SEPARATOR +
                lineSeparator + "--- ManyDesigns Portofino started successfully" +
                lineSeparator + "--- Context path: {}" +
                lineSeparator + "--- Real path: {}" +
                lineSeparator + SEPARATOR,
                new String[]{
                        serverInfo.getContextPath(),
                        serverInfo.getRealPath()
                }
        );
    }

    protected void discoverModules(ModuleRegistry moduleRegistry) {
        ResolverUtil<Module> resolver = new ResolverUtil<Module>();
        resolver.findImplementations(Module.class, Module.class.getPackage().getName());
        Set<Class<? extends Module>> classes = resolver.getClasses();
        classes.remove(Module.class);
        for(Class<? extends Module> moduleClass : classes) {
            try {
                logger.debug("Adding discovered module " + moduleClass);
                Module module = moduleClass.newInstance();
                moduleRegistry.getModules().add(module);
            } catch (Throwable e) {
                logger.error("Could not register module " + moduleClass, e);
            }
        }
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        MDC.clear();
        logger.info("ManyDesigns Portofino stopping...");
        logger.info("Invoking application listeners...");
        for(ApplicationListener listener : applicationListeners) {
            listener.applicationDestroying();
        }
        logger.info("Destroying modules...");
        moduleRegistry.destroy();
        logger.info("ManyDesigns Portofino stopped.");
    }

    //**************************************************************************
    // HttpSessionListener implementation
    //**************************************************************************

    public void sessionCreated(HttpSessionEvent httpSessionEvent) {
        HttpSession session = httpSessionEvent.getSession();
        logger.debug("Session created: id={}", session.getId());
    }

    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        HttpSession session = httpSessionEvent.getSession();
        logger.debug("Session destroyed: id={}", session.getId());
    }

    //**************************************************************************
    // Setup
    //**************************************************************************

    protected void loadConfiguration() throws ConfigurationException {
        applicationDirectory = new File(serverInfo.getRealPath(), "WEB-INF");
        logger.info("Application directory: {}", applicationDirectory.getAbsolutePath());
        File appConfigurationFile = new File(applicationDirectory, "portofino.properties");
        configuration = new PropertiesConfiguration(appConfigurationFile);
    }

    public void addConfiguration(CompositeConfiguration configuration, String resource) {
        try {
            PropertiesConfiguration propertiesConfiguration =
                    new PropertiesConfiguration(resource);
            configuration.addConfiguration(propertiesConfiguration);
        } catch (Throwable e) {
            String errorMessage = ExceptionUtils.getRootCauseMessage(e);
            logger.warn(errorMessage);
            logger.debug("Error loading configuration", e);
        }
    }

    public void setupCommonsConfiguration() {
        logger.debug("Setting up commons-configuration lookups...");
        BeanLookup serverInfoLookup = new BeanLookup(serverInfo);
        ConfigurationInterpolator.registerGlobalLookup(
                "serverInfo",
                serverInfoLookup);
    }
}
