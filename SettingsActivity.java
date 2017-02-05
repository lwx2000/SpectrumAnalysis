package com.github.lwx2000.spectrumanalysis;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by LX on 2017/1/25.
 */

public class SettingsActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        // 文本框取MainActivity传过来的初值
        Intent intent = getIntent();
        int x = intent.getIntExtra("SamplingIntervalX", 20);
        int y = intent.getIntExtra("SamplingIntervalY", 20);
        boolean checked = intent.getBooleanExtra("ShowOnlyBluewave", false);
        ((TextView)findViewById(R.id.sampling_interval_x)).setText(Integer.toString(x));
        ((TextView)findViewById(R.id.sampling_interval_y)).setText(Integer.toString(y));
        ((CheckBox)findViewById(R.id.showonly_bluewave)).setChecked(checked);

        Button ok = (Button)findViewById(R.id.ok);
        ok.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                String x = ((TextView)findViewById(R.id.sampling_interval_x)).getText().toString();
                String y = ((TextView)findViewById(R.id.sampling_interval_y)).getText().toString();
                boolean checked = ((CheckBox)findViewById(R.id.showonly_bluewave)).isChecked();

                // 文本框的新值传回MainActivity
                Intent intent = new Intent();
                intent.putExtra("SamplingIntervalX", x.isEmpty() ? 20 : Integer.parseInt(x));
                intent.putExtra("SamplingIntervalY", y.isEmpty() ? 20 : Integer.parseInt(y));
                intent.putExtra("ShowOnlyBluewave", checked);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
}
