package hk.samwong.android.roomservicedemo.helper;

import hk.samwong.android.roomserviceandroidlibrary.constants.LogLevel;
import hk.samwong.android.roomserviceandroidlibrary.constants.LogTag;
import hk.samwong.android.roomservicedemo.R;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;


/**
 * Display logs on the TextViews across the fragments.
 * @author wongsam
 *
 */
public class Console {
	private static List<Integer> logTextViewsIDs = Arrays.asList(R.id.logTextView, R.id.logTextViewInTrainingActivity, R.id.logTextViewInValidation);
		
	public static void println(Activity activity, LogLevel level, LogTag tag,
			String msg) {
		switch (level) {
		case DEBUG:
			Log.d(tag.toString(), msg);
			return;
		case INFO:
			Log.i(tag.toString(), msg);
			break;
		case ERROR:
			Log.e(tag.toString(), msg);
			break;
		default:
			break;
		}
		if (!level.equals(LogLevel.DEBUG)) {	// change this line to show more logs.
			String formattedMessage = String.format("%s:%s\n",
					new SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
							.format(new Date()), msg);
			
			for (int id : logTextViewsIDs) {
				TextView textView = (TextView) activity.findViewById(id);
				if(textView != null){
					StringBuffer buffer = new StringBuffer(textView.getText());
					buffer.insert(0, formattedMessage);
					buffer.setLength(1024);
					textView.setText(buffer);
				}
			}
		}
		return;
	}
}
