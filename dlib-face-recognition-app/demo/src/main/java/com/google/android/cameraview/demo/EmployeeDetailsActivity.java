package com.google.android.cameraview.demo;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class EmployeeDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_details);
        String id = getIntent().getExtras().getString("id");
        EmployeeData data = EmployeeData.get(getApplicationContext());
        findViewById(R.id.btn_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        ((TextView) findViewById(R.id.tv_name)).setText(data.getEmployeeName(id));
        ((TextView) findViewById(R.id.tv_id)).setText(id);
        ((ImageView) findViewById(R.id.iv_photo)).setImageURI(Uri.parse(data.getEmployeeImageUrl(id)));
    }
}
