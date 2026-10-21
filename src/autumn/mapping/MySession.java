package autumn.mapping;
import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;
import jakarta.servlet.http.HttpSession;

public class MySession {
    private final Map<String, Object> sessionData = new HashMap<>();
    private final HttpSession httpSession;

     public MySession(HttpSession httpSession) {
        this.httpSession = httpSession;

        Enumeration<String> names = httpSession.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            sessionData.put(name, httpSession.getAttribute(name));
        }
    }

    public void setAttribute(String key, Object value) {
        sessionData.put(key, value);
    }

    public Object getAttribute(String key) {
        return sessionData.get(key);
    }

    public void save() {
        for (Map.Entry<String, Object> entry : sessionData.entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue());
        }
    }
}
