package de.kasoki.trierbustimetracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import de.kasoki.swtrealtime.BusStop;
import de.kasoki.swtrealtime.BusTime;
import de.kasoki.trierbustimetracker.utils.Helper;

public class BusTimeActivity extends Activity {

	private List<BusTime> busTimesList;
	private List<Map<String, String>> listViewContent;

	private ListView busStopListView;
	private SimpleAdapter listAdapter;

	private String busTimeCode;

	private volatile boolean reloadActive;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bustimes);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		// get selected bus code
		Intent intent = getIntent();
		busTimeCode = intent.getStringExtra("BUS_TIME_CODE");

		// init stuff
		busStopListView = (ListView) this.findViewById(R.id.busStopListView);
		listViewContent = new ArrayList<Map<String, String>>();

		listAdapter = new SimpleAdapter(this, listViewContent,
				android.R.layout.simple_list_item_2, new String[] {
						"FIRST_LINE", "SECOND_LINE" }, new int[] {
						android.R.id.text1, android.R.id.text2 });

		busStopListView.setAdapter(listAdapter);

		// set thread policy to permit all
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// set title to bus stop
		this.setTitle(BusStop.getBusStopByStopCode(busTimeCode).getName());
		
		// get information
		reload();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bustimes_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		// reload action pressed
		case R.id.action_reload:
			reload();
			return true;
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// reload the bus times and set them to the busStopListView
	private synchronized void reload() {
		final String busTimeCode = this.busTimeCode;
		final BusTimeActivity activity = this;

		if (!this.reloadActive) {
			// maybe is should refactor the code below
			Handler handler = new Handler();

			final Runnable r = new Runnable() {
				@Override
				public void run() {
					if (Helper.isNetworkAvailable(activity)) {
						// delete the content of the old list, we don't them
						// anymore ;)
						listViewContent.clear();

						// disable reload button
						activity.setReloadActive(true);

						// retrieve stuff from the SWT servers
						busTimesList = BusTime.fromStopCode(busTimeCode);

						Collections.sort(busTimesList);

						for (BusTime b : busTimesList) {
							Log.d("BUSTIME RECIEVED", b.toString());

							final Map<String, String> data = new HashMap<String, String>(
									2);

							String delay = "";

							if (b.getDelay() != 0) {
								String operand = b.getDelay() < 0 ? "-" : "+";
								delay = String.format(" %s %d%s", operand,
										b.getDelay(), "m");
							}

							String arrivalTimeText = getResources().getString(
									R.string.bustime_arrival_text);

							data.put(
									"FIRST_LINE",
									String.format("(%d) %s", b.getNumber(),
											b.getDestination()));
							data.put("SECOND_LINE", String.format("%s: %s%s",
									arrivalTimeText,
									b.getArrivalTimeAsString(), delay));

							activity.runOnUiThread(new Runnable() {

								@Override
								public void run() {
									listViewContent.add(data);
								}

							});

						}

						// when the list is empty show the user that there are
						// no buses atm
						if (listViewContent.isEmpty()) {
							final Map<String, String> data = new HashMap<String, String>(
									2);
							data.put(
									"FIRST_LINE",
									getResources().getString(
											R.string.bustime_no_bus));
							data.put("SECOND_LINE", "");

							listViewContent.add(data);
						}

						// enable reload button
						activity.setReloadActive(false);

						listAdapter.notifyDataSetChanged();
					} else {
						// No connection
						Log.d("NETWORK", "NO NETWORK CONNECTION");

						String noNetworkConnectionText = getResources()
								.getString(R.string.no_network_connection_text);

						Toast toast = Toast.makeText(
								activity.getApplicationContext(),
								noNetworkConnectionText, Toast.LENGTH_SHORT);
						toast.show();
					}
				}
			};

			handler.post(r);
		}
	}

	public void setReloadActive(boolean bool) {
		this.reloadActive = bool;
	}

	@Override
	public void onResume() {
		reload();

		super.onResume();
	}

}
