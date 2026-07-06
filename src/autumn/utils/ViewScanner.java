package autumn.utils;

import java.io.File;
import java.util.List;

public class ViewScanner {
    
    public static void findViews(String directoryPath, String suffix, List<File> views) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Le chemin du dossier ne doit pas être vide");
        }

        File folder = new File(directoryPath.trim());
        
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("Le chemin " + directoryPath + " n'existe pas ou n'est pas un dossier");
        }

        File[] files = folder.listFiles();
        
        if (files == null) {
            return; 
        }

        for (File file : files) {
            if (file.isDirectory()) {
                findViews(file.getAbsolutePath(), suffix, views);
            } else {
                if (file.getName().toLowerCase().endsWith(suffix.toLowerCase())) {
                    views.add(file);
                }
            }
        }
    }
}