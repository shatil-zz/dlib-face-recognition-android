package com.google.android.cameraview.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.tzutalin.dlib.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        findViewById(R.id.tv_add_employee).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermissions()) {
                    startActivity(new Intent(getApplicationContext(), AddEmployeeActivity.class));
                }
            }
        });
        findViewById(R.id.tv_check_employee).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermissions()) {
                    startActivity(new Intent(getApplicationContext(), CheckEmployeeActivity.class));
                }
            }
        });
        checkPermissions();
        checkFileCopied();
    }

    String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private void checkFileCopied() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            new initRecAsync().execute();
        }
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        checkFileCopied();
    }

    private class initRecAsync extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... args) {
            File folder = new File(Constants.getDLibDirectoryPath());
            boolean success = false;
            if (!folder.exists()) {
                success = folder.mkdirs();
            }
            if (success) {
                File image_folder = new File(Constants.getDLibImageDirectoryPath());
                image_folder.mkdirs();
                if (!new File(Constants.getFaceShapeModelPath()).exists()) {
                    Log.d("HomeActivity", "Copy Face shape model");
                    FileUtils.copyFileFromRawToOthers(getApplicationContext(), R.raw.shape_predictor_5_face_landmarks, Constants.getFaceShapeModelPath());
                } else {
                    Log.d("HomeActivity", "Face shape model already exists");
                }
                if (!new File(Constants.getFaceDescriptorModelPath()).exists()) {
                    Log.d("HomeActivity", "Copy Face descriptor model");
                    FileUtils.copyFileFromRawToOthers(getApplicationContext(), R.raw.dlib_face_recognition_resnet_model_v1, Constants.getFaceDescriptorModelPath());
                } else {
                    Log.d("HomeActivity", "Face descriptor model already exists");
                }
            } else {
                Log.d("HomeActivity", "Directory not created->Folder already exists:" + folder.exists());
            }
            Log.d("HomeActivity", "Try to copy finished");
            return null;
        }
    }
}
