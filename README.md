# Framework

## Upload de fichiers

Un champ de formulaire est automatiquement lié à un argument `MapFile`,
ou à un attribut `MapFile` du bean passé au controller (avec un setter public).
Le nom du champ HTML doit correspondre au nom du paramètre ou de l'attribut.

```html
<form action="upload" method="post" enctype="multipart/form-data">
    <input type="file" name="document">
    <button type="submit">Envoyer</button>
</form>
```

```java
public String upload(MapFile document) {
    return document == null ? "Aucun fichier" : document.getFilename();
}
```

`MapFile` contient le nom complet sans chemin (`report.pdf`), l'extension
sans point (`pdf`) et le contenu sous forme de `Byte[]`. Un fichier absent
donne `null` ; un fichier sélectionné de zéro octet reste un `MapFile` valide.
Les fichiers sont chargés entièrement en mémoire.

Compiler les controllers avec `javac -parameters` pour conserver les noms
des arguments. Le script `make-jar.sh` utilise aussi cette option.
Le servlet porte `@MultipartConfig` ; si les annotations sont désactivées
dans le déploiement, déclarer `<multipart-config/>` dans sa déclaration
`<servlet>` du `web.xml`.

## Réponses JSON

Ajouter `@WebApiRest` sur une méthode de controller, en plus de `@UrlMapping`.
Le framework ne fait alors aucun dispatch vers une vue :

- Un objet, une liste ou une valeur primitive est converti en JSON par Gson.
- Une `String` est envoyée telle quelle : elle doit contenir du JSON valide, déjà préparé par le développeur.
- Un retour `null` produit le JSON `null`.
- Une exception produit une réponse HTTP 500 avec un corps JSON.

```java
@UrlMapping("/employes")
@WebApiRest
public List<Employe> employes() {
    return service.findAll(); // Le framework convertit la liste en JSON.
}

@UrlMapping("/statut")
@WebApiRest
public String statut() {
    return "{\"ok\":true}"; // JSON déjà préparé, sans double conversion.
}
```

Pour renvoyer une simple chaîne JSON, inclure ses guillemets JSON :
`return "\"bonjour\"";`. `return "bonjour";` ne contient pas du JSON valide.
Sans `@WebApiRest`, le traitement habituel, notamment `ModelAndView`, reste utilisé.

Placer `gson-2.14.0.jar` dans `lib/` pour compiler avec `make-jar.sh`, puis
déployer ce même JAR dans `WEB-INF/lib` de l'application avec `autumn.jar`.
L'API Jakarta Servlet doit être disponible à la compilation ; Tomcat la fournit
à l'exécution. Le script ne regroupe pas les dépendances dans `autumn.jar`.
Documentation Gson : https://google.github.io/gson/UserGuide.html

- Il faut avoir le fichier jar du servlet disponible dans votre tomcat ou l'avoir dans un dossier lib localement
