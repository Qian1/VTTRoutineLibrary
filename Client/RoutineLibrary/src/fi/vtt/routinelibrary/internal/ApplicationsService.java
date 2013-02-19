/*
 * Copyright (c) 2013, VTT Technical Research Centre of Finland 
 * All rights reserved. 
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met: 
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in the 
 *    documentation and/or other materials provided with the distribution. 
 * 3. Neither the name of the VTT Technical Research Centre of Finland nor the 
 *    names of its contributors may be used to endorse or promote products 
 *    derived from this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR 
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 * 
 */

package fi.vtt.routinelibrary.internal;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.util.Log;
import fi.vtt.routinelibrary.common.Application;
import java.util.ArrayList;
import java.util.List;

/**
 * ApplicationsService. 
 *
 */

public class ApplicationsService {

    private final Context context;

    private List<String>  previousRecentApplicationsList = new ArrayList<String>();

    public ApplicationsService(Context contextIncoming) {
        context = contextIncoming;
    }

    public List<Application> findCurrentApplications() {
        Log.d("ApplicationsService", "findCurrentApplications()");

        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);

        PackageManager packageManager = context.getPackageManager();

        final int MAX_APPLICATIONS = 50;

        List<RecentTaskInfo> recentTasksList = activityManager.getRecentTasks(MAX_APPLICATIONS, 0);

        List<RunningTaskInfo> runningTasksList = activityManager.getRunningTasks(MAX_APPLICATIONS);

        List<String> actualRunningApplicationsDuringTheLastTimePeriodList = new ArrayList<String>();
        List<String> list = new ArrayList<String>();
        List<String> recentApplicationsList = new ArrayList<String>();
        List<String> runningApplicationsList = new ArrayList<String>();

        Log.d("ApplicationsService", "got recent lists");
        Log.d("ApplicationsService", Integer.toString(recentTasksList.size()));

        final String NAME_CLASSNAME_DELIM = ",";
        final String APP_LINE_DELIM = ";";

        for (int i = 0; i < recentTasksList.size(); i++) {
            Intent intent = recentTasksList.get(i).baseIntent;

            ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 0);

            if (resolveInfo != null) {
            	ActivityInfo activityInfo = resolveInfo.activityInfo;

            	String launchIDString = activityInfo.name;
            	String nameString = (String) activityInfo.loadLabel(packageManager);

            	recentApplicationsList.add(nameString + NAME_CLASSNAME_DELIM + launchIDString);
            }
        }

        list.addAll(recentApplicationsList);

        // Create list of running tasks (name. package name):
        for (int i = 0; i < runningTasksList.size(); i++) {
            ComponentName componentName = runningTasksList.get(i).baseActivity;

            ActivityInfo activityInfo;

            try {
                activityInfo = packageManager.getActivityInfo(componentName, 0);

                String launchIDString = activityInfo.name;
                String nameString = (String) activityInfo.loadLabel(packageManager);

                runningApplicationsList.add(nameString + NAME_CLASSNAME_DELIM + launchIDString);
            }
            catch (NameNotFoundException nameNotFoundExceptionIncoming) {
            	nameNotFoundExceptionIncoming.printStackTrace();
            }
        }

        // Mark package name with magic value if a task already exists in previously searched list:
        final String REMOVE_MARKER = "REMOVETHIS";

        for (int i = 0; i < previousRecentApplicationsList.size(); i++) {

            if (list.contains(previousRecentApplicationsList.get(i))) {
                int j = list.indexOf(previousRecentApplicationsList.get(i));

                if (i <= j) {
                    String string = list.get(j).toString().concat(REMOVE_MARKER);
                    list.set(j, string);
                }

            }

        }

        // Remove already existing tasks:
        for (int i = (list.size() - 1); i > -1; i--) {
            if (list.get(i).toString().contains(REMOVE_MARKER)) {
                list.remove(i);
            }
        }

        for (int i = 0; i < runningApplicationsList.size(); i++) {
            if (list.contains(runningApplicationsList.get(i))) {
                list.remove(runningApplicationsList.get(i));
            }
        }

        actualRunningApplicationsDuringTheLastTimePeriodList = runningApplicationsList;
        actualRunningApplicationsDuringTheLastTimePeriodList.addAll(list);

        // Construct delimited string of running applications:
        StringBuilder runningAppsBuilder = new StringBuilder();

        int size = actualRunningApplicationsDuringTheLastTimePeriodList.size();

        for (int i = 0; i < size; i++) {
            String appStr = actualRunningApplicationsDuringTheLastTimePeriodList.get(i).toString();

            if (i == (actualRunningApplicationsDuringTheLastTimePeriodList.size() - 1)) {
                // Last element, no new app line delimiter:
                runningAppsBuilder.append(appStr);
            }
            else {
                runningAppsBuilder.append(appStr + APP_LINE_DELIM);
            }
        }

        List<Application> applicationsList = new ArrayList<Application>(size);

        String[] runningApplicationsStringArray = runningAppsBuilder.toString().split(APP_LINE_DELIM);

        for (int i = 0; i < runningApplicationsStringArray.length; i++) {
            Log.d("ApplicationsService, apps", runningApplicationsStringArray[i].toString());

            String[] applicationStringArray = runningApplicationsStringArray[i].toString().split(NAME_CLASSNAME_DELIM);

            String name = applicationStringArray[0];
            String className = applicationStringArray[1];

            final String LAUNCHTIME = "";

            Application app = new Application(name, className, LAUNCHTIME);

            applicationsList.add(app);
        }

        return applicationsList;
    }

}