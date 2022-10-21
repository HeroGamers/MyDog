package dk.fido2603.mydog.managers;

import dk.fido2603.mydog.MyDog;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Language Manager
 */
public class LanguageManager {
    private Locale locale = null;
    private ResourceBundle messages = null;

    private static LanguageManager languageManager;

    /**
     * Load locale matching with locale files.
     */
    private LanguageManager() {
        locale = Locale.getDefault();

        if (locale.getLanguage().equals(new Locale("en").getLanguage())) {
            locale = Locale.ROOT;
        } else if (locale.getLanguage().equals(new Locale("da").getLanguage())) {
            // Example for how it might be done
//            locale = new Locale("da", "DK");
            locale = Locale.ROOT;
        } else {
            locale = Locale.ROOT;
        }

        // initialize
        messages = getMessages();
    }

    public static LanguageManager getInstance() {
        if (languageManager == null) {
            languageManager = new LanguageManager();
        }

        return languageManager;
    }

    /**
     * Function that takes either language of country, and by either language
     * or country decide the language of the game
     *
     * @param language the language you want
     * @param country  the country you want
     */
    public void setLocale(String language, String country) {
        locale = new Locale(language, country);
        messages = getMessages();
    }

    public void setLocale(String language) {
        if (language.contains("_")) {
            setLocale(language.split("_")[0], language.split("_")[1]);
        } else {
            locale = new Locale(language);
            messages = getMessages();
        }
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
        messages = getMessages();
    }

    public String getLocale() {
        return locale.getDisplayLanguage();
    }

    private Locale[] getLocales() {
        ArrayList<Locale> locales = new ArrayList<>();

        InputStream resource;
        for (Locale locale : Locale.getAvailableLocales()) {
            resource = MyDog.instance().getResource("MyDog_" + locale.toString() + ".properties");
            if (resource != null) {
                locales.add(locale);
            }
        }
        locales.add(Locale.ROOT);

        return locales.toArray(new Locale[0]);
    }

    public HashMap<String, Locale> getLocalesMap() {
        HashMap<String, Locale> locales = new HashMap<>();

        for (Locale locale : getLocales()) {
            locales.put(locale.getDisplayLanguage(), locale);
        }

        return locales;
    }

    /**
     * Function that returns the messages
     *
     * @return all the messages in the locale
     */
    private ResourceBundle getMessages() {
        return ResourceBundle.getBundle("GameMessages", locale);
    }

    /**
     * A function that takes a key for a specific message and returns
     * that message
     *
     * @param messageKey key for a specific message
     * @return returns the specific message
     */
    public String getString(String messageKey) {
        String message = "";
        try {
            message = messages.getString(messageKey);
        } catch (Exception e) {
            // Don't print the stacktrace when we run tests
            if (!messageKey.equals("non_existent_test_string")) {
                e.printStackTrace();
            }
            message = "Could not read message '" + messageKey + "' from locale " + locale.getLanguage() + " - Update the language on GitHub!";
        }
        return message;
    }
}
