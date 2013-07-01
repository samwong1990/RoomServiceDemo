package hk.samwong.roomservice.android.roomservicedemo;

import hk.samwong.roomservice.android.library.apicalls.PutStatistics;
import hk.samwong.roomservice.android.library.apicalls.RoomQuery;
import hk.samwong.roomservice.android.library.constants.LogLevel;
import hk.samwong.roomservice.android.library.constants.LogTag;
import hk.samwong.roomservice.android.library.fingerprintCollection.WifiScannerPoller;
import hk.samwong.roomservice.android.roomservicedemo.helper.Console;
import hk.samwong.roomservice.commons.dataFormat.Report;
import hk.samwong.roomservice.commons.dataFormat.Response;
import hk.samwong.roomservice.commons.dataFormat.ResponseWithReports;
import hk.samwong.roomservice.commons.dataFormat.RoomStatistic;
import hk.samwong.roomservice.commons.dataFormat.WifiInformation;
import hk.samwong.roomservice.commons.parameterEnums.ReturnCode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * First step of investigating the possibility of sharing training data. Also a
 * way to taste my own API design.
 * 
 * @author wongsam
 * 
 */
public class ValidationFragment extends Fragment {
	private List<String> latestRoomList = Collections.emptyList();
	private List<String> currentRoomList = null;
	private Map<String, RoomStatistic> mapAlgoToCounter = new ConcurrentHashMap<String, RoomStatistic>();
	private WifiScannerPoller poller;

	/* dispatchedLocationQueries is used to keep track of dispatched api calls, such that it will wait for
	/* every API calls to return before proceeding
	 */
	private AtomicInteger dispatchedLocationQueries = new AtomicInteger(0);

