package com.adobe.prefs.admin.app;

import org.springframework.web.bind.annotation.RequestMapping;

import java.util.prefs.Preferences;

@RequestMapping(SystemPrefsController.REALM)
public class SystemPrefsController extends PrefsController {
    static final String REALM = HomeController.ROOT + "/sys";

    public SystemPrefsController() {
        super(REALM, Preferences.systemRoot());
    }
}
