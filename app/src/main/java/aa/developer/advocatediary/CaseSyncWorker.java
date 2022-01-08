package aa.developer.advocatediary;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.util.MalformedJsonException;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Iterator;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class CaseSyncWorker extends Worker {
    private Context context;
    private CookieManager cookieManager;
    //private String baseurl = "https://court.mah.nic.in/courtweb/cases/";
    private String baseurl = "https://services.ecourts.gov.in/ecourtindia_v4_bilingual/cases/";


    public CaseSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        refresh_date();
        return null;
    }

    @SuppressLint("Range")
    private boolean refresh_date() {
        Boolean ret = true;
        CaseDatabaseHelper dbInstance = CaseDatabaseHelper.getInstance(context);
        Cursor cursor = dbInstance.getUpdatableEntries();
        while (cursor.moveToNext()) {
            JSONObject jsonObject;
            String case_no;
            String result, next_date, csrf_token;
            try {
                jsonObject = new JSONObject(cursor.getString(cursor.getColumnIndex("jcase")));
                case_no = (String) jsonObject.remove("type_and_no");
                String civil_history = "o_civil_case_history.php";
                result = postJSONObject(baseurl + civil_history, jsonObject, true);

                if (result.contains("ERROR")) {
                    ret = false;
                    continue;
                } else {
                    /*Pattern pattern_csrf = Pattern.compile("[0-9]*");
                    Matcher matcher_csrf = pattern_csrf.matcher(result);
                    if(matcher_csrf.find())
                        input.put("__csrf_magic",matcher_csrf.group(0));*/
                    if(result.contains("csrfMagicToken")){
                        csrf_token = result.split("csrfMagicToken = \"")[1].split("\"")[0];
                        jsonObject.put("__csrf_magic", csrf_token);
                        result = postJSONObject(baseurl + civil_history, jsonObject, false);
                    }
                    next_date = findNextDate(result);
                }

                String db_next_date = CaseDatabaseHelper.getInstance(context).getCaseDate(case_no);
                if (!next_date.isEmpty() && !db_next_date.equals(next_date)) {
                    boolean updated = dbInstance.updateCaseDateHtml(case_no, next_date, result);
                    if (updated) {

                        Intent intent = new Intent("refresh-list");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                        String CHANNEL_ID = "case_channel";
                        NotificationManager mNotificationManager =
                                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "Case Updates", NotificationManager.IMPORTANCE_DEFAULT);

                            // Configure the notification channel.
                            notificationChannel.setDescription("Case Updation");
                            notificationChannel.enableLights(true);
                            notificationChannel.enableVibration(true);
                            if (mNotificationManager != null) {
                                mNotificationManager.createNotificationChannel(notificationChannel);
                            }
                        }
                        NotificationCompat.Builder mBuilder =
                                new NotificationCompat.Builder(context, CHANNEL_ID)
                                        .setGroup("CASE_UPDATE")
                                        .setSmallIcon(R.mipmap.ic_launcher)
                                        .setContentTitle("Case Updated: " + case_no)
                                        .setContentText(jsonObject.get("description") + "\nNext Date: " + next_date);
// Creates an explicit intent for an Activity in your app
                        Intent resultIntent = new Intent(context, MainActivity.class);
// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your app to the Home screen.
                        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
// Adds the back stack for the Intent (but not the Intent itself)
                        stackBuilder.addParentStack(MainActivity.class);
// Adds the Intent that starts the Activity to the top of the stack
                        stackBuilder.addNextIntent(resultIntent);
                        PendingIntent resultPendingIntent =
                                stackBuilder.getPendingIntent(
                                        0,
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                );
                        mBuilder.setContentIntent(resultPendingIntent);

// mNotificationId is a unique integer your app uses to identify the
// notification. For example, to cancel the notification, you can pass its ID
// number to NotificationManager.cancel().
                        if (mNotificationManager != null) {
                            mNotificationManager.notify(937, mBuilder.build());
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        BackupManager bmgr = new BackupManager(context);
        bmgr.dataChanged();
        return ret;
    }

    private String findNextDate(String html) {
        String searchstr, next_date = "";
        int start, end;
        searchstr = "Next Hearing Date</strong><strong style='float:left;'>:&nbsp;";
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



    private String postJSONObject(String url, JSONObject jsonObject, boolean isNew) {
        String nullresult = "ERROR";
        try {
            StringBuilder data = new StringBuilder();
            String key;
            Iterator<String> jsonIterator = jsonObject.keys();
            while (jsonIterator.hasNext()) {
                key = jsonIterator.next();
                if(!key.equals("description")) {
                    data.append("&").append(key).append("=").append(jsonObject.get(key));
                }
            }
            data.append("&lang=");
            data = new StringBuilder(data.substring(1));
            URL object = new URL(url);


            if (isNew)
                cookieManager = new CookieManager();

            CookieHandler.setDefault(cookieManager);
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) object.openConnection();

            httpsURLConnection.setDoInput(true);
            httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpsURLConnection.setRequestProperty("Accept", "application/x-www-form-urlencoded");
            httpsURLConnection.setRequestMethod("POST");

            httpsURLConnection.setConnectTimeout(10000);
            httpsURLConnection.setReadTimeout(10000);

            OutputStreamWriter wr = new OutputStreamWriter(httpsURLConnection.getOutputStream());
            wr.write(data.toString());
            wr.flush();

            StringBuilder sb = new StringBuilder();
            int HttpResult = httpsURLConnection.getResponseCode();
            if (HttpResult == HttpURLConnection.HTTP_FORBIDDEN) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(httpsURLConnection.getErrorStream(), "utf-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
            }

            if (HttpResult == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(httpsURLConnection.getInputStream(), "utf-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
            }
            if (sb.toString().isEmpty())
                return nullresult;
            else {
                return sb.toString();
            }
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedJsonException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return nullresult;
    }

}
