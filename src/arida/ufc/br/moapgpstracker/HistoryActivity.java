package arida.ufc.br.moapgpstracker;

import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.moap.chart.SpeedOverTimeChart;
import org.moap.overlays.GoogleMapsOverlay;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.Toast;
import arida.ufc.br.moap.core.beans.LatLonPoint;
import arida.ufc.br.moap.core.beans.MovingObject;
import arida.ufc.br.moap.core.beans.Trajectory;
import arida.ufc.br.moap.datamodelapi.imp.TrajectoryModelImpl;

public class HistoryActivity extends MapActivity {

	private final int GMC_ZONE = -3;

	public enum ResponseType {
		INTERNET_ISSUE, OK, ERROR
	};

	// List of points provided by the remote Server in response to a request
	private JSONArray list_of_points;
	private DateTime start;
	private DateTime end;
	
	// Format
	private final DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-M-d");
	private final DateTimeFormatter fmt_receive = DateTimeFormat
			.forPattern("yyyy-M-d H:m:s");
	
	// TAG
	private final String TAG = "HistoryActivity";
	
	 /** This integer will uniquely define the dialog to be used for displaying date picker.*/
    private static final int DATE_START_DIALOG_ID = 0;
    private static final int DATE_END_DIALOG_ID = 1;
    /** Callback received when the user "picks" a <span id="IL_AD5" class="IL_AD">date in</span> the dialog */
    private DatePickerDialog.OnDateSetListener startListener =
            new DatePickerDialog.OnDateSetListener() {

				public void onDateSet(DatePicker view, int year,
						int monthOfYear, int dayOfMonth) {
					try{
						start = new DateTime(year, monthOfYear+1, dayOfMonth, 0, 0);
						Button start_button = (Button)findViewById(R.id.start_history_button);
						start_button.setText(start.toString(fmt));
					}
					catch(Exception ex){
						Log.e(TAG,"Erro", ex);
					}
					
					
				}
            };
	private DatePickerDialog.OnDateSetListener endListener =
	        new DatePickerDialog.OnDateSetListener() {
	
				public void onDateSet(DatePicker view, int year,
						int monthOfYear, int dayOfMonth) {
					try{
					end = new DateTime(year, monthOfYear+1, dayOfMonth, 0, 0);
					Button end_button = (Button)findViewById(R.id.end_history_button);
					end_button.setText(end.toString(fmt));
					}
					catch(Exception ex){
						Log.e(TAG,"Erro", ex);
					}
					
				}
	        };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_history);

		Button update_button = (Button) findViewById(R.id.update_history_button);
		update_button.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				String username = PreferenceManager
						.getDefaultSharedPreferences(getBaseContext())
						.getString("server_login_key", "");
				String token = getSharedPreferences(GpsMainActivity.MOAP,
						Context.MODE_PRIVATE).getString(
						"user.gpstrackerserver.token", "");
				if (username.equalsIgnoreCase("") || token.equalsIgnoreCase("")) {
					Toast.makeText(HistoryActivity.this,
							"You should log in to the server",
							Toast.LENGTH_LONG).show();
				} else {
					if(start!=null && end != null)
						getHistory(token, username, start, end);
				}

			}
		});
		
		Button start_button = (Button)findViewById(R.id.start_history_button);
		start_button.setOnClickListener(new OnClickListener() {
			
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				showDialog(DATE_START_DIALOG_ID);
				
			}
		});
		start_button.setText("Start");
		
		Button end_button = (Button)findViewById(R.id.end_history_button);
		end_button.setOnClickListener(new OnClickListener() {
			
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				showDialog(DATE_END_DIALOG_ID);
				
			}
		});
		end_button.setText("End");


