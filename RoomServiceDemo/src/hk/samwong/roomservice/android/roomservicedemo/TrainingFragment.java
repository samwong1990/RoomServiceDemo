package hk.samwong.roomservice.android.roomservicedemo;

import hk.samwong.roomservice.android.library.apicalls.SubmitBatchTrainingData;
import hk.samwong.roomservice.android.library.constants.LogLevel;
import hk.samwong.roomservice.android.library.constants.LogTag;
import hk.samwong.roomservice.android.library.helpers.TrainingDataAccumulator;
import hk.samwong.roomservice.android.roomservicedemo.helper.Console;
import hk.samwong.roomservice.commons.dataFormat.Response;
import hk.samwong.roomservice.commons.dataFormat.WifiInformation;
import hk.samwong.roomservice.commons.parameterEnums.ReturnCode;

import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ToggleButton;


/**
 * Simple interface for adding new rooms / contributing more data points
 * @author wongsam
 *
 */
public class TrainingFragment extends Fragment {
	private List<String> latestRoomList = Collections.emptyList();
	private List<String> currentRoomList = null;

	private TrainingDataAccumulator currentAccumulator;

	public void updateAutoComplete(List<String> result, Activity activity) {
		latestRoomList = result;
		if (!latestRoomList.equals(currentRoomList)) {
			currentRoomList = latestRoomList;
			AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) activity.findViewById(R.id.newRoomIdentifier);
			if (autoCompleteTextView != null) {
				String[] resultArray = latestRoomList.toArray(new String[latestRoomList.size()]);
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_dropdown_item_1line, resultArray);
				autoCompleteTextView.setAdapter(adapter);
			}
		}
	}

	public synchronized void toggleDataCollectionMode(final View view) {
		// Is the toggle on?
		boolean on = ((ToggleButton) view).isChecked();
		final Button submitButton = (Button) getActivity().findViewById(R.id.submitDataButton);
		if (on) {
			submitButton.setText(getText(R.string.submitDataForRoomWaiting));
			submitButton.setEnabled(false);
			AutoCompleteTextView textView = (AutoCompleteTextView) getActivity().findViewById(R.id.newRoomIdentifier);
			String roomName = textView.getText().toString();
			
			currentAccumulator = new TrainingDataAccumulator(roomName) {
				int numOfDatapoints = 0;

				@Override
				protected void onProgressUpdate(WifiInformation... scanResult) {
					Console.println(getActivity(), LogLevel.INFO, LogTag.RESULT, "recorded " + ++numOfDatapoints + " datapoints");
				}

				@Override
				protected void onCancelled(final List<WifiInformation> result) {
					Console.println(getActivity(), LogLevel.INFO, LogTag.RESULT, "Done, recorded " + result.size() + " datapoint.");
					submitButton.setText(String.format("%s %s", getString(R.string.submitDataForRoom_), currentAccumulator.getRoomName()));
					submitButton.setEnabled(true);
					submitButton.setOnClickListener(new OnClickListener() {
						// This is for varargs parameter. This should be type safe.
						@SuppressWarnings("unchecked")
						@Override
						public void onClick(View v) {
							submitButton.setEnabled(false);
							submitButton.setText(getString(R.string.submittingDataForRoom_) + " " + currentAccumulator.getRoomName());
							new SubmitBatchTrainingData(currentAccumulator.getRoomName(), getActivity()) {
								@Override
								protected void onPostExecute(Response result) {
									if (result.getReturnCode().equals(ReturnCode.OK)) {
										submitButton.setText(getString(R.string.submittedDataForRoom_) + " " + currentAccumulator.getRoomName());
										Console.println(getActivity(), LogLevel.ERROR, LogTag.APICALL, "OK");
									} else {
										submitButton.setEnabled(true);
										Console.println(getActivity(), LogLevel.ERROR, LogTag.APICALL, result.getExplanation());
									}
								}
							}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, result);
						}
					});
				}
			};
			currentAccumulator.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getActivity());
		} else {
			currentAccumulator.cancel(false);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		((ToggleButton) getActivity().findViewById(R.id.ToggleDataCollection)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleDataCollectionMode(v);
			}
		});
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.activity_training, container, false);

	}
}
