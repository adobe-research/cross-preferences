package com.adobe.prefs.admin.app;

import org.springframework.web.bind.annotation.RequestMapping;

import java.util.prefs.Preferences;

@RequestMapping(UserPrefsController.REALM)
public class UserPrefsController extends PrefsController {
    static final String REALM = HomeController.ROOT + "/usr";

    public UserPrefsController() {
        super(REALM, Preferences.userRoot());
    }
}
