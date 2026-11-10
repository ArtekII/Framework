# Framework

## Session utilisateur

Le framework injecte `autumn.mapping.MySession` dans les paramètres des méthodes
du contrôleur et dans ses champs d'instance, même privés ou hérités. Les champs
injectés ne doivent pas être `final` ; les champs `static` sont ignorés.
Tous les paramètres et champs injectés d'un même appel partagent la même instance.

```java
import autumn.annotation.Controller;
import autumn.annotation.UrlMapping;
import autumn.mapping.MySession;

@Controller
public class ProfilController {
    private MySession session;

    @UrlMapping("/profil")
    public String profil(MySession utilisateur) {
        // utilisateur et session désignent la même instance.
        utilisateur.setAttribute("nom", "Alice");
        return (String) session.getAttribute("nom");
    }

    @UrlMapping(value = "/deconnexion", method = "POST")
    public String deconnexion() {
        session.invalidate();
        return "Déconnecté";
    }
}
```


Ajouter `autumn.annotation.Auth` sur chaque méthode qui exige une connexion :

```java
import autumn.annotation.Auth;
import autumn.annotation.UrlMapping;
import autumn.mapping.MySession;

@Auth
@UrlMapping("/espace-prive")
public String espacePrive(MySession session) {
    return "Bonjour " + session.getAttribute(Auth.USER_ATTRIBUTE);
}
```

Après validation des identifiants par l'application, enregistrer l'utilisateur
validé dans la session avec la clé `Auth.USER_ATTRIBUTE`, dont la valeur est
`"auth.user"` :

```java
session.setAttribute(Auth.USER_ATTRIBUTE, utilisateurValide);
```

Le framework fournit le contrôle d'accès ; l'application reste responsable de
vérifier les identifiants. Un paramètre de requête nommé `auth.user` ne donne
aucun accès. Une session existante sans cet attribut reste anonyme.

Avant de construire le contrôleur ou de lire ses paramètres, le servlet consulte
la session existante via `getSession(false)`. Une méthode annotée `@Auth` renvoie
HTTP **401** si la session est absente, expirée ou sans utilisateur authentifié.
La réponse contient un message en texte ; avec `@WebApiRest`, elle contient un
objet JSON avec une propriété `error`. Aucune session n'est créée par ce contrôle.
Une méthode sans `@Auth` reste publique. Le contrôle s'applique aussi aux routes POST.

Pour retirer l'authentification, supprimer `Auth.USER_ATTRIBUTE` ou appeler
`session.invalidate()` pour détruire toute la session. Les requêtes suivantes
vers les méthodes protégées sont alors refusées.

## Autorisation par rôle

Pour associer une méthode à un rôle, préciser `roles` sur `@Auth` :

```java
@Auth(roles = "ADMIN")
@UrlMapping("/administration")
public String administration() {
    return "Accès administrateur autorisé";
}

@Auth(roles = {"USER", "ADMIN"})
@UrlMapping("/documents")
public String documents() {
    return "Accès autorisé à USER ou ADMIN";
}
```

Après validation des identifiants, l'application enregistre l'utilisateur et son
rôle en session. Le rôle doit provenir des données validées côté serveur :

```java
session.setAttribute(Auth.USER_ATTRIBUTE, utilisateurValide);
session.setAttribute(Auth.ROLE_ATTRIBUTE, "ADMIN"); // clé : auth.role
```

`Auth.ROLE_ATTRIBUTE` contient un rôle sous forme de `String`. Une méthode peut
accepter plusieurs rôles : une seule correspondance suffit. La comparaison est
exacte et sensible à la casse ; `ADMIN` n'accorde pas automatiquement `USER`.

- Sans utilisateur authentifié : HTTP **401**, même si un rôle est présent.
- Utilisateur authentifié avec un rôle absent, mal typé ou non autorisé : HTTP **403**.
- Utilisateur authentifié avec un rôle autorisé : exécution de la méthode.
- `@Auth` sans `roles` conserve son comportement : seule la connexion est exigée.

Le contrôle précède la construction du contrôleur et la lecture des paramètres,
en GET comme en POST. Avec `@WebApiRest`, les refus 401 et 403 sont rendus en JSON.
Un paramètre HTTP `auth.role=ADMIN` ne modifie pas le rôle en session. Lors d'un
changement d'utilisateur, remplacer aussi son rôle pour ne pas conserver celui
de la connexion précédente ; `invalidate()` efface les deux à la déconnexion.

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
