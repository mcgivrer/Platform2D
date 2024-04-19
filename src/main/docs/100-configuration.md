# Configuration

Depuis le début du projet, la configuration est supportée par la class principale `Platform2D`.
Il est grand temps de repenser le module de configuration pour e rendre facilement maintenable et extensible.

Je vous propose de commencer, comme il se doit, avec une classe `Configuration` qui supportera les operations de bases.
Ainsi, dans un premier temps, créons la classe `Configuration` :

```java
import com.snapgames.platform.Platform2D;

import java.util.Properties;

public static class Configuration {
    Platform2D app;

    public Configuration(Platform2D app) {
        this.app = app;
    }
}
```

Et regroupons les méthodes présentes dans la classe principale `Platform2D` vers `Configuration`:

```java
public static class Configuration {
    //...
    public void loadConfiguration(String configFilePath) {
        //...
    }

    public void parseArguments(Properties props) {
        //...
    }

    public void parseArguments(String[] args) {
        //...
    }
    //...
}
```

Nous devons adapter les attributes des 2 classes :

- l'objet `Properties config` dans `Platform2D` est maintenant replacé par

```java
public static class Configuration {
    //...
    Properties props;
    //...
}
```

- Et nous allons ajouter un nouvel attribut `configValues` dans la classe `Configuration`, servant à conserver les
  instances des valeurs de configuration :

```java
public static class Configuration {
    //...
    private Map<String, Object> configValues = new ConcurrentHashMap<>();
    //...
}
```

Cette map sera peuplée par la méthode `parseAttributes(Map)`:

```java
public static class Configuration {
    //...
    public void parseAttributes(Map<String, Object> attributes) {
        if (!attributes.isEmpty()) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                // Each entry is cased as ("configuration key", "long CLI argument", "short key argument")
                switch (key) {
                    // set new configuration file
                    case "configuration.file", "config", "cf" -> configValues.put(key, (String) value);
                    // set internal rendering screen dimension
                    case "app.screen.size", "buffer", "b" -> {
                        String[] values = ((String) value).split("x");
                        configValues.put(key, new Dimension(Integer.parseInt(values[0]), Integer.parseInt(values[1])));
                    }
                    // set display windows dimension
                    case "app.window.size", "window", "w" -> {
                        String[] values = ((String) value).split("x");
                        configValues.put(key, new Dimension(Integer.parseInt(values[0]), Integer.parseInt(values[1])));
                    }
                    // set default debug level
                    case "app.debug.level", "debug", "d" -> configValues.put(key, Integer.parseInt((String) value));
                    // set entity filtering 
                    case "app.debug.filter", "filter", "df" -> configValues.put(key, (String) value);
                    // set the test mode (only for... test mode :)
                    case "app.test.mode", "test" -> configValues.put(key, Boolean.parseBoolean((String) value));
                    // set the default scene to load at start
                    case "app.scenes.default", "default" -> configValues.put(key, (String) value);
                    // set the list of scene to get prepared at starting time
                    case "app.scenes.list", "scenes" -> configValues.put(key, ((String) value)
                        // sanitize string
                        .replace(",,", ",")
                        .replace("::", ":")
                        // split scene items
                        .split(","));
                    // in case of unknown configuration key. 
                    default -> warn("Value entry unknown %s=%s", key, value);
                }
            }
        } else {
            error("Configuration is missing");
        }
    }
}
```

Et maintenant, nous pouvons adapter la classe principale pour qu'elle utilise cette nouvelle configuration :

```java
public static class Platform2D {
    //...

    void initialize(String[] args) {
        // load configuration
        config = new Configuration(this);
        config.initializeDefaultConfiguration();
        config.parseArguments(args);
        config.loadConfiguration(configurationFilePath);
        config.parseArguments();
        config.parseArguments(args);

        // set internals according to configuration.
        screenSize = config.getValue("app.window.size");
        bufferSize = config.getValue("app.screen.size");
        testMode = config.getValue("app.test.mode");
        debugFilter = config.getValue("app.debug.filter");
        debug = config.getValue("app.debug.level");
        //...
    }
    //...
}
```

Et voilà !

Nous avons maintenant une base plus propre pour la gestion de la configuration.

