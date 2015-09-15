package com.orbbec.obHand;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import com.orbbec.astrakernel.AstraContext;
import com.orbbec.astrakernel.PermissionCallbacks;
import com.orbbec.astrastartlibs.HandTracker;
import com.orbbec.astrastartlibs.UserTracker;
import org.openni.*;


public class MainActivity extends Activity {

	String TAG = "obHand-LOG";
	String VERSION = "0.0.5";

	String m_handinfo = "Hand Tracker : ";
	AstraContext m_xc;
	HandTracker m_HandTracker;

	UserTracker mUserTracker;
	String mCalibPose = null;
	boolean bexit = false;
	
	int mouse_last_move_x = 0;
	int mouse_last_move_y = 0;
	
	private PermissionCallbacks m_callbacks = new PermissionCallbacks() {

		@Override
		public void onDevicePermissionGranted() 
		{ 
			try 
			{
				m_HandTracker = new HandTracker(m_xc);
				
				m_HandTracker.addHandCreateObserver(new HandCreatedObserver());
				m_HandTracker.addHandDestroyedObserver(new HandDestroyedObserver()); 
				m_HandTracker.addHandUpdatedObserver(new HandUpdatedObserver()); 
				
			} catch (Exception e) 
			{
				e.printStackTrace();
			} 
 
			try 
			{ 
				m_xc.start();
				newThread.start();
			} catch (Exception e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace(); 
			}
		
		}
		
		@Override
		public void onDevicePermissionDenied() {

		}
	};


	private Thread newThread = new Thread( new Runnable() 
	{
		public void run()
		{
			while(!bexit)
			{
				try 
				{
					m_xc.waitforupdate();
				} catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}
	});


	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();

		Log.i(TAG,"onStop:onDestroy begin!");
		bexit = true;
		if(newThread != null && m_HandTracker != null && m_xc != null)
		{
			try {
				newThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			m_HandTracker.Close();
			m_xc.Close();
		}
		Log.i(TAG,"onStop:Activity Destory!");

		System.exit(0);

	}

/*
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_MENU) {
			bexit = true;
			Log.i(TAG,"Back key or Home key Detected!");
			System.exit(0);
		}
		return super.onKeyDown(keyCode, event);
	}
*/

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		TextView zen_vision = (TextView)findViewById(R.id.mouse_ouput);
		zen_vision.setText(m_handinfo);
		
		try {
			m_xc = new AstraContext(this, m_callbacks);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	protected void onDestroy() {

		Log.i(TAG,"onDestroy begin!");
		bexit = true;
		try {
			newThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		super.onDestroy();
		m_HandTracker.Close();
		m_xc.Close();
		Log.i(TAG,"Activity Destory!");
	}
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	class HandCreatedObserver implements IObserver<ActiveHandEventArgs> 
	{
		@Override
		public void update(IObservable<ActiveHandEventArgs> observable, ActiveHandEventArgs args) 
		{
			
			Message msg_create = new  Message();
			m_handinfo = "New hand created: " + args.getId();

				int mouse_x = (int) args.getPosition().getX();
				int mouse_y = (int) args.getPosition().getY();
				
				mouse_last_move_x = mouse_x;
				mouse_last_move_y = mouse_y;
				Log.v("test_output", m_handinfo);
				
				msg_create.what = 1;		
				handler.sendMessage(msg_create);
			
		}
		

	}
	
	class HandUpdatedObserver implements IObserver<ActiveHandEventArgs> 
	{
		@Override
		public void update(IObservable<ActiveHandEventArgs> observable, ActiveHandEventArgs args) 
		{	
			Message msg_update = new  Message();
			int mouse_x = (int) args.getPosition().getX();
			int mouse_y = (int) args.getPosition().getY();
			
			m_handinfo = "Hand update: " + args.getId() + " + "
					+ Float.toString(args.getPosition().getZ());

			
			Log.v("test_output_x", String.valueOf(mouse_x - mouse_last_move_x));
			Log.v("test_output_y", String.valueOf(mouse_last_move_y - mouse_y));
			
			
			mouse_last_move_x = mouse_x;
			mouse_last_move_y = mouse_y;
			
			msg_update.what = 1;		
			handler.sendMessage(msg_update);
			
			
		}
	}

	class HandDestroyedObserver implements IObserver<InactiveHandEventArgs> 
	{
		@Override
		public void update(IObservable<InactiveHandEventArgs> observable, InactiveHandEventArgs args) 
		{
			Message msg_destroy = new  Message();

			m_handinfo = "Hand destroyed: " + args.getId();
			Log.v("test_output", m_handinfo);
			
			msg_destroy.what = 1;		
			handler.sendMessage(msg_destroy);
		}
		
		
	}
	
	
	@SuppressLint("HandlerLeak")
	Handler handler = new Handler()
	{
			
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			switch(msg.what){ 
			case 1 :
			case 3 :
			case 2:
				TextView zen_vision = (TextView)findViewById(R.id.mouse_ouput);
				zen_vision.setText(m_handinfo);
				break ;

			}
		}
			
	};




	class NewUserObserver implements IObserver<UserEventArgs> {


		public void update(IObservable<UserEventArgs> observable,
						   UserEventArgs args) {


			try {
				if (mUserTracker.needPoseForCalibration()) {
					mUserTracker.startPoseDetection(mCalibPose, args.getId());
				} else {
					mUserTracker.requestSkeletonCalibration(args.getId(), true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	class LostUserObserver implements IObserver<UserEventArgs> {
		public void update(IObservable<UserEventArgs> observable,
						   UserEventArgs args) {


		}
	}

	class CalibrationCompleteObserver implements
			IObserver<CalibrationProgressEventArgs> {
		public void update(
				IObservable<CalibrationProgressEventArgs> observable,
				CalibrationProgressEventArgs args) {

			try {
				if (args.getStatus() == CalibrationProgressStatus.OK) {

					mUserTracker.startTracking(args.getUser());
				} else {
					if (mUserTracker.needPoseForCalibration()) {
						mUserTracker.startPoseDetection(mCalibPose, args.getUser());
					} else {
						mUserTracker.requestSkeletonCalibration(args.getUser(), true);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	class PoseDetectedObserver implements IObserver<PoseDetectionEventArgs> {
		public void update(IObservable<PoseDetectionEventArgs> observable,
						   PoseDetectionEventArgs args) {

			try {
				mUserTracker.stopPoseDetection(args.getUser());
				mUserTracker.requestSkeletonCalibration(args.getUser(), true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}