//		if (username.equalsIgnoreCase("") || token.equalsIgnoreCase("")) {
//			Toast.makeText(HistoryActivity.this,
//					"You should log in to the server", Toast.LENGTH_LONG);
//		} else {
//			getHistory(token, username, today, today.plusDays(2));
//		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_history, menu);
		return true;
	}

	@SuppressWarnings("unchecked")
	private void onChartMenu() {
		TrajectoryModelImpl<LatLonPoint, DateTime> model = getTrajectoryHistory();

		if (model != null) {
			Log.w(TAG, "onCharMenu");

			System.out.println("Trajs: " + model.getTrajectoryCount());

			SpeedOverTimeChart analysis = new SpeedOverTimeChart(model);

			XYMultipleSeriesDataset dataset = analysis.createDataset();
			XYMultipleSeriesRenderer renderer = analysis.createRenderer();

			if (dataset != null && renderer != null) {
				try {
					Intent intent = ChartFactory.getTimeChartIntent(this,
							dataset, renderer, "HH:mm");
					// Intent intent = ChartFactory.getLineChartIntent(this,
					// dataset,renderer);

					startActivity(intent);
				} catch (Exception ex) {
					Utilities.LogError("ERROR CHART", ex);
				}

			} else {
				Utilities.LogDebug(String.format("Dataset - %s\nRenderer - ",
						dataset.toString(), renderer.toString()));
			}
		} else {
			Log.w(TAG, "onCharMenu - model is null");
		}
	}

	public boolean onOptionsItemSelected(MenuItem item) {

		int idx = item.getItemId();
		switch (idx) {
		case R.id.history_chart:
			onChartMenu();
			break;
		default:
			break;
		}

		return true;
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	private void getHistory(final String token, final String username,
			DateTime begin, DateTime end) {

		new AsyncTask<DateTime, Void, ResponseType>() {

			@Override
			protected ResponseType doInBackground(DateTime... params) {
				// TODO Auto-generated method stub
				// http://sw4.us/ufc/default.php?PHPSESSID=ak71dgl77rlcot4ig73hktshr6&q=2&id=1&start=0&end=2013-12-06+03:16:18
				String url = "http://sw4.us/ufc/default.php";

				// Parameters
				url += "?PHPSESSID=" + token + "&q=2" + "&id=" + username
						+ "&start=" + params[0].toString(fmt) + "&end="
						+ params[1].toString(fmt);

				Log.d(TAG, url);

				HttpGet httpGet = new HttpGet(url);

				// BasicHttpParams p = new BasicHttpParams();
				// p.setParameter("q", Integer.toString(2));
				// p.setParameter("id", "1");
				// p.setParameter("start", params[0].toString(fmt));
				// p.setParameter("end", params[1].toString(fmt));
				// httpGet.setParams(p);

				Log.d("HistoryActivity", "URI: " + httpGet.getURI().toString());
				HttpClient client = new DefaultHttpClient();
				HttpResponse response = null;

				try {
					response = client.execute(httpGet);
				} catch (Exception e) {
					Log.w("HistoryActivity", "No internet connection", e);

					return ResponseType.INTERNET_ISSUE;
				}
				JSONObject jsonObject = null;
				try {
					jsonObject = new JSONObject(IOUtils.toString(response
							.getEntity().getContent()));
				} catch (Exception e) {
					Log.e("HistoryActivity", "Wrong JSON format", e);
					return ResponseType.ERROR;
				}

				try {

					if (jsonObject.has("meta")) {

						int code = jsonObject.getJSONObject("meta").getInt(
								"code");

						if (code == 200) {

							Log.d("HistoryActivity", "code " + code);

							list_of_points = jsonObject.getJSONObject("result")
									.getJSONArray("point");
							Log.i(TAG, "Number of points "+list_of_points.length());
						} else {

							Log.w("HistoryActivity",
									"Cannot connect to the server: " + code);
							return ResponseType.INTERNET_ISSUE;
						}
					}
				} catch (Exception e) {
					Log.e("HistoryActivity", "ERROR 2", e);
				}

				return ResponseType.OK;

			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				ProgressBar pb = (ProgressBar) findViewById(R.id.history_progress_bar);
				pb.setEnabled(true);
				pb.setVisibility(View.VISIBLE);
				pb.setIndeterminate(true);
				Log.d("HistoryActivity", "onPreExecute");

			}

			@Override
			protected void onPostExecute(ResponseType v) {

				ProgressBar pb = (ProgressBar) findViewById(R.id.history_progress_bar);
				pb.setEnabled(false);
				pb.setVisibility(View.INVISIBLE);
				pb.setIndeterminate(false);

				Log.d("HistoryActivity", "onPosExecute");

				// Check the result of the process
				switch (v) {
				case OK:
					Toast.makeText(HistoryActivity.this,
							"Loaded "+list_of_points.length()+" points",
							Toast.LENGTH_LONG).show();
					drawTrajectoryHistory();
					break;
				case INTERNET_ISSUE:
					Toast.makeText(getApplicationContext(),
							"Cannot connect to the Server", Toast.LENGTH_LONG)
							.show();
					break;
				case ERROR:
					break;
				default:
					break;
				}

			}

		}.execute(begin, end);

	}

	@SuppressWarnings({ "unchecked", "unused" })
	private TrajectoryModelImpl<LatLonPoint, DateTime> getTrajectoryHistory() {

		Log.d(TAG, "get trajectory history");

		TrajectoryModelImpl<LatLonPoint, DateTime> model = null;
		if (this.list_of_points != null) {
			int size = this.list_of_points.length();

			model = new TrajectoryModelImpl<LatLonPoint, DateTime>();

			MovingObject mo = model.factory().newMovingObject(
					Session.getUserName());

			Trajectory<LatLonPoint, DateTime> traj = model.factory()
					.newTrajectory(mo.getId() + "_0", mo);
			try {
				Log.d(TAG,
						" GetTrajectoryHistory - Loading trajectory model from JSON");
				for (int i = 0; i < size; i++) {
					JSONObject object = this.list_of_points.getJSONObject(i);
					double lat = object.getDouble("lat");
					double lon = object.getDouble("long");
					LatLonPoint point = new LatLonPoint(lon, lat);
					String date = object.getString("time");

					DateTime datetime;
					if (GMC_ZONE < 0)
						datetime = fmt_receive.parseDateTime(date).minusHours(
								GMC_ZONE * -1);
					else {
						datetime = fmt_receive.parseDateTime(date).plusHours(
								GMC_ZONE);
					}
					traj.addPoint(point, datetime);

				}
				model.addTrajectory(traj);
			} catch (Exception e) {
				Log.e(TAG, "Error to get trajectory history", e);
			}

		}
		return model;
	}

	/*
	 * Receive JSONArray with {time,lat,long} and draw on the map through
	 * GoogleMapsOverlay
	 */
	private void drawTrajectoryHistory() {

		Log.d(TAG, "Draw trajectory history");

		// Map settings
		MapView mapView = (MapView) findViewById(R.id.history_map_view);
		mapView.setBuiltInZoomControls(true);
		mapView.displayZoomControls(true);
		mapView.setClickable(true);

		// Overlay list
		List<Overlay> overlayList = mapView.getOverlays();
		overlayList.clear();

		// Google Maps overlay
		GoogleMapsOverlay overlay = new GoogleMapsOverlay(Color.BLUE);
		if (this.list_of_points != null) {

			int size = this.list_of_points.length();

			try {

				Log.d(TAG, "Loading trajectory model from JSON");

				for (int i = 0; i < size; i++) {
					JSONObject object = this.list_of_points.getJSONObject(i);
					double lat = object.getDouble("lat");
					double lon = object.getDouble("long");

					GeoPoint geoPoint = new GeoPoint(
							Utilities.convertCoordinates(lat),
							Utilities.convertCoordinates(lon));
					OverlayItem oi = new OverlayItem(geoPoint, "", "");

					overlay.addOverlayItem(oi);
				}

				overlayList.add(overlay);

				// Set zoom
				JSONObject object = this.list_of_points.getJSONObject(size - 1);
				double lat = object.getDouble("lat");
				double lon = object.getDouble("long");

				GeoPoint geoPoint = new GeoPoint(
						Utilities.convertCoordinates(lat),
						Utilities.convertCoordinates(lon));

				mapView.getController().setCenter(geoPoint);
			} catch (Exception e) {

			}

			// View the map
			mapView.invalidate();
		}

	}
	@Override
    protected Dialog onCreateDialog(int id) {
		DateTime now = new DateTime();
		DatePickerDialog pd;
        switch (id) {
        case DATE_START_DIALOG_ID:
        	pd = new DatePickerDialog(this, 
                    startListener,
                    now.getYear(), now.getMonthOfYear()-1, now.getDayOfMonth());
        	pd.setTitle("Start Date");
            return pd;
        case DATE_END_DIALOG_ID:
        	pd = new DatePickerDialog(this, 
                    endListener,
                    now.getYear(), now.getMonthOfYear()-1, now.getDayOfMonth());
        	pd.setTitle("End Date");
                return pd;
        }
        return null;
    }

}
