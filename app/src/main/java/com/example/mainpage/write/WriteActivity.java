package com.example.mainpage.write;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainpage.R;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class WriteActivity extends AppCompatActivity {
    public static int OPEN_IMAGE_REQUEST_CODE = 49018;
    private RelativeLayout mBtnAddCamera;
    private RecyclerView rvContentView;
    private PhotoAdapter photoAdapter;
    private TextView tvPhotoLabel;

    EditText ETT, ETP, ETC;
    Button ETB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write);

        mBtnAddCamera = findViewById(R.id.view_camera);
        rvContentView = findViewById(R.id.rv_content);
        tvPhotoLabel = findViewById(R.id.tv_photo_number);

        ETT = (EditText) findViewById(R.id.et_title);
        ETP = (EditText) findViewById(R.id.et_price);
        ETC = (EditText) findViewById(R.id.et_content);

        ETB = (Button)findViewById(R.id.et_button);

        initRecyclerView();

        mBtnAddCamera.setOnClickListener(view -> getGallaryIntent(WriteActivity.this));

        ETB.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                try {
                    String ett = ETT.getText().toString().trim();
                    String etp = ETP.getText().toString().trim();
                    String etc = ETC.getText().toString().trim();

                    JSONObject jsonob = new JSONObject();

                    jsonob.accumulate("title", ett);
                    jsonob.accumulate("price", etp);
                    jsonob.accumulate("detail", etc);

                    String url = "http://192.168.0.12:3000/";

                    new JSONTask().execute(url,jsonob.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public class JSONTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... urls) {
            //JSONObject??? ????????? key value ???????????? ?????? ???????????????.

            HttpURLConnection nect = null;
            BufferedReader rea = null;

            try {
                URL url = new URL(urls[0]);
                nect = (HttpURLConnection) url.openConnection();

                nect.setRequestMethod("POST");//POST???????????? ??????
                nect.setRequestProperty("Cache-Control", "no-cache");//?????? ??????
                nect.setRequestProperty("Content-Type", "application/json");//application JSON ???????????? ??????

                nect.setRequestProperty("Accept", "text/html");//????????? response ???????????? html??? ??????
                nect.setDoOutput(true);//Outstream?????? post ???????????? ?????????????????? ??????
                nect.setDoInput(true);//Inputstream?????? ??????????????? ????????? ???????????? ??????
                nect.connect();

                //????????? ?????????????????? ????????? ??????
                OutputStream out = nect.getOutputStream();
                out.write(urls[1].getBytes("utf-8"));
                out.flush();
                out.close();//????????? ?????????

                //????????? ?????? ???????????? ??????
                InputStream stream = nect.getInputStream();

                rea = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();

                String line = "";
                while ((line = rea.readLine()) != null) {
                    buffer.append(line);
                }
                return buffer.toString();//????????? ?????? ?????? ?????? ???????????? ?????? OK!!??? ???????????????

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (nect != null) {
                    nect.disconnect();
                }
                try {
                    if (rea != null) {
                        rea.close();//????????? ?????????
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d(result,"nono");
            if(result.equals("Create Success")){
                finish();
                Intent intent = new Intent(getApplicationContext(),WriteActivity.class);
                startActivity(intent);
            }else{
                Toast.makeText(WriteActivity.this, "Used ID", Toast.LENGTH_SHORT);
            }
        }
    }



    private void initRecyclerView() {

        photoAdapter = new PhotoAdapter(this) {
            @Override
            public void itemCallback(int position) {
                photoAdapter.removeItem(position);
                updatePhotoIndexLabel();
            }
        };
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        rvContentView.setLayoutManager(linearLayoutManager);
        rvContentView.setAdapter(photoAdapter);
    }

    private void getGallaryIntent(Activity activity) {
        Dexter.withActivity(activity).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (report.areAllPermissionsGranted()) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent, OPEN_IMAGE_REQUEST_CODE);
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

            }
        }).check();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_IMAGE_REQUEST_CODE) {
            if (data == null) {
                return;
            }

            Uri uri = data.getData();
            photoAdapter.addItem(String.valueOf(uri));
            updatePhotoIndexLabel();
            return;
        }
    }

    private void updatePhotoIndexLabel() {
        if (photoAdapter.getItemList() == null) {
            tvPhotoLabel.setText(String.format("%s/%s",0,10));
        } else {
            int registerPhotoAmount = photoAdapter.getItemList().size();
            tvPhotoLabel.setText(String.format("%s/%s",registerPhotoAmount,10));
        }
    }
}
