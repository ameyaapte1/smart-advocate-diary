package aa.developer.advocatediary;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Base64;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.ads.MobileAds;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;


public class CaseSearchWebview extends AppCompatActivity {

    private static final String AD_FREE = "ad_free";
    private String html;
    private JSONObject jsonObject;
    private Button save_button;
    private MaterialDialog progressDialog;
    private WebView myWebView;
    //private String baseurl = "https://court.mah.nic.in/courtweb/cases/";
    private String baseurl = "https://services.ecourts.gov.in/ecourtindia_v4_bilingual/cases/";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_case_search_webview);

        save_button = findViewById(R.id.save_button);

        Intent intent = getIntent();
        String dist_cd = intent.getStringExtra("dist_code");
        String search_url = intent.getStringExtra("search_url");
        jsonObject = new JSONObject();

        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertIntoDatabase();
            }
        });

        myWebView = findViewById(R.id.webview);

        WebSettings webSettings = myWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);


        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        myWebView.addJavascriptInterface(this, "app");
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        myWebView.setWebViewClient(new WebViewClient() {

            @Override public void onReceivedError(WebView view, WebResourceRequest request,
                                                  WebResourceError error) {
                super.onReceivedError(view, request, error);
                // Do something
            }


            @Override
            public void onPageStarted(final WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                injectScriptFile(view, "script.js"); // see below ...

            }

            private void injectScriptFile(WebView view, String scriptFile) {
                InputStream input;
                try {
                    input = getAssets().open(scriptFile);
                    byte[] buffer = new byte[input.available()];
                    input.read(buffer);
                    input.close();

                    // String-ify the script byte-array using BASE64 encoding !!!
                    String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
                    view.loadUrl("javascript:(function() {" +
                            "var parent = document.getElementsByTagName('head').item(0);" +
                            "var script = document.createElement('script');" +
                            "script.type = 'text/javascript';" +
                            // Tell the browser to BASE64-decode the string into your script !!!
                            "script.innerHTML = window.atob('" + encoded + "');" +
                            "parent.appendChild(script)" +
                            "})()");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        myWebView.loadUrl(baseurl + search_url + "?state=D&state_cd=1&dist_cd=" + dist_cd);
//        myWebView.loadUrl("https://services.ecourts.gov.in/ecourtindia_v4_bilingual/cases/ki_petres.php?state=D&state_cd=1&dist_cd=25");
    }



    @JavascriptInterface
    public void showCaseDetails(String parameters, String result) {
        /*
        __csrf_magic=sid:97c094ac89409f3f32595f141d2c61e852f797bc,1485175560
        court_code=24
        state_code=1
        dist_code=25
        case_no=201700000022016
        cino=MHPU210001062016
        appFlag=
        */

        String[] data;
        parameters = (parameters + " ").replace('&', '=');
        data = parameters.split("=");

        for (int i = 0; i < data.length; i += 2) {
            try {
                jsonObject.put(data[i], data[i + 1]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            jsonObject.put("description", getDescription(result));
            jsonObject.put("type_and_no", getTypeNo(result));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        jsonObject.remove("__csrf_magic");
        html = result;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                save_button.setVisibility(View.VISIBLE);
            }
        });
    }

    private String getDescription(String html) {
        String searchstr = ">1)";

        int start1 = html.indexOf(searchstr) + searchstr.length();
        int end1 = html.indexOf("<br", start1);

        int start2 = html.indexOf(searchstr, end1) + searchstr.length();
        int end2 = html.indexOf("<br", start2);

        return html.substring(start1, end1) + " vs " + html.substring(start2, end2);
    }

    private String getTypeNo(String html) {
        String case_type_start = "Case Type </label></span>:";
        String registration_start = "Registration Number</label></span><label>:";
        int start1 = html.indexOf(case_type_start) + case_type_start.length();
        int end1 = html.indexOf("</span>", start1);

        int start2 = html.indexOf(registration_start, end1) + registration_start.length();
        int end2 = html.indexOf("</label>", start2);

        return html.substring(start1, end1).replace("&nbsp;","") + "/" + html.substring(start2, end2).replace("&nbsp;","");
    }

    private void insertIntoDatabase() {
        HashMap<String, String> hashMap = new HashMap<>();
        try {
            hashMap.put("parties", jsonObject.getString("description"));
            hashMap.put("case_no", jsonObject.getString("type_and_no"));
            hashMap.put("next_date", findNextDate(html));
            hashMap.put("html", html);
            hashMap.put("json", jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        CaseDatabaseHelper.getInstance(this).addCase(hashMap);
        new MaterialDialog.Builder(CaseSearchWebview.this)
                .title("Case Saved")
                .content("This case has been added to the Database.\nTo continue Press OK")
                .positiveText("OK")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        save_button.setVisibility(View.INVISIBLE);
                    }
                })
                .show();
    }

    private String findNextDate(String html) {
        String searchstr, next_date = "";
        int start, end;
        searchstr = "Next Hearing Date</strong><strong style='float:left;'>:";
        start = html.indexOf(searchstr) + searchstr.length();
        end = html.indexOf("<", start);
        if (start != searchstr.length() - 1) {
            next_date = html.substring(start, end);
            String date[] = next_date.split(" ");
            next_date = date[0].replace("th", "").replace("st", "").replace("nd", "").replace("rd", "").replace("&nbsp;", "") + " " + date[1].replace("&nbsp;", "") + " " + date[2].replace("&nbsp;", "");
        } else if (html.contains("Decision Date")) {
            return "Decided";
        }
        return next_date;
    }
}
