package com.deqing.usb_flashlight_glass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.android.glass.app.Card;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	Handler handler = new Handler();
	
    //private List<Card> mCards;
    private Card message_card;
    UsbManager mUsbManager;
	PendingIntent mPermissionIntent;
	UsbDevice target_device=null;
	
	private static final String ACTION_USB_PERMISSION =
		    "com.android.example.USB_PERMISSION";
	
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        if (ACTION_USB_PERMISSION.equals(action)) {
	            synchronized (this) {
	                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

	                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
	                    if(device != null){
	                      //call method to set up device communication
	                    	System.out.println("GOT Permission!");
	                    	toggle_flashlight();
	                   }
	                }else {
	                	System.out.println("permission denied for device " + device);
	                }
	            }
	        }
	    }
	};
	
	private final BroadcastReceiver mUsbAttachedReceiver = new BroadcastReceiver(){
	@Override
	public void onReceive(Context context, Intent intent){
	        //BREAKPOINT HERE IS NEVER HIT
	        String action = intent.getAction();
	        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
	            synchronized(this){
	                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
	                if (device != null){
	                    //getDevicePermission(device);
	                	System.out.println("GOT DEVICE!");
	                }
	            }
	        }
	    }
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

		//IntentFilter attachedFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		//registerReceiver(mUsbAttachedReceiver, attachedFilter);
		
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(mUsbReceiver, filter);
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		
	    message_card = new Card(this);
	    message_card.setText("Trying to connect to usb flashlight");
	    message_card.setInfo("");
	    View card_view=message_card.toView ();
	    setContentView(card_view);
	    
	    search_usb_device();
	    if (target_device!=null){
	    	//toggle_flashlight();   	
	    }
	    
	}
	
	@Override
	protected void onResume() {
	    super.onResume();


	    
	    count_down_exit();
	}
	
	void count_down_exit(){
	    handler.removeCallbacksAndMessages(null);  
	    message_card.setInfo("Will exit in " + 3 + " seconds.");
	    View card_view=message_card.toView ();
	    setContentView(card_view);
	    int time_limit;
	    if (target_device==null || mUsbManager.hasPermission (target_device)){
	    	time_limit=3;
	    }else{
	    	time_limit=10;
	    }
	    for (int count = 0; count < time_limit; count++){
	    	final int count1=time_limit-1-count;
	        handler.postDelayed(new Runnable(){
	            @Override
	            public void run() {
	            	message_card.setInfo("Will exit in " + count1 + " seconds.");
	        	    View card_view=message_card.toView ();
	        	    setContentView(card_view);
	                if (count1==0){
	                	finish();	                	
	                }
	            }
	        }, 1000 * (count + 1));
	    }	
	}
	
	public boolean search_usb_device(){
		boolean result=false;
		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		
		System.out.println(deviceList);
		System.out.println(deviceList.size());
		int c=deviceList.size();
		int total=0;
		while(deviceIterator.hasNext()){
		    UsbDevice device = deviceIterator.next();
		    total++;
		    System.out.println("VID "+String.format("%04X", device.getVendorId())+" PID "+String.format("%04X", device.getProductId())+" NAME "+device.getDeviceName());
		    if (device.getVendorId()==0x4207 && device.getProductId()==0x20A0){
		    	target_device=device;
		    	break;
		    }
		}
		if (target_device!=null){
			System.out.println("GOT DEVICE!");
			message_card.setText("Flashlight found!");
    	    View card_view=message_card.toView ();
    	    setContentView(card_view);
			mUsbManager.requestPermission(target_device, mPermissionIntent);
			result=true;
		}else{
			System.out.println("NOT GOT DEVICE!!");
			message_card.setText("Not found!");
    	    View card_view=message_card.toView ();
    	    setContentView(card_view);
    	    result=false;
		}
		return result;
	}
	public void toggle_flashlight(){
		if (mUsbManager.hasPermission (target_device)){
			boolean forceClaim = true;
			UsbDeviceConnection usb_connection;
			
			System.out.println("There are "+target_device.getInterfaceCount()+" interfaces.");
			UsbInterface intf = target_device.getInterface(0);
			System.out.println("There are "+intf.getEndpointCount()+" endpoints.");
			
			usb_connection = mUsbManager.openDevice(target_device); 
			
			usb_connection.claimInterface(intf, forceClaim);
			
			UsbEndpoint out_endpoint = null;
			for (int i=0;i<intf.getEndpointCount();i++){
				UsbEndpoint endpoint = intf.getEndpoint(i);
				if (endpoint.getDirection()==UsbConstants.USB_DIR_OUT) {
					out_endpoint=endpoint;
				}
			}
			
			if (out_endpoint!=null){
				byte[] bytes={(byte) 0x80};
				int TIMEOUT = 0;
				usb_connection.bulkTransfer (out_endpoint, bytes, 1, TIMEOUT);
				message_card.setText("Flashlight Toggled");
	    	    View card_view=message_card.toView ();
	    	    setContentView(card_view);
			}
		}else{
			message_card.setText("NO USB permission");
    	    View card_view=message_card.toView ();
    	    setContentView(card_view);
		}
	}


    @Override
    public void onDestroy() {
    	handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
