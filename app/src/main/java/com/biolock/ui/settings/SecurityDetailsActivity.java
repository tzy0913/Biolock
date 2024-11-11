/**
 * Activity for displaying detailed security assessment information.
 * Shows security status and last assessment timestamp with appropriate color coding.
 */
package com.biolock.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.biolock.R;
import com.biolock.utils.SecurityAssessment;

public class SecurityDetailsActivity extends AppCompatActivity {
    // Constants
    private static final String EXTRA_SECURITY_STATUS = "security_status";
    private static final String EXTRA_LAST_ASSESSMENT = "last_assessment";
    private static final String EXTRA_SECURITY_LEVEL = "security_level";

    // Lifecycle Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_details);
        setupActionBar();
        displaySecurityDetails();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Initialization Methods
    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Security Assessment");
        }
    }

    private void displaySecurityDetails() {
        TextView textStatus = findViewById(R.id.textSecurityStatus);
        TextView textLastAssessment = findViewById(R.id.textLastAssessment);

        String statusText = getIntent().getStringExtra(EXTRA_SECURITY_STATUS);
        String lastAssessment = getIntent().getStringExtra(EXTRA_LAST_ASSESSMENT);
        String securityLevel = determineSecurityLevel(statusText);

        // Set last assessment text
        textLastAssessment.setText(lastAssessment);
        textLastAssessment.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));

        // Format the status text with color
        SpannableStringBuilder builder = new SpannableStringBuilder();

        // Add security level with color
        SpannableString levelText = new SpannableString(securityLevel + "\n\n");
        levelText.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(this, getColorForSecurityLevel(securityLevel))),
                0,
                securityLevel.length(),
                0
        );
        builder.append(levelText);

        // Add the rest of the status text
        String remainingText = statusText.substring(statusText.indexOf('\n') + 1);
        builder.append(remainingText);

        // Colorize specific sections
        colorizeSection(builder, "Warnings:", android.R.color.holo_red_dark);
        colorizeSection(builder, "Recommendations:", android.R.color.holo_blue_dark);

        textStatus.setText(builder);
    }

    private void colorizeSection(SpannableStringBuilder builder, String sectionTitle, int colorRes) {
        String text = builder.toString();
        int startIndex = text.indexOf(sectionTitle);
        if (startIndex != -1) {
            builder.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(this, colorRes)),
                    startIndex,
                    startIndex + sectionTitle.length(),
                    0
            );
        }
    }

    private String determineSecurityLevel(String statusText) {
        if (statusText.contains("EXCELLENT")) return "EXCELLENT";
        if (statusText.contains("GOOD")) return "GOOD";
        if (statusText.contains("FAIR")) return "FAIR";
        if (statusText.contains("ATTENTION NEEDED")) return "ATTENTION NEEDED";
        return "NOT CONFIGURED";
    }

    private int getColorForSecurityLevel(String level) {
        switch (level) {
            case "EXCELLENT":
                return android.R.color.holo_green_dark;
            case "GOOD":
                return android.R.color.holo_green_light;
            case "FAIR":
                return android.R.color.holo_orange_light;
            case "ATTENTION NEEDED":
                return android.R.color.holo_red_light;
            default:
                return android.R.color.darker_gray;
        }
    }

    // Factory Method
    public static Intent createIntent(Context context, String securityStatus, String lastAssessment) {
        Intent intent = new Intent(context, SecurityDetailsActivity.class);
        intent.putExtra(EXTRA_SECURITY_STATUS, securityStatus);
        intent.putExtra(EXTRA_LAST_ASSESSMENT, lastAssessment);
        return intent;
    }
}