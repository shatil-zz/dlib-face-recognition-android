/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modified by Gaurav on Feb 23, 2018

package com.google.android.cameraview.demo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.cameraview.CameraView;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceRec;
import com.tzutalin.dlib.VisionDetRet;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CheckEmployeeActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "CheckActivity";
    private static final int INPUT_SIZE = 500;

    private static final int[] FLASH_OPTIONS = {
            CameraView.FLASH_AUTO,
            CameraView.FLASH_OFF,
            CameraView.FLASH_ON,
    };

    private static final int[] FLASH_ICONS = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on,
    };

    private static final int[] FLASH_TITLES = {
            R.string.flash_auto,
            R.string.flash_off,
            R.string.flash_on,
    };

    private int mCurrentFlash;

    private CameraView mCameraView;

    private Handler mBackgroundHandler;

    Button btnRecognize;
    private FaceRec mFaceRec;
    String employeeId;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_employee);
        employeeId = getIntent().getExtras().getString("id");
        new trainAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        mCameraView = findViewById(R.id.camera);
        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
        }
        btnRecognize = findViewById(R.id.take_picture);
        if (btnRecognize != null) {
            btnRecognize.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EmployeeData employeeData = EmployeeData.get(getApplicationContext());
                    if (mCameraView != null && employeeId.length() > 0 && employeeData.hasDetails(employeeId)) {
                        mCameraView.takePicture();
                    }
                }
            });
        }
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

    }

    private void changeProgressDialogMessage(final ProgressDialog pd, final String msg) {
        Runnable changeMessage = new Runnable() {
            @Override
            public void run() {
                pd.setMessage(msg);
            }
        };
        runOnUiThread(changeMessage);
    }


    @Override
    protected void onResume() {
        Log.d(TAG, "onResume called");
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            mCameraView.start();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause called");
        mCameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called");
        super.onDestroy();
        if (mFaceRec != null) {
            mFaceRec.release();
        }
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.switch_flash:
                if (mCameraView != null) {
                    mCurrentFlash = (mCurrentFlash + 1) % FLASH_OPTIONS.length;
                    item.setTitle(FLASH_TITLES[mCurrentFlash]);
                    item.setIcon(FLASH_ICONS[mCurrentFlash]);
                    mCameraView.setFlash(FLASH_OPTIONS[mCurrentFlash]);
                }
                return true;
            case R.id.switch_camera:
                if (mCameraView != null) {
                    int facing = mCameraView.getFacing();
                    mCameraView.setFacing(facing == CameraView.FACING_FRONT ?
                            CameraView.FACING_BACK : CameraView.FACING_FRONT);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLoading(boolean enable) {
        if (enable) {
            findViewById(R.id.iv_taken_image).setVisibility(View.VISIBLE);
            dialog = new ProgressDialog(CheckEmployeeActivity.this);
            dialog.setMessage("Recognizing...");
            dialog.setCancelable(false);
            dialog.show();
        } else {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            findViewById(R.id.iv_taken_image).setVisibility(View.GONE);
        }
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    private String getResultMessage(ArrayList<String> names) {
        String msg = new String();
        if (names.isEmpty()) {
            msg = "No face detected or Unknown person";

        } else {
            for (int i = 0; i < names.size(); i++) {
                msg += names.get(i);
                if (i != names.size() - 1) msg += ", ";
            }
            msg += " found!";
        }
        return msg;
    }

    private boolean isMatchedWithId(ArrayList<String> ids) {
        String inputId = getEmployeeId();
        for (String id : ids) {
            if (inputId.length() > 0 && inputId.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private String getEmployeeId() {
        return employeeId;
    }

    private synchronized void trainFace() {
        if (mFaceRec == null) {
            long startTime = System.currentTimeMillis();
            mFaceRec = new FaceRec(Constants.getDLibDirectoryPath(employeeId));
            mFaceRec.train();
            Log.d(TAG, "Actual Train time cost: " + String.valueOf((System.currentTimeMillis() - startTime) / 1000f) + " sec");
        }
    }

    private class trainAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            trainFace();
            return null;
        }
    }

    private class recognizeAsync extends AsyncTask<Bitmap, Void, ArrayList<String>> {
        private int mScreenRotation = 0;
        private Bitmap mCroppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);

        @Override
        protected void onPreExecute() {
            showLoading(true);
            super.onPreExecute();
        }

        protected ArrayList<String> doInBackground(Bitmap... bp) {
            long startTime = System.currentTimeMillis();
            drawResizedBitmap(bp[0], mCroppedBitmap);
            Log.d(TAG, "Resize time cost: " + String.valueOf((System.currentTimeMillis() - startTime) / 1000f) + " sec");
            startTime = System.currentTimeMillis();
            trainFace();
            Log.d(TAG, "Recognize Train time cost: " + String.valueOf((System.currentTimeMillis() - startTime) / 1000f) + " sec");
            startTime = System.currentTimeMillis();
            List<VisionDetRet> results;
            results = mFaceRec.recognize(mCroppedBitmap);
            Log.d(TAG, "Recognize time cost: " + String.valueOf((System.currentTimeMillis() - startTime) / 1000f) + " sec");

            ArrayList<String> ids = new ArrayList<>();
            for (VisionDetRet n : results) {
                ids.add(n.getLabel().split(Pattern.quote("."))[0]);
            }
            return ids;
        }

        protected void onPostExecute(ArrayList<String> names) {
            showLoading(false);
            Log.d("DetectedImages", getResultMessage(names));
            if (!isMatchedWithId(names)) {
                MsgUtils.showSnackBarDefault(btnRecognize, "Face not matched");
            } else {
                Intent intent = new Intent(getApplicationContext(), EmployeeDetailsActivity.class);
                intent.putExtra("id", names.get(0));
                startActivity(intent);
                finish();
            }
        }

        private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {
            Display getOrient = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int orientation = Configuration.ORIENTATION_UNDEFINED;
            Point point = new Point();
            getOrient.getSize(point);
            int screen_width = point.x;
            int screen_height = point.y;
            Log.d(TAG, String.format("screen size (%d,%d)", screen_width, screen_height));
            if (screen_width < screen_height) {
                orientation = Configuration.ORIENTATION_PORTRAIT;
                mScreenRotation = 0;
            } else {
                orientation = Configuration.ORIENTATION_LANDSCAPE;
                mScreenRotation = 0;
            }

            Assert.assertEquals(dst.getWidth(), dst.getHeight());
            final float minDim = Math.min(src.getWidth(), src.getHeight());

            final Matrix matrix = new Matrix();

            // We only want the center square out of the original rectangle.
            final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
            final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
            matrix.preTranslate(translateX, translateY);

            final float scaleFactor = dst.getHeight() / minDim;
            matrix.postScale(scaleFactor, scaleFactor);

            // Rotate around the center if necessary.
            if (mScreenRotation != 0) {
                matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
                matrix.postRotate(mScreenRotation);
                matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
            }

            final Canvas canvas = new Canvas(dst);
            canvas.drawBitmap(src, matrix, null);
        }
    }


    private CameraView.Callback mCallback
            = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);
            Bitmap bp = BitmapFactory.decodeByteArray(data, 0, data.length);

            if (bp.getHeight() < bp.getWidth()) {
                Matrix matrix = new Matrix();
                matrix.postRotate(270);
                bp = Bitmap.createBitmap(bp, 0, 0, bp.getWidth(), bp.getHeight(), matrix, true);
            }
            ((ImageView) findViewById(R.id.iv_taken_image)).setImageBitmap(bp);
            new recognizeAsync().execute(bp);
        }

    };

}
