package com.jiee.smartplug;

import android.app.Activity;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by Chinsoft on 1/12/15.
 * Not being used at the moment
 */
public class utility {

    public void utility(){}

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);

    }
}
