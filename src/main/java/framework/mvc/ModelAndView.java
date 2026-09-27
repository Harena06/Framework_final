package framework.mvc;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModelAndView {

    private String viewName;
    private final Map<String, Object> model = new LinkedHashMap<>();

    public ModelAndView() {
    }

    public ModelAndView(String viewName) {
        this.viewName = viewName;
    }

    public ModelAndView(String viewName, Map<String, Object> model) {
        this.viewName = viewName;
        if (model != null) {
            this.model.putAll(model);
        }
    }

    public ModelAndView addObject(String name, Object value) {
        model.put(name, value);
        return this;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public boolean hasView() {
        return viewName != null && !viewName.isBlank();
    }

    @Override
    public String toString() {
        return "ModelAndView[" + viewName + ", " + model + "]";
    }
}
