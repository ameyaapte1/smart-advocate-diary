package aa.developer.advocatediary;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONObject;

public class CaseDetailsActivity extends AppCompatActivity {

    final String mimeType = "text/html";
    final String encoding = "UTF-8";
    private String html, id;
    private JSONObject json;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_case_details);


        WebView webView = findViewById(R.id.webview);
        Button button_sync_delete = findViewById(R.id.button_sync_delete);
        Intent intent = getIntent();


        html = intent.getStringExtra("html");
        id = intent.getStringExtra("id");

        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.loadDataWithBaseURL("", html, mimeType, encoding, "");

        button_sync_delete.setText("Delete Case");
        button_sync_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(CaseDetailsActivity.this)
                        .title("Deleting Case")
                        .content("Are you sure? All case data will be lost.\nTo continue Press OK")
                        .positiveText("OK")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                removeFromDatabase(id);
                                finish();
                            }
                        })
                        .show();
            }
        });
    }


    private void removeFromDatabase(String id) {
        CaseDatabaseHelper.getInstance(CaseDetailsActivity.this).removeCase(id);
    }
}
