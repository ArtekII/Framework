package autumn.binding;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import autumn.mapping.MapFile;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import autumn.mapping.MySession;

public class RequestBinder {

    public Object[] bindArguments(Method method, HttpServletRequest req)
        throws ReflectiveOperationException, IOException, ServletException {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];
        MySession session = null;

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String name = parameter.getName();
            Class<?> type = parameter.getType();
            if (type == MapFile.class) {
                arguments[i] = bindFile(req, name);
            } else if (verifyType(type)) {
                arguments[i] = convertParameter(name, req.getParameter(name), type);
            } else if (type == MySession.class) {
                if (session == null) {
                    session = new MySession(req.getSession());
                }
                arguments[i] = session;
            }
            else {
                arguments[i] = bindBean(type, req, type.getSimpleName());
            }
        }
        return arguments;
    }

    private Object bindBean(Class<?> beanType, HttpServletRequest req, String path)
        throws ReflectiveOperationException, IOException, ServletException {
        if (beanType.isPrimitive() || beanType.isArray() || beanType.isInterface()
                || beanType.isEnum() || beanType.getName().startsWith("java.")) {
            throw new BindingException("Type de bean non supporté pour " + path + " : " + beanType.getSimpleName());
        }
        Object bean = beanType.getDeclaredConstructor().newInstance();

        for (Class<?> current = beanType;
        current != null && current != Object.class;
        current = current.getSuperclass()) {
            bindFields(current, bean, req, path);
        }

        return bean;
    }

    private void bindFields(Class<?> beanType, Object bean, HttpServletRequest req, String path)
        throws ReflectiveOperationException, IOException, ServletException {
        for (Field field : beanType.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            String name = field.getName();
            String fieldPath = path + "." + name;
            Class<?> type = field.getType();
            Object value;
            if (type == MapFile.class) {
                value = bindFile(req, name);
                if (value == null) {
                    continue;
                }
            } else if(verifyType(type)) {
                String text = req.getParameter(name);
                if (text == null) {
                    continue;
                }
                value = convertParameter(fieldPath, text, type);
            }
            else {
                value = bindBean(type, req, fieldPath);
            }
            setBeanProperty(bean, name, type, value);
        }
    }

    private MapFile bindFile(HttpServletRequest req, String name)
        throws IOException, ServletException {
        String contentType = req.getContentType();
        if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT)
                .startsWith("multipart/form-data")) {
            return null;
        }
        Part filePart = req.getPart(name);
        if (filePart == null) {
            return null;
        }
        String filename = filePart.getSubmittedFileName();
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        
        filename = filename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        int dot = filename.lastIndexOf('.');
        String extension = dot > 0 ? filename.substring(dot + 1) : "";
        try (InputStream stream = filePart.getInputStream()) {
            return new MapFile(filename, extension, stream.readAllBytes());
        }
    }

    private void setBeanProperty(Object bean, String name, Class<?> type, Object value)
        throws ReflectiveOperationException {
        String setterName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        Method setter = bean.getClass().getMethod(setterName, type);
        setter.invoke(bean, value);
    }

    private boolean verifyType(Class<?> type) {
        return type == String.class
            || type == int.class || type == Integer.class
            || type == long.class || type == Long.class
            || type == double.class || type == Double.class;
    }

    private Object convertParameter(String path, String value, Class<?> type) {
        if (!verifyType(type)) {
            throw new BindingException("Type de paramètre non supporté : "
                + path + " (" + type.getSimpleName() + ")");
        }
        if (value == null) {
            if (type.isPrimitive()) {
                throw new BindingException("Paramètre obligatoire absent : " + path);
            }
            return null;
        }
        try {
            if (type == String.class) {
                return value;
            }
            if (type == int.class || type == Integer.class) {
                return Integer.valueOf(value);
            }
            if (type == long.class || type == Long.class) {
                return Long.valueOf(value);
            }
            if (type == double.class || type == Double.class) {
                return Double.valueOf(value);
            }
            throw new BindingException("Type de paramètre non supporté : " + path);
        } catch (NumberFormatException e) {
            throw new BindingException("Impossible de remplir " + path
                + " : la valeur \"" + value + "\" doit être de type "
                + type.getSimpleName());
        }
    }

}
