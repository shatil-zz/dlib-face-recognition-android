package com.tzutalin.dlib;

import android.os.Environment;

import java.io.File;

/**
 * Created by darrenl on 2016/4/22.
 * Modified by Gaurav on Feb 23, 2018
 */

public final class Constants {
    private Constants() {
        // Constants should be prive
    }

    public static String getDLibDirectoryPath(String id) {
        File sdcard = Environment.getExternalStorageDirectory();
        String targetPath = sdcard.getAbsolutePath() + File.separator + "EmployeeData" + File.separator + id;
        return targetPath;
    }

    public static String getDLibImageDirectoryPath(String id) {
        String targetPath = getDLibDirectoryPath(id) + File.separator + "images";
        return targetPath;
    }

    public static String getFaceShapeModelPath(String id) {
        String targetPath = getDLibDirectoryPath(id) + File.separator + "shape_predictor_5_face_landmarks.dat";
        return targetPath;
    }

    public static String getFaceDescriptorModelPath(String id) {
        String targetPath = getDLibDirectoryPath(id) + File.separator + "dlib_face_recognition_resnet_model_v1.dat";
        return targetPath;
    }
}
