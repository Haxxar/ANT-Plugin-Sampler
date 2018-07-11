/*
This software is subject to the license described in the License.txt file
included with this software distribution. You may not use this file except in compliance
with this license.

Copyright (c) Dynastream Innovations Inc. 2013
All rights reserved.
 */

package com.dsi.ant.antplus.pluginsampler.heartrate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dsi.ant.antplus.pluginsampler.R;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.DataState;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.ICalculatedRrIntervalReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IPage4AddtDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.RrFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc.IRssiReceiver;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.ICumulativeOperatingTimeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.IManufacturerAndSerialReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.IVersionAndModelReceiver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Base class to connects to Heart Rate Plugin and display all the event data.
 */
public abstract class Activity_HeartRateDisplayBase extends Activity
{
    protected abstract void requestAccessToPcc();

    AntPlusHeartRatePcc hrPcc = null;
    protected PccReleaseHandle<AntPlusHeartRatePcc> releaseHandle = null;

    TimerTask timer;
    int timeInSec = 0;
    TextView tv_status;

    TextView tv_estTimestamp;

    TextView tv_computedHeartRate;
    TextView tv_heartBeatCounter;
    TextView tv_heartBeatEventTime;
    TextView tv_time;
    TextView tv_interesting;
    



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);


        handleReset();
    }
    
    public void onClick(View v){
    	File root = android.os.Environment.getExternalStorageDirectory(); 
        

        // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder

        File dir = new File (root.getAbsolutePath() + "/download");
        dir.mkdirs();
        File file = new File(dir, "myData.txt");

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            pw.println("Hi , How are you");
            pw.println("Hello");
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            
        } catch (IOException e) {
            e.printStackTrace();
        }   
       
    

    	Timer T=new Timer();
        T.scheduleAtFixedRate(new TimerTask() {         
                @Override
                public void run() {
                	String toWrite = Integer.toString(timeInSec);
                	
					
                    timeInSec++;                
                }
            }, 1000, 1000);
    	
    }
    
    
    

    /**
     * Resets the PCC connection to request access again and clears any existing display data.
     */
    protected void handleReset()
    {
        //Release the old access if it exists
        if(releaseHandle != null)
        {
            releaseHandle.close();
        }

        requestAccessToPcc();
    }

    protected void showDataDisplay(String status)
    {
        setContentView(R.layout.activity_heart_rate);

        tv_status = (TextView)findViewById(R.id.textView_Status);

        tv_estTimestamp = (TextView)findViewById(R.id.textView_EstTimestamp);

      

        tv_computedHeartRate = (TextView)findViewById(R.id.textView_ComputedHeartRate);
        tv_heartBeatCounter = (TextView)findViewById(R.id.textView_HeartBeatCounter);
        tv_heartBeatEventTime = (TextView)findViewById(R.id.textView_HeartBeatEventTime);
        tv_time = (TextView)findViewById(R.id.textView_Time);
        tv_interesting =(TextView)findViewById(R.id.textView_Interest);

      

        //Reset the text display
        tv_status.setText(status);

        tv_estTimestamp.setText("---");
        
       tv_time.setText("---");
       tv_interesting.setText("---");
   

        tv_computedHeartRate.setText("---");
        tv_heartBeatCounter.setText("---");
        tv_heartBeatEventTime.setText("---");


    
    }

    /**
     * Switches the active view to the data display and subscribes to all the data events
     */
    public void subscribeToHrEvents()
    {
        hrPcc.subscribeHeartRateDataEvent(new IHeartRateDataReceiver()
        {
            @Override
            public void onNewHeartRateData(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                final int computedHeartRate, final long heartBeatCount,
                final BigDecimal heartBeatEventTime, final DataState dataState)
            {
                // Mark heart rate with asterisk if zero detected
                final String textHeartRate = String.valueOf(computedHeartRate)
                    + ((DataState.ZERO_DETECTED.equals(dataState)) ? "*" : "");

                // Mark heart beat count and heart beat event time with asterisk if initial value
                final String textHeartBeatCount = String.valueOf(heartBeatCount)
                    + ((DataState.INITIAL_VALUE.equals(dataState)) ? "*" : "");
                final String textHeartBeatEventTime = String.valueOf(heartBeatEventTime)
                    + ((DataState.INITIAL_VALUE.equals(dataState)) ? "*" : "");

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                    	String blah = Integer.toString(timeInSec);
                        tv_estTimestamp.setText(String.valueOf(estTimestamp));

                        tv_computedHeartRate.setText(textHeartRate);
                        tv_heartBeatCounter.setText(textHeartBeatCount);
                        tv_heartBeatEventTime.setText(textHeartBeatEventTime);
                        tv_time.setText(blah);

                       
                    }
                });
            }
        });

        hrPcc.subscribePage4AddtDataEvent(new IPage4AddtDataReceiver()
        {
            @Override
            public void onNewPage4AddtData(final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                final int manufacturerSpecificByte,
                final BigDecimal previousHeartBeatEventTime)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        tv_estTimestamp.setText(String.valueOf(estTimestamp));

                        
                    }
                });
            }
        });

        hrPcc.subscribeCumulativeOperatingTimeEvent(new ICumulativeOperatingTimeReceiver()
        {
            @Override
            public void onNewCumulativeOperatingTime(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long cumulativeOperatingTime)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        tv_estTimestamp.setText(String.valueOf(estTimestamp));

                       
                    }
                });
            }
        });

        hrPcc.subscribeManufacturerAndSerialEvent(new IManufacturerAndSerialReceiver()
        {
            @Override
            public void onNewManufacturerAndSerial(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final int manufacturerID,
                final int serialNumber)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        tv_estTimestamp.setText(String.valueOf(estTimestamp));

                     
                    }
                });
            }
        });

        hrPcc.subscribeVersionAndModelEvent(new IVersionAndModelReceiver()
        {
            @Override
            public void onNewVersionAndModel(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final int hardwareVersion,
                final int softwareVersion, final int modelNumber)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        tv_estTimestamp.setText(String.valueOf(estTimestamp));

                    
                    }
                });
            }
        });

        hrPcc.subscribeCalculatedRrIntervalEvent(new ICalculatedRrIntervalReceiver()
        {
            @Override
            public void onNewCalculatedRrInterval(final long estTimestamp,
                EnumSet<EventFlag> eventFlags, final BigDecimal rrInterval, final RrFlag flag)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        tv_estTimestamp.setText(String.valueOf(estTimestamp));
                    

                        // Mark RR with asterisk if source is not cached or page 4
                        if (flag.equals(RrFlag.DATA_SOURCE_CACHED)
                            || flag.equals(RrFlag.DATA_SOURCE_PAGE_4));
                          
                      
                    }
                });
            }
        });

        hrPcc.subscribeRssiEvent(new IRssiReceiver() {
            @Override
            public void onRssiData(final long estTimestamp, final EnumSet<EventFlag> evtFlags, final int rssi) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_estTimestamp.setText(String.valueOf(estTimestamp));
                      
                    }
                });
            }
        });
    }

    protected IPluginAccessResultReceiver<AntPlusHeartRatePcc> base_IPluginAccessResultReceiver =
        new IPluginAccessResultReceiver<AntPlusHeartRatePcc>()
        {
        //Handle the result, connecting to events on success or reporting failure to user.
        @Override
        public void onResultReceived(AntPlusHeartRatePcc result, RequestAccessResult resultCode,
            DeviceState initialDeviceState)
        {
            showDataDisplay("Connecting...");
            switch(resultCode)
            {
                case SUCCESS:
                    hrPcc = result;
                    tv_status.setText(result.getDeviceName() + ": " + initialDeviceState);
                    subscribeToHrEvents();
                  
                case CHANNEL_NOT_AVAILABLE:
                    Toast.makeText(Activity_HeartRateDisplayBase.this, "Channel Not Available", Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case ADAPTER_NOT_DETECTED:
                    Toast.makeText(Activity_HeartRateDisplayBase.this, "ANT Adapter Not Available. Built-in ANT hardware or external adapter required.", Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case BAD_PARAMS:
                    //Note: Since we compose all the params ourself, we should never see this result
                    Toast.makeText(Activity_HeartRateDisplayBase.this, "Bad request parameters.", Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case OTHER_FAILURE:
                    Toast.makeText(Activity_HeartRateDisplayBase.this, "RequestAccess failed. See logcat for details.", Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case DEPENDENCY_NOT_INSTALLED:
                    tv_status.setText("Error. Do Menu->Reset.");
                    AlertDialog.Builder adlgBldr = new AlertDialog.Builder(Activity_HeartRateDisplayBase.this);
                    adlgBldr.setTitle("Missing Dependency");
                    adlgBldr.setMessage("The required service\n\"" + AntPlusHeartRatePcc.getMissingDependencyName() + "\"\n was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?");
                    adlgBldr.setCancelable(true);
                    adlgBldr.setPositiveButton("Go to Store", new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            Intent startStore = null;
                            startStore = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=" + AntPlusHeartRatePcc.getMissingDependencyPackageName()));
                            startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            Activity_HeartRateDisplayBase.this.startActivity(startStore);
                        }
                    });
                    adlgBldr.setNegativeButton("Cancel", new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    });

                    final AlertDialog waitDialog = adlgBldr.create();
                    waitDialog.show();
                    break;
                case USER_CANCELLED:
                    tv_status.setText("Cancelled. Do Menu->Reset.");
                    break;
                case UNRECOGNIZED:
                    Toast.makeText(Activity_HeartRateDisplayBase.this,
                        "Failed: UNRECOGNIZED. PluginLib Upgrade Required?",
                        Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                default:
                    Toast.makeText(Activity_HeartRateDisplayBase.this, "Unrecognized result: " + resultCode, Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
            }
        }
        };

        //Receives state changes and shows it on the status display line
        protected  IDeviceStateChangeReceiver base_IDeviceStateChangeReceiver =
            new IDeviceStateChangeReceiver()
        {
            @Override
            public void onDeviceStateChange(final DeviceState newDeviceState)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        tv_status.setText(hrPcc.getDeviceName() + ": " + newDeviceState);
                    }
                });


            }
        };

        @Override
        protected void onDestroy()
        {
            if(releaseHandle != null)
            {
                releaseHandle.close();
            }
            super.onDestroy();
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu)
        {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.activity_heart_rate, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            switch(item.getItemId())
            {
                case R.id.menu_reset:
                    handleReset();
                    tv_status.setText("Resetting...");
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }
}
