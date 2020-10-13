package com.newlogic.mlkit.demo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newlogic.mlkit.R;
import com.newlogic.mlkitlib.newlogic.MLKitActivity;
import com.newlogic.mlkitlib.newlogic.config.Config;


public class MainActivity extends AppCompatActivity {

    private final int OP_MLKIT = 1001;

    private static final String TAG = "Newlogic-MLkit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == OP_MLKIT) {
            Log.d(TAG, "Plugin post ML Activity resultCode " + resultCode);

            if (resultCode == Activity.RESULT_OK) {
                String returnedResult = intent.getStringExtra(MLKitActivity.MLKIT_RESULT);
                JsonObject jsonObject = null;
                if (returnedResult != null) {
                    jsonObject = JsonParser.parseString(returnedResult).getAsJsonObject();
                    if (jsonObject.get("imagePath") != null) {
                        String path = jsonObject.get("imagePath").getAsString();
                        Bitmap myBitmap = BitmapFactory.decodeFile(path);
                        ImageView image = findViewById(R.id.imageView);
                        image.setImageBitmap(myBitmap);
                    }
                    TextView text = findViewById(R.id.editTextTextMultiLine);
                    text.setText(returnedResult);
                }
            }
        }
    }

    public void startScanningActivity(@NonNull View view) {
        Intent intent = new Intent(this, MLKitActivity.class);
        Config config = getSampleConfig();
        intent.putExtra(MLKitActivity.MLKIT_CONFIG, config);
        startActivityForResult(intent, OP_MLKIT);
    }

    public void startPDF417ScanningActivity(@NonNull View view) {
        Intent intent = new Intent(this, MLKitActivity.class);
        intent.putExtra("mode", "pdf417");
        startActivityForResult(intent, OP_MLKIT);
    }

    public void startQRCodeScanningActivity(@NonNull View view) {
        /*Intent intent = new Intent(this, MLKitActivity.class);
        intent.putExtra("mode", "pdf417");
        startActivityForResult(intent, OP_MLKIT);*/
    }

    public void startBarcodeScanningActivity(@NonNull View view) {
        Intent intent = new Intent(this, MLKitActivity.class);
        intent.putExtra("mode", "barcode");
        startActivityForResult(intent, OP_MLKIT);
    }

    private Config getSampleConfig() {
        return new Config(
                "NOTO_SANS_ARABIC",
                "ARABIC", "الكرامة والحقوق. وقد وهبوا عقلاً",
                "mrz",
                true);
    }
}
