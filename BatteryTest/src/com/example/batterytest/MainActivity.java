package com.example.batterytest;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    BatteryListener blBatteryListener = new BatteryListener();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        blBatteryListener.StartBatteryListener();
        
        final Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // --------------------------------------------------------------
                Context context = getApplicationContext();
                String s = "Battery life: " + String.valueOf(blBatteryListener.GetBatteryLife()) + "\n";
                s = s + "Battery consumption: " + String.valueOf(blBatteryListener.GetBatteryComsumption()) + "\n";
                CharSequence text = s;
                int duration = Toast.LENGTH_SHORT;
                
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                // --------------------------------------------------------------
            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
