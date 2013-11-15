package com.cs253.appdebugger;

import com.cs253.appdebugger.AppListAdapter;
import com.cs253.appdebugger.App;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.content.Context;

/**
 * This demo program displays all currently installed apps of the device in a list. An app can be started
 * upon clicking on its row.
 *
 * Copyright 2k11 Impressive Artworx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Manuel Schwarz (m.schwarz[at]impressive-artworx.de)
 */
public class ListApps extends Activity implements OnItemClickListener {

    /* whether or not to include system apps */
    private static final boolean INCLUDE_SYSTEM_APPS = false;

    private ListView mAppsList;
    private AppListAdapter mAdapter;
    private List<App> mApps;
    private Context context;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate our list apps view
        setContentView(R.layout.activity_listapps);
        // It's always handy to keep our app's context around for use elsewhere
        this.context = getApplicationContext();

        // Inflate our listview view
        mAppsList = (ListView) findViewById(R.id.appslist);
        // add a listener to this view
        mAppsList.setOnItemClickListener(this);

        // Get the apps that are on the system
        mApps = loadInstalledApps(INCLUDE_SYSTEM_APPS);

        // Add a listadapter to our view
        mAdapter = new AppListAdapter(getApplicationContext());
        // put the apps onto our listview
        mAdapter.setListItems(mApps);
        // add the listener to our listview
        mAppsList.setAdapter(mAdapter);

        // Load our app's icons in the background so we don't leave our main thread
        // hangin if a problem arises
        new LoadIconsTask().execute(mApps.toArray(new App[]{}));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        final App app = (App) parent.getItemAtPosition(position);

        Intent intent = new Intent(view.getContext(), AppDetails.class);

        // Toast.makeText(getApplicationContext(), app.getPackageName(), Toast.LENGTH_LONG).show();

        intent.putExtra("packageName", app.getPackageName());
        startActivity(intent);
/*
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String msg = app.getTitle() + "\n\n" +
                "Version " + app.getVersionName() + " (" +
                app.getVersionCode() + ")" +
                (app.getDescription() != null ? ("\n\n" + app.getDescription()) : "");

        builder.setMessage(msg)
                .setCancelable(true)
                .setTitle(app.getTitle())
                .setIcon(mAdapter.getIcons().get(app.getPackageName()))
                .setPositiveButton("Launch", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // start the app by invoking its launch intent
                        Intent i = getPackageManager().getLaunchIntentForPackage(app.getPackageName());
                        try {
                            if (i != null) {
                                startActivity(i);
                            } else {
                                i = new Intent(app.getPackageName());
                                startActivity(i);
                            }
                        } catch (ActivityNotFoundException err) {
                            Toast.makeText(ListApps.this, "Error launching app", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
        */
    }

    /**
     * Uses the package manager to query for all currently installed apps which are put into beans and returned
     * in form of a list.
     *
     * @param includeSysApps whether or not to include system applications
     * @return a list containing an {@code App} bean for each installed application
     */
    private List<App> loadInstalledApps(boolean includeSysApps) {
        List<App> apps = new ArrayList<App>();

        // the package manager contains the information about all installed apps
        PackageManager packageManager = getPackageManager();

        List<PackageInfo> packs = packageManager.getInstalledPackages(0); //PackageManager.GET_META_DATA

        for(int i=0; i < packs.size(); i++) {
            PackageInfo p = packs.get(i);
            ApplicationInfo a = p.applicationInfo;
            // skip system apps if they shall not be included
            if ((!includeSysApps) && ((a.flags & ApplicationInfo.FLAG_SYSTEM) == 1)) {
                continue;
            }
            App app = new App();
            app.setTitle(p.applicationInfo.loadLabel(packageManager).toString());
            app.setPackageName(p.packageName);
            app.setVersionName(p.versionName);
            app.setVersionCode(p.versionCode);
            CharSequence description = p.applicationInfo.loadDescription(packageManager);
            app.setDescription(description != null ? description.toString() : "");
            app.setUid(a.uid);
            apps.add(app);
        }
        return apps;
    }

    /**
     * An asynchronous task to load the icons of the installed applications.
     */
    private class LoadIconsTask extends AsyncTask<App, Void, Void> {
        @Override
        protected Void doInBackground(App... apps) {

            Map<String, Drawable> icons = new HashMap<String, Drawable>();
            PackageManager manager = getApplicationContext().getPackageManager();

            for (App app : apps) {
                String pkgName = app.getPackageName();
                Drawable ico = null;
                try {
                    Intent i = manager.getLaunchIntentForPackage(pkgName);
                    if (i != null) {
                        ico = manager.getActivityIcon(i);
                    }
                } catch (NameNotFoundException e) {
                    Log.e("ERROR", "Unable to find icon for package '" + pkgName + "': " + e.getMessage());
                }
                icons.put(app.getPackageName(), ico);
            }
            mAdapter.setIcons(icons);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mAdapter.notifyDataSetChanged();
        }
    }
}