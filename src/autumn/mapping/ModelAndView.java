package autumn.mapping;

import java.util.Map;

public class ModelAndView {

    private Map<String, Object> model;
    private String viewChemin;

    public ModelAndView(String viewName, Map<String, Object> model) {
        this.viewChemin = viewName;
        this.model = model;
    }

    public String getViewChemin() {
        return viewChemin;
    }

    public void setViewChemin(String viewName) {
        this.viewChemin = viewName;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public void setModel(Map<String, Object> model) {
        this.model = model;
    }
    
}
