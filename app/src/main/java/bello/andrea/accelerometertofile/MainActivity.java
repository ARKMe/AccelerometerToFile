package bello.andrea.accelerometertofile;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class MainActivity extends Activity implements SensorEventListener {

    private final String FILE_NAME = "accelerometer_data.txt";

    private SensorManager sensorManager;

    private Vector3D gravity;

    BufferedWriter bufferedWriter;

    boolean firstValue;

    AccelerometerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        adapter = new AccelerometerAdapter();
        ((ListView)findViewById(R.id.listview)).setAdapter(adapter);

        findViewById(R.id.startListening).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWriter(FILE_NAME);
                registerListener();
            }
        });

        findViewById(R.id.stopListening).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unregisterListener();
                closeWriter();
                try {
                    FileInputStream fileInputStream = new FileInputStream(getFilesDir() + FILE_NAME);
                    InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, Charset.forName("UTF-8"));
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    JsonReader reader = new JsonReader(bufferedReader);
                    try {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            Vector3D vector3D = readVector(reader);
                            adapter.addValue(vector3D);
                        }
                        reader.endArray();
                    } finally {
                        reader.close();
                    }
                    adapter.notifyDataSetChanged();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Vector3D readVector(JsonReader reader) throws IOException {
        Vector3D vector3D = new Vector3D();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            int readInt = reader.nextInt();
            if (name.equals("x")) {
                vector3D.setX(readInt);
            } else if (name.equals("y")) {
                vector3D.setY(readInt);
            } else if (name.equals("z")) {
                vector3D.setZ(readInt);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return vector3D;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometerChanged(event);
        }
    }

    private void accelerometerChanged(SensorEvent event) {
        Vector3D accelerationVector = magicGoogleFormulaToIgnoreGravity(event);
        try {
            String accelerationString = accelerationVector.toJSONObject().toString();

            if(!firstValue)
                bufferedWriter.write(",");
            else
                firstValue = false;

            bufferedWriter.write(accelerationString);
            Log.i("ACCELEROMETER DATA", accelerationString);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Vector3D magicGoogleFormulaToIgnoreGravity(SensorEvent event){
        if(gravity == null){
            gravity = new Vector3D();
        }

        gravity.applyLowPassFilter(event.values[0], event.values[1], event.values[2]);

        Vector3D vector3D = new Vector3D(event.values[0], event.values[1], event.values[2]);
        vector3D.subtraction(gravity);

        return vector3D;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterListener();
        closeWriter();
    }

    private void closeWriter(){
        if(bufferedWriter != null) {
            try {
                bufferedWriter.write("]");
                bufferedWriter.flush();
                bufferedWriter.close();
                bufferedWriter = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void openWriter(String fileName){
        try {
            firstValue = true;
            adapter.reset();

            FileOutputStream fileOutputStream = new FileOutputStream(getFilesDir() + fileName);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, Charset.forName("UTF-8"));
            bufferedWriter = new BufferedWriter(outputStreamWriter);

            bufferedWriter.write("[");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void registerListener(){
        sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL
        );
    }

    private void unregisterListener(){
        sensorManager.unregisterListener(this);
    }

    private class AccelerometerAdapter extends BaseAdapter{

        ArrayList<Vector3D> values;

        public AccelerometerAdapter() {
            values = new ArrayList<>();
        }

        public void reset(){
            notifyDataSetInvalidated();
            values = new ArrayList<>();
        }

        public void addValue(Vector3D vector3D){
            values.add(vector3D);
        }

        @Override
        public int getCount() {
            return values.size();
        }

        @Override
        public Object getItem(int position) {
            return values.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null){
                convertView =  getLayoutInflater().inflate(R.layout.vector_layout, null);
            }
            Vector3D vector3D = values.get(position);
            TextView xTextView = (TextView)convertView.findViewById(R.id.valueX);
            TextView yTextView = (TextView)convertView.findViewById(R.id.valueY);
            TextView zTextView = (TextView)convertView.findViewById(R.id.valueZ);

            initTextView(xTextView, (int)vector3D.getX());
            initTextView(yTextView, (int)vector3D.getY());
            initTextView(zTextView, (int)vector3D.getZ());

            return convertView;
        }

        private void initTextView(TextView textView, int value){
            textView.setText(""+value);
            if(value > 0){
                textView.setBackgroundColor(ContextCompat.getColor(textView.getContext(), R.color.green));
            } else if(value < 0){
                textView.setBackgroundColor(ContextCompat.getColor(textView.getContext(), R.color.red));
            } else {
                textView.setBackgroundColor(ContextCompat.getColor(textView.getContext(), android.R.color.white));
            }
        }
    }
}
