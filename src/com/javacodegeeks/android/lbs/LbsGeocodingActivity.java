package com.javacodegeeks.android.lbs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;

import android.content.Context;

import android.location.Location;

import android.location.LocationListener;

import android.location.LocationManager;


import android.util.Log;
import android.view.View;

import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.TextView;

import android.widget.Toast;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class LbsGeocodingActivity extends Activity implements
		SensorEventListener {

	private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1; // in
																		// Meters
	private static final long MINIMUM_TIME_BETWEEN_UPDATES = 1000; // in
																	// Milliseconds
	Socket socket = null;
	private String serverIpAddress = "192.168.1.104";
	private int serverPort = 8888;
	private boolean connected = false;
	DataOutputStream dataOutputStream = null;

	DataInputStream dataInputStream = null;
	public static BufferedWriter out;
	public static BufferedWriter outfile;
	File Root;
	File LogFile;
	File LogFileAccel;
	FileWriter LogWriter;
	SensorManager sm;
	Thread cThread;
	String outToServer;
	File base = Environment.getExternalStorageDirectory();
	File myLog;
	FileWriter myLogWriter;
	SensorManager sensorManager = null;
	protected LocationListener locationListener;
	protected LocationManager locationManager;
	protected Button retrieveLocationButton;
	FileWriter LogWriterAccel;
	TextView xyzCoordinate;
	TextView gps;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		Button buttonStart = (Button) findViewById(R.id.buttonStart);
		buttonStart.setOnClickListener(startListener); // Register the onClick
														// listener with the
														// implementation above

		Button buttonSend = (Button) findViewById(R.id.buttonSend);
		buttonSend.setOnClickListener(stopListener);

		xyzCoordinate = (TextView) findViewById(R.id.xyz);

		gps = (TextView) findViewById(R.id.gpsreading);

		LogFileAccel = new File(base, "MyAccelerometerLog2.txt");

		try {
			LogWriterAccel = new FileWriter(LogFileAccel, true);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private OnClickListener startListener = new OnClickListener() {
		public void onClick(View v) {
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, MINIMUM_TIME_BETWEEN_UPDATES,
					MINIMUM_DISTANCE_CHANGE_FOR_UPDATES,
					new MyLocationListener());

			showCurrentLocation();

		}
	};

	private OnClickListener stopListener = new OnClickListener() {
		public void onClick(View v) {

			try {
				out.close();
				outfile.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			cThread = new Thread(new ClientThread());
			cThread.start();

		}
	};

	@Override
	protected void onStop() {
		super.onStop();
		sensorManager.unregisterListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
		sensorManager.unregisterListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION));
	}

	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				sensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				sensorManager.SENSOR_DELAY_GAME);
	}

	// added today

	public void onSensorChanged(SensorEvent event) {
		synchronized (this) {
			switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				String AccelerometerValue = String.format(" " + event.values[0]
						+ " " + event.values[1] + " " + event.values[2]);
				xyzCoordinate.setText(AccelerometerValue);
				outfile = new BufferedWriter(LogWriterAccel);

				Date date = new Date();
				Format formatter = new SimpleDateFormat("hh:mm:ss");
				String s = formatter.format(date);

				try {
					outfile.write(s + AccelerometerValue + "#");

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				xyzCoordinate.setText(AccelerometerValue);
				break;
			case Sensor.TYPE_ORIENTATION:
				break;
			}
		}
	}

	// added today
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private void createFile(String string) {
		// TODO Auto-generated method stub

	}

	private boolean fileExists(String string) {
		// TODO Auto-generated method stub
		return false;
	}

	public void onAccuracyChanged(int sensor, int accuracy) {
		// Log.d("Sensor","onAccuracyChanged: " + sensor + ", accuracy: " +
		// accuracy);
	}

	protected void showCurrentLocation() {

		Location location = locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		if (location == null) {
			System.out.println("Location is null ");
		}
		boolean args;
		if (location != null) {
			String message = String.format("%1$s %2$s,",
					location.getLongitude(), location.getLatitude());
			Log.v("themsg", message);
			// To create the File externally to store data in it
			Root = Environment.getExternalStorageDirectory();

			if (Root.canWrite()) {
				LogFile = new File(Root, "MyLog3.txt");
				if (fileExists(" MyLog3.txt")) {
					createFile("MyLog3.txt");
					Log.i("File", "overwritten");
				}
				Log.i("hi", "Created FILE " + LogFile);
				try {
					LogWriter = new FileWriter(LogFile, true);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				out = new BufferedWriter(LogWriter);
				Date date = new Date();
				Format formatter = new SimpleDateFormat("hh:mm:ss");
				String s = formatter.format(date);
				try {
					out.write(s + " " + message);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			gps.setText(message);
		}
	}

	public class ClientThread implements Runnable {
		public void run() {
			try {
				InetAddress serverAddr = InetAddress.getByName(serverIpAddress);
				Socket socket = new Socket(serverAddr, serverPort);
				connected = true;
				try {
					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					try {
						File f = new File(
								Environment.getExternalStorageDirectory()
										+ "/MyLog3.txt");
						File f1 = new File(
								Environment.getExternalStorageDirectory()
										+ "/MyAccelerometerLog2.txt");

						FileInputStream fileIS = new FileInputStream(f);
						FileInputStream fileIS1 = new FileInputStream(f1);

						BufferedReader buf = new BufferedReader(
								new InputStreamReader(fileIS));
						BufferedReader buf1 = new BufferedReader(
								new InputStreamReader(fileIS1));
						out.println(buf.readLine());
						out.println(buf1.readLine());
						out.println("Hello server");

						buf.close();
						buf1.close();

					} catch (FileNotFoundException e) {
						Log.d("Hi ", "No File Found");
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}

				} catch (Exception e) {
					Log.e("ClientActivity", "S: Error", e);
				}

				socket.close();
				cThread.stop();
				locationManager.removeUpdates(locationListener);
			} catch (Exception e) {
				Log.e("ClientActivity", "C: Error   :(" + e.toString());
				connected = false;
			}
		}
	}

	private class MyLocationListener implements LocationListener {
		public void onLocationChanged(Location location) {
			String message = String.format(" %1$s %2$s,",
					location.getLongitude(), location.getLatitude());
			Log.v("themsg", message);
			Date date = new Date();
			Format formatter = new SimpleDateFormat("hh:mm:ss");
			String s = formatter.format(date);
			try {
				out.write(s + " " + message);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			gps.setText(message);
		}

		public void onStatusChanged(String s, int i, Bundle b) {
			Toast.makeText(LbsGeocodingActivity.this,
					"Provider status changed",
					Toast.LENGTH_LONG).show();
		}

		public void onProviderDisabled(String s) {
			Toast.makeText(LbsGeocodingActivity.this,
			"Provider disabled by the user. GPS turned off",
			Toast.LENGTH_LONG).show();
		}

		public void onProviderEnabled(String s) {

			Toast.makeText(LbsGeocodingActivity.this,
			"Provider enabled by the user. GPS turned on",
			Toast.LENGTH_LONG).show();
		}
	}
	@Override
	protected void onDestroy() {
		Log.i("hi", "My Gps is shut down");
		super.onDestroy();
	}
}