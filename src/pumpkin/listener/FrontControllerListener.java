package pumpkin.listener;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.HashMap;
import pumpkin.utils.ControllerScanner;
import pumpkin.utils.MethodScanner;
import pumpkin.mapping.Mapping;
import pumpkin.mapping.UrlKey;

@WebListener
public class FrontControllerListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {

            ServletContext context = sce.getServletContext();

            List<Class<?>> controllers = new ArrayList<>();

            Map<UrlKey, Mapping> routes = new HashMap();


            String packageName = context.getInitParameter("controller");
            routes = MethodScanner.findMappings(packageName, controllers);

            context.setAttribute("routes", routes);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
