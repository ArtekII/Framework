package pumpkin.mapping;

public class Url {
    String path;
    String method;

    public Url(String path, String method) {
        this.path = path;
        this.method = method;
    }

    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "Url{" +
                "path='" + path + '\'' +
                ", method='" + method + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Url url = (Url) obj;

        if (!path.equals(url.path)) return false;
        return method.equals(url.method);
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + method.hashCode();
        return result;
    }

    // hashcode est utilisé pour comparer les objets dans les collections, 
    // comme les HashMap ou les HashSet. 
    // Il est important de le redéfinir lorsque vous redéfinissez equals() pour garantir que deux objets égaux ont le même code de hachage.
}
