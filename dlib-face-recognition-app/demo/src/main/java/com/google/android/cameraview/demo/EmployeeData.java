package com.google.android.cameraview.demo;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

public class EmployeeData {
    private JSONObject employeeData;
    private Context context;
    private static EmployeeData instance;
    private static String EMPLOYEE_INFO_SHARED_PREF = "employee_info_shared_pref";

    private EmployeeData(Context context) {
        this.context = context;
        loadFromPref();
    }

    public static EmployeeData get(Context context) {
        if (instance == null) {
            instance = new EmployeeData(context);
        }
        return instance;
    }

    public String getEmployeeName(String id) {
        try {
            return employeeData.getJSONObject(id).getString("name");
        } catch (Exception e) {
            return null;
        }
    }

    public String getEmployeeImageUrl(String id) {
        try {
            return employeeData.getJSONObject(id).getString("image_url");
        } catch (Exception e) {
            return null;
        }
    }

    public void setEmployeeName(String id, String name) {
        JSONObject object = new JSONObject();
        try {
            if (employeeData.has(id)) object = employeeData.getJSONObject(id);
        } catch (Exception e) {
        }
        try {
            object.put("name", name);
            employeeData.put(id, object);
        } catch (Exception e) {

        }
    }

    public void setEmployeeImageUrl(String id, String imageUrl) {
        JSONObject object = new JSONObject();
        try {
            if (employeeData.has(id)) object = employeeData.getJSONObject(id);
        } catch (Exception e) {
        }
        try {
            object.put("image_url", imageUrl);
            employeeData.put(id, object);
        } catch (Exception e) {

        }
    }

    public boolean hasDetails(String id) {
        try {
            return employeeData.has(id);
        } catch (Exception e) {
            return false;
        }
    }

    private void loadFromPref() {
        SharedPreferences prefs = context.getSharedPreferences(EMPLOYEE_INFO_SHARED_PREF, Context.MODE_PRIVATE);
        try {
            employeeData = new JSONObject(prefs.getString("data", "{}"));
        } catch (Exception e) {
            employeeData = new JSONObject();
        }
    }

    public void commit() {
        SharedPreferences saved_values = context.getSharedPreferences(EMPLOYEE_INFO_SHARED_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = saved_values.edit();
        editor.putString("data", employeeData.toString());
        editor.apply();
    }
}
