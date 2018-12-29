package gps_application.evismar.tutorials.com.servicesample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Home extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button devicesMapButton = findViewById(R.id.buttonDevicesMap);
        Button devicesListButton = findViewById(R.id.buttonDevicesList);

        devicesMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent devicesMapPage = new Intent(Home.this, MapsActivity.class);
                startActivity(devicesMapPage);
            }
        });

        devicesListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent devicesListPage = new Intent(Home.this, ListActivity.class);
                startActivity(devicesListPage);
            }
        });
    }
}
