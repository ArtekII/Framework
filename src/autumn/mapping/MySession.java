package autumn.mapping;

import java.util.Objects;
import jakarta.servlet.http.HttpSession;

public class MySession {
    private final HttpSession httpSession;

    public MySession(HttpSession httpSession) {
        this.httpSession = Objects.requireNonNull(httpSession, "httpSession");
    }

    public void setAttribute(String key, Object value) {
        httpSession.setAttribute(key, value);
    }

    public Object getAttribute(String key) {
        return httpSession.getAttribute(key);
    }

    public void removeAttribute(String key) {
        httpSession.removeAttribute(key);
    }

    public void invalidate() {
        httpSession.invalidate();
    }

    public void save() {
    }
}
