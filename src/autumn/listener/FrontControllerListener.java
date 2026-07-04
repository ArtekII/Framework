package autumn.listener;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import autumn.mapping.Mapping;
import autumn.mapping.UrlKey;
import autumn.utils.MethodScanner;
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

            Map<UrlKey, Mapping> routes = new HashMap();


            String packageName = context.getInitParameter("controller");
            MethodScanner.findMappings(packageName, controllers, routes);

            context.setAttribute("routes", routes);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
