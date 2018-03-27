package com.example.administrator.campreview;

//import android.graphics.Camera;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    Button mEasyOpenCamera;
    Button mGLsurfaceViewPreviewCamera;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEasyOpenCamera = (Button)findViewById(R.id.easy_start);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mEasyOpenCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        mGLsurfaceViewPreviewCamera = (Button)findViewById(R.id.glsurface_camera);
        mGLsurfaceViewPreviewCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, GLSurfaceViewActivity.class);
                startActivity(intent);
            }
        });
    }
}
