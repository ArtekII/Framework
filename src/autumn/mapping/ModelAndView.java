package autumn.mapping;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {

    private Map<String, Object> model = new HashMap<>();
    private String url;

    public ModelAndView() {
        this.model = new HashMap<>();
    }

    public ModelAndView(String url) {
        this.url = url;
        this.model = new HashMap<>();
    }

    public ModelAndView(String url, Map<String, Object> model) {
        this.url = url;
        this.model = model;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public void setModel(Map<String, Object> model) {
        this.model = model;
    }

    public void addAttribute(String key, Object value) {
        this.model.put(key, value);
    }
    
}
