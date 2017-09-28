/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.services;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.ThreadedAsyncTask;
import com.weebly.opus1269.clipman.db.ClipContentProvider;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.Notifications;

/**
 * An app private {@link Service} to listen for changes to the clipboard,
 * persist them to storage using {@link ClipContentProvider} and push them to
 * registered fcm devices
 */
public class ClipboardWatcherService extends Service implements
  ClipboardManager.OnPrimaryClipChangedListener {
  private static final String TAG = "ClipboardWatcherService";

  /** {@link Intent} extra to indicate if service is starting on boot */
  private static final String EXTRA_ON_BOOT = "on_boot";

  /** The fastest we will process identical local copies {@value} */
  private static final long MIN_TIME_MILLIS = 200;

  /** The last text we read */
  private String mLastText;

  /** The last time we read */
  private long mLastTime;

  /** ye olde ClipboardManager */
  private ClipboardManager mClipboard = null;

  /**
   * Start ourselves
   * @param onBoot true if called on device boot
   */
  @TargetApi(26)
  public static void startService(Boolean onBoot) {
    if (Prefs.isMonitorClipboard()
      && !AppUtils.isMyServiceRunning(ClipboardWatcherService.class)) {
      // only start if the user has allowed it and we are not running
      final Context context = App.getContext();
      final Intent intent = new Intent(context, ClipboardWatcherService.class);
      intent.putExtra(EXTRA_ON_BOOT, onBoot);
      if (AppUtils.isOreoOrLater()) {
        context.startForegroundService(intent);
      } else {
        context.startService(intent);
      }
    }
  }

  @Override
  public void onCreate() {
    if (AppUtils.isOreoOrLater()) {
      Notifications.startAndShow(this);
    }

    mClipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    mClipboard.addPrimaryClipChangedListener(this);
    mLastText = "";
    mLastTime = System.currentTimeMillis();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    mLastText = "";
    mLastTime = System.currentTimeMillis();

    if (intent != null) {
      final boolean onBoot = intent.getBooleanExtra(EXTRA_ON_BOOT, false);
      if (!onBoot) {
        // don't process on boot
        processClipboard(true);
      }
    } else {
      processClipboard(true);
    }

    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mClipboard.removePrimaryClipChangedListener(this);
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    throw new UnsupportedOperationException("Unimplemented onBind method in: " + TAG);
  }

  @Override
  public void onPrimaryClipChanged() {
    processClipboard(false);
  }

  /**
   * Read the clipboard, then write to database asynchronously.
   * DO NOT read clipboard in AsyncTask, you will regret it my boy.
   * @param onNewOnly if true only update database if the current contents don't
   *                  exit
   */
  private void processClipboard(boolean onNewOnly) {
    final ClipItem clipItem =
      ClipItem.getFromClipboard(mClipboard);
    final long now = System.currentTimeMillis();
    final long deltaTime = now - mLastTime;
    mLastTime = now;

    if ((clipItem == null) || TextUtils.isEmpty(clipItem.getText())) {
      mLastText = "";
      return;
    }

    if (clipItem.isRemote()) {
      // ignore remote clips - they were already saved to DB
      mLastText = "";
      return;
    }

    if (mLastText.equals(clipItem.getText())) {
      if (deltaTime > MIN_TIME_MILLIS) {
        // only handle identical local copies this fast
        // some apps (at least Chrome) write to clipboard twice.
        new StoreClipAsyncTask(clipItem).executeMe(onNewOnly);
      }
    } else {
      // normal situation, fire away
      new StoreClipAsyncTask(clipItem).executeMe(onNewOnly);
    }
    mLastText = clipItem.getText();
  }

  /**
   * AsyncTask to write to the Clip database
   */
  private class StoreClipAsyncTask extends ThreadedAsyncTask<Boolean, Void, Void> {
    final ClipItem mClipItem;
    boolean mResult;

    StoreClipAsyncTask(ClipItem clipItem) {
      mClipItem = clipItem;
    }

    @Override
    protected Void doInBackground(Boolean... params) {
      final Boolean onNewOnly = params[0];
      if (onNewOnly) {
        mResult = mClipItem.saveIfNew();
      } else {
        mResult = mClipItem.save();
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      if (mResult) {
        // display notification if requested by user
        Notifications.show(mClipItem);

        if (!mClipItem.isRemote() && Prefs.isAutoSend()) {
          // send local copy to server for delivery
          mClipItem.send();
        }
      }
    }
  }
}
