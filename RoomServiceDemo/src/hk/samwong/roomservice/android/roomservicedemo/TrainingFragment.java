package hk.samwong.roomservice.android.roomservicedemo;

import hk.samwong.roomservice.android.library.apicalls.RoomTrainer;
import hk.samwong.roomservice.android.library.constants.LogLevel;
import hk.samwong.roomservice.android.library.constants.LogTag;
import hk.samwong.roomservice.android.roomservicedemo.helper.Console;
import hk.samwong.roomservice.commons.dataFormat.Response;
import hk.samwong.roomservice.commons.dataFormat.WifiInformation;
import hk.samwong.roomservice.commons.parameterEnums.ReturnCode;

import java.util.Collections;
import java.util.List;

import android.app.Activity;
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

	private RoomTrainer roomTrainer;
	public synchronized void toggleDataCollectionMode(final View view) {
		// Is the toggle on?
		boolean on = ((ToggleButton) view).isChecked();
		final Button submitButton = (Button) getActivity().findViewById(R.id.submitDataButton);
		if (on) {
			submitButton.setText(getText(R.string.submitDataForRoomWaiting));
			submitButton.setEnabled(false);
			AutoCompleteTextView textView = (AutoCompleteTextView) getActivity().findViewById(R.id.newRoomIdentifier);
			String roomName = textView.getText().toString();
			
			roomTrainer = new RoomTrainer(getActivity()) {
				int numOfDatapoints = 0;

				@Override
				protected void onProgressUpdate(WifiInformation[] values) {
					Console.println(getActivity(), LogLevel.INFO, LogTag.RESULT, "recorded " + ++numOfDatapoints + " datapoints");
				}
				
				@Override
				protected void onPostExecute(Response result) {
					if (result.getReturnCode().equals(ReturnCode.OK)) {
						submitButton.setText(getString(R.string.submittedDataForRoom_) + " " + roomTrainer.getRoomLabel());
						Console.println(getActivity(), LogLevel.ERROR, LogTag.APICALL, "OK");
					} else {
						submitButton.setEnabled(true);
						Console.println(getActivity(), LogLevel.ERROR, LogTag.APICALL, result.getExplanation());
					}
				}
			}.beginCollection().setRoomLabel(roomName);
			

			
		} else {
			roomTrainer.stopCollection();
			submitButton.setText(String.format("%s %s", getString(R.string.submitDataForRoom_), roomTrainer.getRoomLabel()));
			submitButton.setEnabled(true);
			submitButton.setOnClickListener(new OnClickListener() {
				// This is for varargs parameter. This should be type safe.
				@Override
				public void onClick(View v) {
					submitButton.setEnabled(false);
					submitButton.setText(getString(R.string.submittingDataForRoom_) + " " + roomTrainer.getRoomLabel());
					roomTrainer.submit();
				}
			});

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
