package com.google.android.cameraview.demo;

import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

public class MsgUtils {
    public static final int TYPE_DEFAULT = 0, TYPE_ERROR = 1, TYPE_WARNING = 2, TYPE_SUCCESS = 3;

    public static void showSnackBar(View view, String message, int type) {
        if (type == TYPE_ERROR) {
            showSnackBarError(view, message);
        } else {
            showSnackBarDefault(view, message);
        }
    }

    public static void showSnackBarDefault(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .show();
    }

    public static void showSnackBarError(View view, String message) {
        final Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        TextView textView = snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        textView.setMaxLines(10);
        snackbar.setAction("dismiss", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        }).show();
    }

}
