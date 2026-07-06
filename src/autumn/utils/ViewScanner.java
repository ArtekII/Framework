package autumn.utils;

import java.util.List;

public class ViewScanner {
    public static void findViews(String packageName, List<Class<?>> views) {
        if (packageName == null || packageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du package ne doit pas etre vide");
        }

        String packName = packageName.trim();
        // Implémentation de la recherche des vues dans le package spécifié
        // Cette méthode peut être similaire à findControllers, mais adaptée pour les vues
    }
}