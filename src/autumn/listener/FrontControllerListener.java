package autumn.listener;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.File;

import autumn.mapping.Mapping;
import autumn.mapping.UrlKey;
import autumn.utils.MethodScanner;
import autumn.utils.ViewScanner;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.HashMap;

@WebListener
public class FrontControllerListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {

            ServletContext context = sce.getServletContext();

            List<Class<?>> controllers = new ArrayList<>();
            List<File> views = new ArrayList<>();

            Map<UrlKey, Mapping> routes = new HashMap();


            String packageName = context.getInitParameter("controller");

            String viewsDirectory = context.getInitParameter("prefix");
            String suffix = context.getInitParameter("suffix");

            MethodScanner.findMappings(packageName, controllers, routes);
            ViewScanner.findViews(viewsDirectory, suffix, views);

            context.setAttribute("routes", routes);
            context.setAttribute("views", views);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
