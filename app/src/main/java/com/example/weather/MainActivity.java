package com.example.weather;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private GpsTracker gpsTracker;
    private ImageView wea_image;
    private TextView wet, location,temp, rainy, rain_cate;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wet = findViewById(R.id.wea_wet);
        temp = findViewById(R.id.wea_location_temp);
        location = findViewById(R.id.wea_location);
        rainy =findViewById(R.id.wea_rainy);
        //rain_cate = findViewById(R.id.wea_rain_cate);

        wea_image = findViewById(R.id.image_Weather);

        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        }else {
            checkRunTimePermission();
        }
        gpsTracker = new GpsTracker(MainActivity.this);

        double latitude = gpsTracker.getLatitude();
        double longitude = gpsTracker.getLongitude();

        String address = getCurrentAddress(latitude, longitude);
        location.setText(address);

        //Toast.makeText(Weather.this, "현재위치 \n위도 " + latitude + "\n경도 " + longitude, Toast.LENGTH_LONG).show();

        JSON_Task();


        Button button = findViewById(R.id.wea_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSON_Task();
            }
        });
    }


    public void JSON_Task() {

        double latitude = gpsTracker.getLatitude();
        double longitude = gpsTracker.getLongitude();

        int nx = (int) latitude;
        int ny = (int) longitude;


        SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat( "yyyyMMdd", Locale.KOREA );
        SimpleDateFormat mSimpleDateFormat2 = new SimpleDateFormat( "HH", Locale.KOREA );
        String date = mSimpleDateFormat.format (new Date());
        String hour = mSimpleDateFormat2.format (new Date());

        if (Integer.parseInt(hour) <3) {
            Calendar calendar=new GregorianCalendar();
            calendar.add(Calendar.DATE,-1);
            date = mSimpleDateFormat.format (calendar.getTime());
            hour = "2300";

        } else {
            switch (Integer.parseInt(hour)) {
                case 3: case 4: case 5:
                    hour = "0200";
                    break;
                case 6:case 7:case 8:
                    hour="0500";
                    break;
                case 9:case 10:case 11:
                    hour="0800";
                    break;
                case 12: case 13:case 14:
                    hour="1100";
                    break;
                case 15:case 16:case 17:
                    hour="1400";
                    break;
                case 18:case 19: case 20:
                    hour="1700";
                    break;
                case 21:case 22:case 23:
                    hour="2000";
                    break;
            }

        }


        String url = "http://newsky2.kma.go.kr/service/SecndSrtpdFrcstInfoService2/ForecastSpaceData?serviceKey=5PIc5zCh%2Fc%2FrUZIXbE8dYaaJpc4TF9y9qp%2F374VeiKsRGiN7rlwTI%2FPt8HY1o1gW4Oh9nmzCC4mV%2FjsU9Q5WmQ%3D%3D" +
                "&base_date=" + date +
                "&base_time=" + hour +
                "&nx=" + nx +
                "&ny=" + ny +
                "&numOfRows=10" +
                "&pageNo=1" +
                "&_type=json";
        //POP -> 강수확률, PTY->강수형태, REH ->습도,  SKY ->하늘상태, TMN->아침 최저기온
        //TMX 낮 최고기온  하늘상태(SKY) 코드 : 맑음(1), 구름많음(3), 흐림(4)
        //강수형태(PTY) 코드 : 없음(0), 비(1), 비/눈(2), 눈(3), 소나기(4)

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONObject json0 = (JSONObject) response.getJSONObject("response");
                            JSONObject json1 = (JSONObject) json0.getJSONObject("body");
                            JSONObject json2 = (JSONObject) json1.getJSONObject("items");
                            JSONArray jsonArray = (JSONArray) json2.getJSONArray("item");

                            String pop, pty, r06, reh, s06, sky, t3h, tmn, tmx, uuu, vec, vvv, wav, wsd;

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject item = jsonArray.getJSONObject(i);

                                String category = item.getString("category");

                                switch (category) {
                                    case "POP":
                                        pop = (item.get("fcstValue")).toString();
                                        System.out.println("pop 강수확률:" + pop);
                                        rainy.setText(pop + "%");
                                        break;
                                    case "PTY":
                                        pty = (item.get("fcstValue")).toString();
                                        if (pty == "0") {
                                            rain_cate.setText("비가 안와요!");
                                        } else if ( pty == "1") {
                                            rain_cate.setText("비가 오네요!");
                                        } else if ( pty == "2") {
                                            rain_cate.setText("비와 눈이 같이 와요!");
                                        } else if ( pty == "3") {
                                            rain_cate.setText("눈이 오네요!");
                                        } else if ( pty == "4") {
                                            rain_cate.setText("소나기가 와요!");
                                        }
                                        System.out.println("pty 강수형태: " + pty);
                                        break;
                                    case "R06":
                                        r06 = (item.get("fcstValue")).toString();
                                        System.out.println("r06 강수량:" + r06);
                                        break;
                                    case "REH":
                                        reh = (item.get("fcstValue")).toString();
                                        System.out.println("reh 습도:" + reh);
                                        wet.setText(reh +"%");
                                        break;
                                    case "S06":
                                        s06 = (item.get("fcstValue")).toString();
                                        System.out.println("s06 신적설:" + s06);
                                        break;
                                    case "SKY":
                                        sky = (item.get("fcstValue")).toString();
                                        if (sky == "1") {
                                            wea_image.setImageResource(R.drawable.contrast);
                                        } else if ( sky == "3") {
                                            //wea_image.setImageResource(R.drawable.cloudy);
                                        } else if ( sky == "4") {
                                            //wea_image.setImageResource(R.drawable.cloud);
                                        }
                                        System.out.println("sky 하늘상태:" + sky);
                                        break;
                                    case "T3H":
                                        t3h = (item.get("fcstValue")).toString();
                                        System.out.println("t3h 기온:" + t3h);
                                        temp.setText(t3h + "도");
                                        break;
                                    case "TMN":
                                        tmn = (item.get("fcstValue")).toString();
                                        System.out.println("tmn 아침 최저기온:" + tmn);
                                        break;
                                    case "TMX":
                                        tmx = (item.get("fcstValue")).toString();
                                        System.out.println("tmx 낮 최고기온:" + tmx);
                                        break;
                                    case "UUU":
                                        uuu = (item.get("fcstValue")).toString();
                                        System.out.println("uuu 풍속(동서성분):" + uuu);
                                        break;
                                    case "VEC":
                                        vec = (item.get("fcstValue")).toString();
                                        System.out.println("vec 풍향:" + vec);
                                        break;
                                    case "VVV":
                                        vvv = (item.get("fcstValue")).toString();
                                        System.out.println("vvv 풍속(남북성분):" + vvv);
                                        break;
                                    case "WAV":
                                        wav = (item.get("fcstValue")).toString();
                                        System.out.println("wav 파고:" + wav);
                                        break;
                                    case "WSD":
                                        wsd = (item.get("fcstValue")).toString();
                                        System.out.println("wsd 풍속:" + wsd);
                                        break;

                                }

                                String fcstValue = String.valueOf(item.getInt("fcstValue"));

                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);

    }



    /*
     * ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.
     */


    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면

            boolean check_result = true;


            // 모든 퍼미션을 허용했는지 체크합니다.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if (check_result) {

                //위치 값을 가져올 수 있음
                ;
            } else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();


                } else {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();

                }
            }

        }
    }

    void checkRunTimePermission() {

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)


            // 3.  위치 값을 가져올 수 있음


        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }


    public String getCurrentAddress(double latitude, double longitude) {

        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {

            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";

        }


        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString() + "\n";

    }


    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

}
