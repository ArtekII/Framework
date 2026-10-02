package autumn.mapping;

import java.lang.reflect.Method;

public class Mapping {
    // il faut assuer l'uniciter de l'url, quand on fait /liste, il faut pas qu'il y ait un autre /liste
    private String nomClasse;
    private String nomMethode;
    private Method method;


    public Mapping(String nomClasse, String nomMethode) {
        this.nomClasse = nomClasse;
        this.nomMethode = nomMethode;
    }

    public String getNomClasse() {
        return nomClasse;
    }
    public void setNomClasse(String nomClasse) {
        this.nomClasse = nomClasse;
    }
    public String getNomMethode() {
        return nomMethode;
    }
    public void setNomMethode(String nomMethode) {
        this.nomMethode = nomMethode;
    }
    public Method getMethod() {
        return method;
    }
    public void setMethod(Method method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "Mapping{" +
            "nomClasse='" + nomClasse + '\'' +
            ", nomMethode='" + nomMethode + '\'' +
            '}';
    }
}
