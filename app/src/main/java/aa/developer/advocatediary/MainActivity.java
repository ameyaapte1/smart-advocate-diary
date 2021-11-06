package aa.developer.advocatediary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.arlib.floatingsearchview.FloatingSearchView;



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;



public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "BaseDriveActivity";

    private ListView listView_casestore;
    private BroadcastReceiver mMessageReceiver;
    private CursorAdapter cursorAdapter;
    private TextView textView_count,textViewName,textViewEmail;
    private int filter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final FloatingSearchView floatingSearchView = findViewById(R.id.floating_search_view);


        textView_count = findViewById(R.id.case_count);
        if (listView_casestore == null) {
            listView_casestore = findViewById(R.id.casestore_listview);
        }


        refresh_cursor();
        registerRefreshBroadcast();

        listView_casestore.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, CaseDetailsActivity.class);
                intent.putExtra("html", CaseDatabaseHelper.getInstance(getApplicationContext()).getCaseHTML(((TextView) (view.findViewById(R.id.textview_db_id))).getText().toString()));
                intent.putExtra("id", ((TextView) view.findViewById(R.id.textview_db_id)).getText().toString());
                startActivity(intent);
            }
        });

        final SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                OneTimeWorkRequest oneTimeCaseSyncWorkRequest =
                        new OneTimeWorkRequest.Builder(CaseSyncWorker.class)
                                // Constraints
                                .build();
                WorkManager workManager = WorkManager.getInstance(getApplicationContext());
                workManager.enqueueUniqueWork("CaseSyncWorkOneTime", ExistingWorkPolicy.REPLACE,oneTimeCaseSyncWorkRequest);

                swipeRefreshLayout.setRefreshing(false);
            }
        });

        LinearLayout linearLayout_empty = findViewById(R.id.emty_list);

        listView_casestore.setEmptyView(linearLayout_empty);
        listView_casestore.setAdapter(cursorAdapter);


        final String districts_array[] = getResources().getStringArray(R.array.districts);
        final ArrayList<String> district_list = new ArrayList<>();
        final HashMap<String, String> districts = new HashMap<>();
        for (String district : districts_array) {
            String splitresult[] = district.split(",");
            districts.put(splitresult[1], splitresult[0]);
            district_list.add(splitresult[1]);
        }

        FloatingActionButton floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(MainActivity.this)
                        .title("Search Criteria")
                        .items(R.array.search_types)
                        .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                final String search_type;
                                switch (which) {
                                    case 0:
                                        search_type = "ki_petres.php";
                                        break;
                                    case 1:
                                        search_type = "case_no.php";
                                        break;
                                    case 2:
                                        search_type = "fir1.php";
                                        break;
                                    case 3:
                                        search_type = "qs_civil_advocate.php";
                                        break;
                                    default:
                                        search_type = "ki-petres.php";
                                }
                                new MaterialDialog.Builder(MainActivity.this)
                                        .title("Select District")
                                        .items(district_list)
                                        .itemsCallback(new MaterialDialog.ListCallback() {
                                            @Override
                                            public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                                                Intent intent = new Intent(MainActivity.this, CaseSearchWebview.class);
                                                intent.putExtra("search_url", search_type);
                                                intent.putExtra("dist_code", districts.get(text));
                                                startActivity(intent);
                                            }
                                        })
                                        .positiveText(android.R.string.cancel)
                                        .show();
                                return true;
                            }
                        })
                        .positiveText("Proceed")
                        .show();

            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        floatingSearchView.attachNavigationDrawerToMenuButton(drawer);
        floatingSearchView.setDismissOnOutsideClick(true);
        floatingSearchView.setDismissFocusOnItemSelection(true);
        floatingSearchView.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
            @Override
            public void onSearchTextChanged(String oldQuery, String newQuery) {
                cursorAdapter.getFilter().filter(newQuery);
            }
        });

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View v = navigationView.getHeaderView(0);
         textViewName = v.findViewById(R.id.nametextView);
         textViewEmail = v.findViewById(R.id.emailtextView);

        PeriodicWorkRequest caseSyncWorkRequest =
                new PeriodicWorkRequest.Builder(CaseSyncWorker.class, 6, TimeUnit.HOURS)
                        // Constraints
                        .build();
        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        workManager.enqueueUniquePeriodicWork("PeriodicCaseSyncWork", ExistingPeriodicWorkPolicy.KEEP,caseSyncWorkRequest);

    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh_cursor();
    }




    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        final TextView textView_filter = findViewById(R.id.filter_text);

        if (id == R.id.nav_filters) {
            new MaterialDialog.Builder(MainActivity.this)
                    .title("Filters")
                    .items(R.array.filter_types)
                    .itemsCallbackSingleChoice(filter, new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                            filter = which;
                            textView_filter.setText("Current Filter: " + text);
                            refresh_cursor();
                            return false;
                        }
                    })
                    .show();
        } else if (id == R.id.nav_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    "Hey check out my app at: https://play.google.com/store/apps/details?id=aa.developer.advocatediary");
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        } else if (id == R.id.nav_backup_db) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void registerRefreshBroadcast() {
        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get extra data included in the Intent
                String message = intent.getStringExtra("message");
                Log.d("receiver", "Got message: " + message);
                refresh_cursor();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("refresh-list"));
    }

    private void refresh_cursor() {
        Cursor cursor = CaseDatabaseHelper.getInstance(this).getListEntries(filter);
        textView_count.setText("Count: " + String.valueOf(cursor.getCount()));
        if (cursorAdapter == null) {
            cursorAdapter = new CasesCursorAdapter(this, cursor);
            cursorAdapter.setFilterQueryProvider(new FilterQueryProvider() {
                @Override
                public Cursor runQuery(CharSequence constraint) {
                    return CaseDatabaseHelper.getInstance(MainActivity.this).searchCase(constraint.toString(), filter);
                }
            });
        } else {
            cursorAdapter.swapCursor(cursor).close();
        }
    }

    private class CasesCursorAdapter extends CursorAdapter implements Filterable {
        CasesCursorAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
        }

        // The newView method is used to inflate a new view and return it,
        // you don't bind any data to the view at this point.
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.casestore_rowlayout, parent, false);
        }

        // The bindView method is used to bind all data to a given view
        // such as setting the text on a TextView.
        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            // Find fields to populate in inflated template
            TextView textView_Name = view.findViewById(R.id.textview_petres_name);
            TextView textView_Case_No = view.findViewById(R.id.textview_case_no);
            TextView textView_Next_Date = view.findViewById(R.id.textview_case_next_date);
            TextView textView_DB_Id = view.findViewById(R.id.textview_db_id);
            ImageButton shareCaseButton = view.findViewById(R.id.sharecase_button);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMMM-yyyy", Locale.ENGLISH);
            Long tomorrow = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(18);

            // Extract properties from cursor
            final String name, case_no, next_date, id;
            Long db_date;
            db_date = Long.parseLong(cursor.getString(cursor.getColumnIndexOrThrow("ndate")));
            id = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
            name = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            case_no = cursor.getString(cursor.getColumnIndexOrThrow("case_no"));
            if (db_date > 0) {
                if (db_date < tomorrow)
                    next_date = simpleDateFormat.format(new Date(db_date)) + "\nDate Not Updated";
                else
                    next_date = simpleDateFormat.format(new Date(db_date));
            } else {
                next_date = "Case Decided";
            }
            // Populate fields with extracted properties
            textView_Name.setText(name);
            textView_Case_No.setText(case_no);
            textView_Next_Date.setText(next_date);
            textView_DB_Id.setText(id);

            shareCaseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String content = "The next date for your case:\nCase No: " + case_no + "\n" + "Next Date: " + next_date + "\n";
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, content);
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, "Share Case Details"));
                }
            });
        }
    }
}
