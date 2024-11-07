package com.biolock.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.biolock.R;

public class SecurityDetailsActivity extends AppCompatActivity {
    private static final String EXTRA_SECURITY_STATUS = "security_status";
    private static final String EXTRA_LAST_ASSESSMENT = "last_assessment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_details);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Security Assessment");
        }

        TextView textStatus = findViewById(R.id.textSecurityStatus);
        TextView textLastAssessment = findViewById(R.id.textLastAssessment);

        textStatus.setText(getIntent().getStringExtra(EXTRA_SECURITY_STATUS));
        textLastAssessment.setText(getIntent().getStringExtra(EXTRA_LAST_ASSESSMENT));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static Intent createIntent(Context context, String securityStatus, String lastAssessment) {
        Intent intent = new Intent(context, SecurityDetailsActivity.class);
        intent.putExtra(EXTRA_SECURITY_STATUS, securityStatus);
        intent.putExtra(EXTRA_LAST_ASSESSMENT, lastAssessment);
        return intent;
    }
}