	/**
	 * Uses AsyncTask and sleep to do the polling. executeOnExecutor is used
	 * because the default executor is a serial executor. The serial executor
	 * prevents the AsyncTask in locateMe from running.
	 * 
	 * @param view
	 */
	public synchronized void toggleValidationMode(final View view) {
		// Is the toggle on?
		if (((ToggleButton) view).isChecked()) {
			final AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) getActivity().findViewById(R.id.roomPicker);
			final Button submitValidationButton = (Button) getActivity().findViewById(R.id.submitValidationButton);
			// reset counters
			mapAlgoToCounter.clear();
			dispatchedLocationQueries.set(0);

			// Lock the room name, disable the button, update button text
			submitValidationButton.setEnabled(false);
			final String expectedRoom = autoCompleteTextView.getText().toString();
			submitValidationButton.setText(getText(R.string.collectingValidationForRoom_) + " " + expectedRoom);

			// begin polling
			poller = new WifiScannerPoller() {

				@Override
				protected void onProgressUpdate(WifiInformation... values) {
					dispatchedLocationQueries.incrementAndGet();
					validateLocation(expectedRoom, values[0]);
				}

				@Override
				protected void onCancelled() {
					// Wait for all jobs to return first
					int sleepTime = 50;
					while (sleepTime < 3000 && dispatchedLocationQueries.get() > 0) {
						SystemClock.sleep(sleepTime);
						sleepTime *= 2;
					}
					// prepare the payload
					final List<RoomStatistic> statistics = new ArrayList<RoomStatistic>(mapAlgoToCounter.values());
					// enable the submit button, update text
					submitValidationButton.setEnabled(true);
					submitValidationButton.setText(getText(R.string.submitValidationForRoom_) + " for " + expectedRoom);
					// update summary text
					TextView summary = (TextView) getActivity().findViewById(R.id.validationSummary);
					StringBuilder sb = new StringBuilder();
					sb.append(String.format("%s\n", new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date())));
					for (RoomStatistic stat : statistics) {
						sb.append(String.format("%s: Hit/Trials: %d/%d %%: %.2f\nDetails\n", stat.getAlgorithmName(), stat.getHits(), stat.getNumOfTrials(),
								1.0 * stat.getHits() / stat.getNumOfTrials()));
						for (Entry<String, AtomicInteger> entry : stat.getRoomToHitMap().entrySet()) {
							sb.append(entry.getKey() + ":" + entry.getValue().get() + "\n");
						}
						sb.append("\n");
					}
					summary.setText(sb.toString());

					// click to submit statistics
					submitValidationButton.setOnClickListener(new OnClickListener() {
						// for passing generic List to PutStatistics
						@SuppressWarnings("unchecked")
						@Override
						public void onClick(View v) {
							// disable the button, let user know it is
							// uploading
							submitValidationButton.setEnabled(false);
							submitValidationButton.setText(getText(R.string.submittingValidationDataForRoom_) + " " + expectedRoom);
							// upload data
							new PutStatistics(getActivity()) {
								@Override
								protected void onPostExecute(Response result) {
									// Upload finished
									if (!result.getReturnCode().equals(ReturnCode.OK)) {
										// Something went wrong, enable
										// button to let user retry
										submitValidationButton.setEnabled(true);
										Console.println(getActivity(), LogLevel.ERROR, LogTag.APICALL,
												"Failed to submit statistics. " + result.getExplanation());
										return;
									}
									// Succeeded, change text but remain
									// disabled
									submitValidationButton.setText(getText(R.string.submittedValidationDataForRoom_) + " " + expectedRoom);
									Console.println(getActivity(), LogLevel.ERROR, LogTag.APICALL, "statistics submitted. " + result.getExplanation());
								}
							}.execute(statistics);
						}
					});

				}
			};
			poller.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getActivity());
		} else {
			poller.cancel(false);
		}
	}

	private void validateLocation(final String expectedRoom, WifiInformation scanResult) {
		new RoomQuery(getActivity(), scanResult) {
			@Override
			protected void onPostExecute(final ResponseWithReports results) {
				if (!results.getReturnCode().equals(ReturnCode.OK)) {
					Console.println(getActivity(), LogLevel.ERROR, LogTag.APICALL, "No response from server. " + results.getExplanation());
					for (Exception e : this.getExceptions()) {
						Console.println(getActivity(), LogLevel.ERROR, LogTag.APICALL, e.toString());
					}
					dispatchedLocationQueries.decrementAndGet();
					return;
				}
				for (final Report report : results.getReports()) {
					final String predictedRoom = report.getBestMatch().getAlias();
					final String algoName = report.getAlgorithm();
					if (!mapAlgoToCounter.containsKey(algoName)) {
						mapAlgoToCounter.put(algoName, new RoomStatistic().withRoomName(expectedRoom).withAlgorithm(algoName));
					}
					RoomStatistic stat = mapAlgoToCounter.get(algoName);
					stat.hit(predictedRoom);
					if (expectedRoom.equals(predictedRoom)) {
						Console.println(getActivity(), LogLevel.INFO, LogTag.RESULT,
								String.format("Match! increment counter. hit ratio: %d/%d", stat.getHits(), stat.getNumOfTrials()));
					} else {
						Console.println(getActivity(), LogLevel.INFO, LogTag.RESULT,
								String.format("Missed! increment counter. hit ratio: %d/%d", stat.getHits(), stat.getNumOfTrials()));
					}
					Console.println(getActivity(), LogLevel.INFO, LogTag.APICALL, "Algo: " + report.getAlgorithm() + " | Location Report: " + predictedRoom
							+ " | Notes: " + report.getNotes());

				}
				dispatchedLocationQueries.decrementAndGet();
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.validation_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		((ToggleButton) view.findViewById(R.id.validationToggler)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleValidationMode(v);
				return;
			}
		});
		super.onViewCreated(view, savedInstanceState);
	}

	public void updateAutoComplete(List<String> result, Activity activity) {
		latestRoomList = result;
		if (!latestRoomList.equals(currentRoomList)) {
			currentRoomList = latestRoomList;
			AutoCompleteTextView autoCompleteTextView = ((AutoCompleteTextView) activity.findViewById(R.id.roomPicker));
			if (autoCompleteTextView != null) {
				String[] resultArray = latestRoomList.toArray(new String[latestRoomList.size()]);
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_dropdown_item_1line, resultArray);
				((AutoCompleteTextView) activity.findViewById(R.id.roomPicker)).setAdapter(adapter);
			}
		}
	}
}