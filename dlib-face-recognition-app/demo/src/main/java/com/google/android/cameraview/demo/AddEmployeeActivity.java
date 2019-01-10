/**
 * Created by Gaurav on Feb 23, 2018
 */

package com.google.android.cameraview.demo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceRec;
import com.tzutalin.dlib.VisionDetRet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

// Copy the person image renamed to his name into the dlib image directory
public class AddEmployeeActivity extends AppCompatActivity {

    EditText et_name, et_id;
    ImageView ivPhoto;
    Button btn_select_image, btn_add;
    int BITMAP_QUALITY = 100;
    int MAX_IMAGE_SIZE = 500;
    String TAG = "AddPerson";
    private Bitmap bitmap;
    private File destination = null;
    private String imgPath = null;
    private final int PICK_IMAGE_CAMERA = 1, PICK_IMAGE_GALLERY = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_person);
        btn_select_image = findViewById(R.id.btn_select_image);
        btn_add = findViewById(R.id.btn_add);
        et_name = findViewById(R.id.et_name);
        et_id = findViewById(R.id.et_id);
        ivPhoto = findViewById(R.id.iv_photo);
        btn_select_image.setOnClickListener(mOnClickListener);
        btn_add.setOnClickListener(mOnClickListener);
        destination = new File(Constants.getDLibDirectoryPath() + "/temp.jpg");
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_select_image:
                    selectImage();
                    break;
                case R.id.btn_add:
                    if (isReadyToAdd()) {
                        String targetPath = Constants.getDLibImageDirectoryPath() + "/" + getId() + ".jpg";
                        FileUtils.copyFile(imgPath, targetPath);
                        EmployeeData employeeData = EmployeeData.get(getApplicationContext());
                        employeeData.setEmployeeName(getId(), getName());
                        employeeData.setEmployeeImageUrl(getId(), targetPath);
                        employeeData.commit();
                        Intent intent = new Intent(getApplicationContext(), EmployeeDetailsActivity.class);
                        intent.putExtra("id", getId());
                        startActivity(intent);
                        finish();
                    }
                    break;
            }
        }
    };

    private boolean isReadyToAdd() {
        boolean isReady = true;
        if (getName().length() == 0) {
            et_name.setError("Enter name");
            isReady = false;
        }
        if (getId().length() == 0) {
            et_id.setError("Enter Id");
            isReady = false;
        }
        if (imgPath == null) {
            Toast.makeText(this, "Please set a face", Toast.LENGTH_SHORT).show();
            isReady = false;
        }
        return isReady;
    }

    private String getName() {
        return et_name.getText().toString().trim();
    }

    private String getId() {
        return et_id.getText().toString().trim();
    }

    // Select image from camera and gallery
    private void selectImage() {
        try {
            PackageManager pm = getPackageManager();
            int hasPerm = pm.checkPermission(Manifest.permission.CAMERA, getPackageName());
            if (hasPerm == PackageManager.PERMISSION_GRANTED) {
                final CharSequence[] options = {"Take Photo", "Choose From Gallery", "Cancel"};
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(AddEmployeeActivity.this);
                builder.setTitle("Select Option");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (options[item].equals("Take Photo")) {
                            dialog.dismiss();
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(intent, PICK_IMAGE_CAMERA);
                        } else if (options[item].equals("Choose From Gallery")) {
                            dialog.dismiss();
                            Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(pickPhoto, PICK_IMAGE_GALLERY);
                        } else if (options[item].equals("Cancel")) {
                            dialog.dismiss();
                        }
                    }
                });
                builder.show();
            } else
                Toast.makeText(this, "Camera Permission error", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Camera Permission error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_CAMERA) {
            try {
                bitmap = (Bitmap) data.getExtras().get("data");
                Bitmap scaledBitmap = scaleDown(bitmap, MAX_IMAGE_SIZE, true);
                new detectAsync().execute(scaledBitmap);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (requestCode == PICK_IMAGE_GALLERY) {
            Uri selectedImage = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                Bitmap scaledBitmap = scaleDown(bitmap, MAX_IMAGE_SIZE, true);
                new detectAsync().execute(scaledBitmap);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Audio.Media.DATA};
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize, boolean filter) {
        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, filter);
        return newBitmap;
    }

    private FaceRec mFaceRec;

    private class detectAsync extends AsyncTask<Bitmap, Void, String> {
        ProgressDialog dialog = new ProgressDialog(AddEmployeeActivity.this);

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Detecting face...");
            dialog.setCancelable(false);
            dialog.show();
            super.onPreExecute();
        }

        protected String doInBackground(Bitmap... bp) {
            mFaceRec = new FaceRec(Constants.getDLibDirectoryPath());
            List<VisionDetRet> results;
            results = mFaceRec.detect(bp[0]);
            String msg = null;
            if (results.size() == 0) {
                msg = "No face was detected or face was too small. Please select a different image";
            } else if (results.size() > 1) {
                msg = "More than one face was detected. Please select a different image";
            } else {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bp[0].compress(Bitmap.CompressFormat.JPEG, BITMAP_QUALITY, bytes);
                FileOutputStream fo;
                try {
                    destination.createNewFile();
                    fo = new FileOutputStream(destination);
                    fo.write(bytes.toByteArray());
                    fo.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imgPath = destination.getAbsolutePath();
            }
            return msg;
        }

        protected void onPostExecute(String result) {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
                if (result != null) {
                    MsgUtils.showSnackBarDefault(btn_add, result);
                    imgPath = null;
                    ivPhoto.setImageResource(R.drawable.ic_profile_icon);
                } else {
                    ivPhoto.setImageURI(Uri.parse(imgPath));
                }
            }

        }
    }

}
