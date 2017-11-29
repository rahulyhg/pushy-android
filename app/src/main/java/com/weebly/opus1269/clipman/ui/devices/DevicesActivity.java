/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.devices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.msg.MessagingClient;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.model.Notifications;

/** Activity to manage our connected devices */
public class DevicesActivity extends BaseActivity {

  /** Adapter being used to display the list's data */
  private DevicesAdapter mAdapter = null;

  /** Receiver to be notified of changes */
  private BroadcastReceiver mReceiver = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    mLayoutID = R.layout.activity_devices;

    super.onCreate(savedInstanceState);

    final FloatingActionButton fab = findViewById(R.id.fab);
    if (fab != null) {
      fab.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          doRefresh();
          Analytics.INST(v.getContext()).imageClick(TAG, "refreshDevices");
        }
      });
    }

    setupRecyclerView();

    setupDevicesBroadcastReceiver();
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Register mReceiver to receive Device notifications.
    LocalBroadcastManager.getInstance(this)
      .registerReceiver(mReceiver,
        new IntentFilter(Intents.FILTER_DEVICES));

    Notifications.removeDevices();

    // ping devices
    MessagingClient.sendPing();

    // so relative dates get updated
    mAdapter.notifyDataSetChanged();
  }

  @Override
  protected void onPause() {
    super.onPause();

    // Unregister since the activity is not visible
    LocalBroadcastManager.getInstance(this)
      .unregisterReceiver(mReceiver);
  }

  /** Connect the {@link DevicesAdapter} to the {@link RecyclerView} */
  private void setupRecyclerView() {
    final RecyclerView recyclerView =
      findViewById(R.id.deviceList);
    if (recyclerView != null) {
      mAdapter = new DevicesAdapter(this);
      recyclerView.setAdapter(mAdapter);
      recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
  }

  /** Create the {@link BroadcastReceiver} to handle changes to the list */
  private void setupDevicesBroadcastReceiver() {
    // handler for received Intents for the "devices" event
    mReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        notifyAdapter(intent);
      }

      private void notifyAdapter(Intent intent) {
        final Bundle bundle = intent.getBundleExtra(Intents.BUNDLE_DEVICES);
        final String action = bundle.getString(Intents.ACTION_TYPE_DEVICES);
        if (action == null) {
          return;
        }

        switch (action) {
          case Intents.TYPE_UPDATE_DEVICES:
            mAdapter.notifyDataSetChanged();
            break;
          default:
            break;
        }
      }
    };
  }

  /** Refresh the list */
  private void doRefresh() {
    mAdapter.notifyDataSetChanged();
    MessagingClient.sendPing();
  }
}
