package com.studio.statusvault.ui.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.EdgeToEdge;
import androidx.core.view.WindowCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.studio.statusvault.BuildConfig;
import com.studio.statusvault.R;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Support: Formspree POST if support_formspree_form_id is set, else mailto: support_recipient_email. */
public class SupportActivity extends AppCompatActivity {

    private TextInputLayout tilUserEmail;
    private TextInputLayout tilSubject;
    private TextInputLayout tilMessage;
    private TextInputEditText editUserEmail;
    private TextInputEditText editSubject;
    private TextInputEditText editMessage;
    private MaterialButton buttonSubmit;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_support);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_activity_support);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        tilUserEmail = findViewById(R.id.tilSupportUserEmail);
        tilSubject = findViewById(R.id.tilSupportSubject);
        tilMessage = findViewById(R.id.tilSupportMessage);
        editUserEmail = findViewById(R.id.editSupportUserEmail);
        editSubject = findViewById(R.id.editSupportSubject);
        editMessage = findViewById(R.id.editSupportMessage);
        buttonSubmit = findViewById(R.id.buttonSupportSubmit);
        buttonSubmit.setOnClickListener(v -> trySubmit());

        ColorStateList hintColors = AppCompatResources.getColorStateList(this, R.color.support_til_hint);
        if (hintColors != null) {
            tilUserEmail.setDefaultHintTextColor(hintColors);
            tilSubject.setDefaultHintTextColor(hintColors);
            tilMessage.setDefaultHintTextColor(hintColors);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }

    private void trySubmit() {
        String userEmail = stringOrEmpty(editUserEmail);
        String subject = stringOrEmpty(editSubject);
        String message = stringOrEmpty(editMessage);

        clearErrors();
        if (!userEmail.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            tilUserEmail.setError(getString(R.string.support_error_invalid_email));
            return;
        }
        if (message.trim().isEmpty()) {
            tilMessage.setError(getString(R.string.support_error_message_required));
            return;
        }

        if (subject.trim().isEmpty()) {
            subject = getString(R.string.support_default_subject, BuildConfig.VERSION_NAME);
        }
        final String finalSubject = subject;
        final String finalUserEmail = userEmail;
        String fullBody = message.trim() + "\n\n" + buildDeviceInfoBlock();
        final String finalBody = fullBody;
        final String formId = getString(R.string.support_formspree_form_id).trim();
        final String recipient = getString(R.string.support_recipient_email).trim();
        final boolean canMail = isConfiguredRecipient(recipient);
        final boolean hasFormspree = !formId.isEmpty();

        if (!hasFormspree && !canMail) {
            Toast.makeText(this, R.string.support_error_configure_email, Toast.LENGTH_LONG).show();
            return;
        }

        buttonSubmit.setEnabled(false);
        if (hasFormspree) {
            executor.execute(() -> {
                boolean ok = postToFormspree(formId, finalUserEmail, finalSubject, finalBody);
                runOnUiThread(() -> {
                    if (isFinishing()) {
                        return;
                    }
                    buttonSubmit.setEnabled(true);
                    if (ok) {
                        Toast.makeText(this, R.string.support_toast_sent, Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, R.string.support_toast_send_failed, Toast.LENGTH_LONG).show();
                        if (canMail) {
                            openMailto(finalUserEmail, finalSubject, finalBody, recipient);
                        }
                    }
                });
            });
        } else {
            openMailto(finalUserEmail, finalSubject, finalBody, recipient);
            buttonSubmit.setEnabled(true);
        }
    }

    private static boolean isConfiguredRecipient(String recipient) {
        if (recipient == null || recipient.trim().isEmpty()) {
            return false;
        }
        return Patterns.EMAIL_ADDRESS.matcher(recipient.trim()).matches();
    }

    private void clearErrors() {
        tilUserEmail.setError(null);
        tilSubject.setError(null);
        tilMessage.setError(null);
    }

    private static String stringOrEmpty(@Nullable TextInputEditText e) {
        if (e == null || e.getText() == null) {
            return "";
        }
        return e.getText().toString().trim();
    }

    private String buildDeviceInfoBlock() {
        return "---\n"
                + getString(R.string.support_device_info_header) + "\n"
                + getString(R.string.support_device_info_app, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE) + "\n"
                + getString(R.string.support_device_info_android, android.os.Build.VERSION.RELEASE, android.os.Build.VERSION.SDK_INT) + "\n"
                + getString(R.string.support_device_info_model, android.os.Build.MANUFACTURER, android.os.Build.MODEL);
    }

    private boolean postToFormspree(String formId, String userEmail, String subject, String fullBody) {
        HttpURLConnection conn = null;
        try {
            String url = "https://formspree.io/f/" + formId;
            StringBuilder data = new StringBuilder();
            data.append("message=").append(URLEncoder.encode(fullBody, StandardCharsets.UTF_8.name()));
            data.append("&_subject=").append(URLEncoder.encode(subject, StandardCharsets.UTF_8.name()));
            if (!userEmail.isEmpty()) {
                data.append("&email=").append(URLEncoder.encode(userEmail, StandardCharsets.UTF_8.name()));
                data.append("&_replyto=").append(URLEncoder.encode(userEmail, StandardCharsets.UTF_8.name()));
            }
            byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);

            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "ReelVault-Android/" + BuildConfig.VERSION_NAME);
            conn.setConnectTimeout(20_000);
            conn.setReadTimeout(20_000);
            conn.setDoOutput(true);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(bytes);
            }
            int code = conn.getResponseCode();
            return code >= 200 && code < 300;
        } catch (IOException e) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void openMailto(String userEmail, String subject, String fullBody, String recipient) {
        if (!isConfiguredRecipient(recipient)) {
            return;
        }
        try {
            String body = fullBody;
            if (!userEmail.isEmpty()) {
                body = getString(R.string.support_mail_from_line, userEmail) + "\n\n" + fullBody;
            }
            String uri = "mailto:" + recipient
                    + "?subject=" + Uri.encode(subject)
                    + "&body=" + Uri.encode(body);
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse(uri));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, getString(R.string.support_chooser_email)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.support_error_no_email_app, Toast.LENGTH_LONG).show();
        }
    }
}
