package com.studio.statussave.ui.activity;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;


import com.studio.statussave.R;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;


public class CustomProgressDialog extends Dialog {

    Context context;

    public CustomProgressDialog(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (window != null) {
            window.requestFeature(Window.FEATURE_NO_TITLE);
        }
        super.onCreate(savedInstanceState);
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        View progressView = ((LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE)).
                inflate(R.layout.view_progress_dialog, null);

        LinearLayout linearLayout = progressView.findViewById(R.id.loaderDialog);


        setContentView(progressView);


        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }
}