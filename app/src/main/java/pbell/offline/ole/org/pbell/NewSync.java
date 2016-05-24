package pbell.offline.ole.org.pbell;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.replicator.ReplicationState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CountDownLatch;

public class NewSync extends AppCompatActivity {

    public static final String PREFS_NAME = "MyPrefsFile";
    SharedPreferences settings;
    String sys_oldSyncServerURL,sys_username,sys_lastSyncDate= "";
    TextView tv;
    View clcview;
    String message="";
    //////Replication push,pull;
    FloatingActionButton fab;
    Boolean wipeClearn =false;
    final Context context = this;
    String[] databaseList = {"members","membercourseprogress","meetups","usermeetups","assignments",
            "calendar","groups","invitations","requests","shelf","languages"};

    Replication[] push = new Replication[databaseList.length];
    Replication[] pull= new Replication[databaseList.length];

    Database[] db = new Database[databaseList.length];
    Manager[] manager = new Manager[databaseList.length];

    ProgressDialog[] progressDialog = new ProgressDialog[databaseList.length];

    AndroidContext androidContext;
    int syncCnt=0;
    Button btnStartSyncPull,btnClose;

    Boolean members,membercourseprogress,meetups,
            usermeetups,assignments,calendar,groups,
            invitations,languages,shelf,requests = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sync);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_New);
        setSupportActionBar(toolbar);

        androidContext = new AndroidContext(this);
        // Restore preferences
        settings = getSharedPreferences(PREFS_NAME, 0);
        sys_username = settings.getString("pf_username","");
        sys_oldSyncServerURL = settings.getString("pf_sysncUrl","");
        sys_lastSyncDate = settings.getString("pf_lastSyncDate","");
        //tv = (TextView)findViewById(R.id.txtLogConsole);

        TextView lblSyncURL =  (TextView)findViewById(R.id.lblSyncSyncUrl_New);
        lblSyncURL.setText(sys_oldSyncServerURL);

        tv = (TextView)findViewById(R.id.txtLogConsole_New);
        tv.setMovementMethod(new ScrollingMovementMethod());

        btnClose = (Button)findViewById(R.id.btnCloseWindow);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }

        });

        btnStartSyncPull = (Button)findViewById(R.id.btnstartSyncPull_New);
        btnStartSyncPull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                syncCnt=0;
                tv.setText(" Sync Started, please wait ... " );
                tv.scrollTo(0,tv.getTop());
                new TestAsyncPull().execute();
            }

        });

        Switch sw_wipeClean = (Switch) findViewById(R.id.swWipeClean_New);
        sw_wipeClean.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                wipeClearn = isChecked;
                ///Log.v("Switch State=", ""+isChecked);
            }
        });


        fab = (FloatingActionButton) findViewById(R.id.checkConnection_New);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clcview = view;
                try {
                    if (checkConnectionURL()) {
                        fab.setVisibility(View.INVISIBLE);
                        Snackbar.make(clcview, "Checking connection to "+sys_oldSyncServerURL +".... please wait", Snackbar.LENGTH_INDEFINITE)
                                .setAction("Action", null).show();
                        new TryDownloadTask().execute(sys_oldSyncServerURL);

                    } else {

                    }
                }catch(Exception e) {
                    Log.v("myErrorTag","Error "+e.getLocalizedMessage());

                }
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    private boolean checkConnectionURL() {
        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            Log.v("myTag","Network Connected");
            return true;
            // fetch data
        } else {

            Log.v("myErrorTag","Network Not Connected");
            return false;
        }

    }

    private class TryDownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            InputStream is = null;
            int response=0;
            try {
                URL url = new URL(sys_oldSyncServerURL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(5000 /* milliseconds */);
                conn.setConnectTimeout(10000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                response = conn.getResponseCode();
                Log.d("Responce", "The response is: " + response);
                is = conn.getInputStream();
                return "";
            }catch (Exception err) {

            }finally
            {
                if(response!=200){
                    displayDownMesage(false);
                }else{
                    displayDownMesage(true);
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return "";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            /// textView.setText(result);
        }
    }
    public void displayDownMesage(boolean status){
        if(status){
            Snackbar.make(clcview, "Connection to established successful", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }else{
            Snackbar.make(clcview, "Sorry , server was unreachable, check url and device connection", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }


    class TestAsyncPull extends AsyncTask<Void, Integer, String> {
        protected void onPreExecute (){
            Log.d("PreExceute","On pre Exceute......");
        }

        protected String doInBackground(Void...arg0) {
            Log.d("DoINBackGround","On doInBackground...");
            pull= new Replication[databaseList.length];
            db = new Database[databaseList.length];
            manager = new Manager[databaseList.length];
            try {
                manager[syncCnt] = new Manager(androidContext, Manager.DEFAULT_OPTIONS);
                if(wipeClearn){
                    try{
                        db[syncCnt] = manager[syncCnt].getExistingDatabase(databaseList[syncCnt]);
                        db[syncCnt].delete();
                    }catch(Exception err){
                        Log.e("MyCouch", "Delete Error "+ err.getLocalizedMessage());
                    }
                }

                db[syncCnt] = manager[syncCnt].getDatabase(databaseList[syncCnt]);
                URL url = new URL(sys_oldSyncServerURL+"/"+databaseList[syncCnt]);
                pull[syncCnt]=  db[syncCnt].createPullReplication(url);
                pull[syncCnt].setContinuous(false);
                pull[syncCnt].addChangeListener(new Replication.ChangeListener() {
                    @Override
                    public void changed(Replication.ChangeEvent event) {
                        if(pull[syncCnt] .isRunning()){
                            Log.e("MyCouch", databaseList[syncCnt]+" "+event.getChangeCount());
                            message = String.valueOf(event.getChangeCount());
                        }else {
                            Log.e("Finished", databaseList[syncCnt]+" "+ db[syncCnt].getDocumentCount());

                            if(syncCnt < (databaseList.length-2)){
                                syncCnt++;
                                new TestAsyncPull().execute();
                            }

                        }
                    }
                });
                pull[syncCnt].start();

            } catch (Exception e) {
                Log.e("MyCouch", databaseList[syncCnt]+" "+" Cannot create database", e);


            }
            publishProgress(syncCnt);
            return "You are at PostExecute";
        }
        protected void onProgressUpdate(Integer...a){
            Log.d("onProgress","You are in progress update ... " + a[0]);
            if(syncCnt != (databaseList.length-2)){
                tv.setText(tv.getText().toString()+" \n Pulled "+databaseList[syncCnt]+ "\n Pulling "+ databaseList[syncCnt+1]+"....." );
                tv.scrollTo(0,(tv.getLineCount()*20+syncCnt));

                tv.requestFocus();
            }else{
                tv.setText(tv.getText().toString()+" \n Pulled "+ databaseList[syncCnt] );
                tv.scrollTo(0,(tv.getLineCount()*20)+syncCnt);
                tv.requestFocus();
                //finishActivity();
            }
        }

        protected void onPostExecute(String result) {
            Log.d("OnPostExec",""+result);
        }
    }
}