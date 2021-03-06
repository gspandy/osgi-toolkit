/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.developers.blog.osgi.webservice.jaxrs.activator;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.developers.blog.osgi.webservice.jaxrs.impl.HttpServiceTrackerCustomizer;
import org.developers.blog.osgi.webservice.jaxrs.impl.RestProviderServiceTrackerCustomizer;
import org.developers.blog.osgi.webservice.jaxrs.impl.ServiceStateListener;
import org.developers.blog.osgi.webservice.jaxrs.api.JAXRSProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rafsob
 */
public class JAXRSProviderWhiteboardActivator implements BundleActivator {
    private Logger log = LoggerFactory.getLogger(HttpServiceTrackerCustomizer.class);

    private ServiceTracker restServiceProviderTracker;
    private ServiceTracker httpServiceTracker;

    private ServiceRegistration regFilterCORS;

    @Override
    public void start(BundleContext context) throws Exception {
        RestProviderServiceTrackerCustomizer restProviderServiceTrackerCustomizer =
                new RestProviderServiceTrackerCustomizer(context);
        HttpServiceTrackerCustomizer httpServiceTrackerCustomizer = 
                new HttpServiceTrackerCustomizer(context);

        ServiceStateListener serviceStateListener = new ServiceStateListener();
        restProviderServiceTrackerCustomizer.addServiceEventListener(serviceStateListener);
        httpServiceTrackerCustomizer.addServiceEventListener(serviceStateListener);

        restServiceProviderTracker =
                new ServiceTracker(context, JAXRSProvider.class.getName(), restProviderServiceTrackerCustomizer);
        httpServiceTracker =
                new ServiceTracker(context, HttpService.class.getName(), httpServiceTrackerCustomizer);
        httpServiceTracker.open();
        restServiceProviderTracker.open();
        
        Dictionary regAuthFilterProperties = new Hashtable();
        regAuthFilterProperties.put("pattern", "/.*");

        Filter filterCORS = new Filter() {

            @Override
            public void init(FilterConfig fc) throws ServletException {
                //noop
            }

            @Override
            public void doFilter(ServletRequest sr, ServletResponse sr1, FilterChain fc) throws IOException, ServletException {
                HttpServletResponse response = (HttpServletResponse)sr1;
                HttpServletRequest request = (HttpServletRequest)sr;
                String origin = request.getHeader("Origin");
                if (origin != null) {
                    log.info("CORS Handling on. Origin is " + origin);
                    response.addHeader("Access-Control-Allow-Origin", origin);
                    response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE, PUT, HEAD");
                    response.addHeader("Access-Control-Allow-Headers", "Content-Type, Auth-Session-Id");
                    response.addHeader("Access-Control-Max-Age", "86400");
                }

                fc.doFilter(sr, sr1);
            }

            @Override
            public void destroy() {
                //noop
            }
        };

        // Whiteboard Registration
        regFilterCORS = context.registerService(Filter.class.getName(), filterCORS,  regAuthFilterProperties);
        
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (restServiceProviderTracker != null) {
            restServiceProviderTracker.close();
        }
        if (httpServiceTracker != null) {
            httpServiceTracker.close();
        }
        regFilterCORS.unregister();
    }
}
