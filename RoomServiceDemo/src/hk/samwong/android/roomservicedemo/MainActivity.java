package hk.samwong.android.roomservicedemo;

import hk.samwong.android.roomserviceandroidlibrary.apicalls.GetListOfRooms;
import hk.samwong.android.roomserviceandroidlibrary.constants.LogLevel;
import hk.samwong.android.roomserviceandroidlibrary.constants.LogTag;
import hk.samwong.android.roomservicedemo.helper.Console;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Entry point
 * @author wongsam
 *
 */
public class MainActivity extends FragmentActivity {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;



	private final Activity thisActivity = this; // for passing into helpers. 
	private List<String> latestRoomList = Collections.emptyList();
	
	private void updateRoomList() {
		new GetListOfRooms(this) {
			@Override
			protected void onPostExecute(final List<String> result) {
				Console.println(thisActivity, LogLevel.INFO, LogTag.APICALL,
						"Received roomList:" + result);
				if (result == null) {
					Console.println(
							thisActivity,
							LogLevel.ERROR,
							LogTag.APICALL,
							"No response for the roomList query."
									+ this.getLastException());
					return;
				}else{
					latestRoomList = result;
				}
			}
		}.execute();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_validation);

		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		
		// Set it to update autocomplete list on every scroll
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int pageNo) {
				switch(pageNo){
				case 1:
					((TrainingFragment)mSectionsPagerAdapter.getItem(1)).updateAutoComplete(latestRoomList, thisActivity);
				case 2:
					((ValidationFragment)mSectionsPagerAdapter.getItem(2)).updateAutoComplete(latestRoomList, thisActivity);
				}
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {}

			@Override
			public void onPageScrolled(int arg0, float arg1,
					int arg2) {}
			
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Fetch room lists for autocomplete
		updateRoomList();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_validation, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.menu_reloadRoomList:
			updateRoomList();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			switch (position) {
			case 0:
				return new ClassifierFragment();
			case 1: 
				return new TrainingFragment();
			case 2:
				return new ValidationFragment();
			default:
				throw new NoSuchElementException();
			}
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return getString(R.string.title_section1);
			case 1:
				return getString(R.string.title_section2);
			case 2:
				return getString(R.string.title_section3);
			}
			return null;
		}
	}
}
