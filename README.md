# Framework

## Instances des contrôleurs

Au démarrage, `FrontControllerListener` crée une instance de chaque classe
`@Controller`, avec son constructeur sans paramètre. Les instances sont publiées
dans une map non modifiable du `ServletContext`, sous la clé `"controllers"`
(`FrontControllerListener.CONTROLLERS_ATTRIBUTE`). Toutes les requêtes et toutes
les méthodes d'une même classe réutilisent cette instance. Un redéploiement crée
de nouvelles instances ; l'arrêt retire le registre du contexte.

Un contrôleur est partagé entre les utilisateurs et peut exécuter plusieurs
requêtes simultanément. Les données de requête, l'utilisateur et sa session
doivent rester dans les paramètres et les variables locales. Les champs servant
à partager des services doivent être adaptés aux accès concurrents.

## Session utilisateur

Le framework injecte `autumn.mapping.MySession` dans les paramètres des méthodes
du contrôleur. Les paramètres `MySession` d'un même appel partagent la même
instance ; chaque appel dispose de son propre objet lié à sa session HTTP.
L'injection dans les champs est supprimée. Un champ `MySession`, privé, hérité
ou statique, empêche le démarrage avec un message demandant de le remplacer par
un paramètre. Cela évite de partager la session d'un utilisateur dans le contrôleur.

```java
import autumn.annotation.Controller;
import autumn.annotation.UrlMapping;
import autumn.mapping.MySession;

@Controller
public class ProfilController {
    @UrlMapping("/profil")
    public String profil(MySession session) {
        session.setAttribute("nom", "Alice");
        return (String) session.getAttribute("nom");
    }

    @UrlMapping(value = "/deconnexion", method = "POST")
    public String deconnexion(MySession session) {
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

À chaque requête, avant de lire les paramètres et d'appeler la méthode, le servlet consulte
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

Le contrôle précède la lecture des paramètres et l'appel de la méthode,
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
