/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.backup;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.widget.Button;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.BackupFile;
import com.weebly.opus1269.clipman.backup.BackupHelper;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.ui.errorviewer.ErrorViewerActivity;

/** Handlers for UI events */
public class BackupHandlers implements
  DialogInterface.OnClickListener {
  /** Our activity */
  private final BackupActivity mActivity;

  /** File that may be operated on */
  private BackupFile mFile;

  BackupHandlers(BackupActivity backupActivity) {
    this.mActivity = backupActivity;
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    final Button button = ((AlertDialog) dialog).getButton(which);
    final String btnText = button.getText().toString();

    Analytics.INST(mActivity).buttonClick(mActivity.getTAG(), button);

    if (mActivity.getString(R.string.button_delete).equals(btnText)) {
      new BackupHelper.DeleteBackupAsyncTask(mActivity, mFile).executeMe();
    } else if (mActivity.getString(R.string.button_restore).equals(btnText)) {
      new BackupHelper
        .GetBackupContentsAsyncTask(mActivity, mFile, false).executeMe();
    } else if (mActivity.getString(R.string.button_sync).equals(btnText)) {
      new BackupHelper
        .GetBackupContentsAsyncTask(mActivity, mFile, true).executeMe();
    } else if (mActivity.getString(R.string.button_backup).equals(btnText)) {
      new BackupHelper.CreateBackupAsyncTask(mActivity).executeMe();
    } else if (mActivity.getString(R.string.button_details).equals(btnText)) {
      final Intent intent = new Intent(mActivity, ErrorViewerActivity.class);
      AppUtils.startActivity(mActivity, intent);
    }
    mFile = null;
  }

  /**
   * Click on fab button
   * @param context A context
   */
  public void onFabClick(Context context) {
    Analytics.INST(context).imageClick(mActivity.getTAG(), "refreshBackups");
    new BackupHelper.GetBackupsAsyncTask(mActivity).executeMe();
  }

  /**
   * Click on backup menu
   * @param context The View
   * @param menu    The menu
   */
  public void onBackupClick(Context context, MenuItem menu) {
    Analytics.INST(context).menuClick(mActivity.getTAG(), menu);
    showDialog(R.string.backup_dialog_backup_message, R.string.button_backup);
  }

  /**
   * Click on restore button
   * @param context The View
   * @param file    The file
   */
  public void onRestoreClick(Context context, BackupFile file) {
    Analytics.INST(context).imageClick(mActivity.getTAG(), "restoreBackup");
    mFile = file;
    showDialog(R.string.backup_dialog_restore_message, R.string.button_restore);
  }

  /**
   * Click on sync button
   * @param context The View
   * @param file    The file
   */
  public void onSyncClick(Context context, BackupFile file) {
    Analytics.INST(context).imageClick(mActivity.getTAG(), "syncBackup");
    mFile = file;
    showDialog(R.string.backup_dialog_sync_message, R.string.button_sync);
  }

  /**
   * Click on delete button
   * @param context The View
   * @param file    The file
   */
  public void onDeleteClick(Context context, BackupFile file) {
    Analytics.INST(context).imageClick(mActivity.getTAG(), "deleteBackup");
    mFile = file;
    showDialog(R.string.backup_dialog_delete_message, R.string.button_delete);
  }

  /**
   * Display an error in a dialog
   * @param title dialog title
   * @param msg   dialog meesage
   */
  public void showErrorMessage(@NonNull String title, @NonNull String msg) {
    mActivity.getViewModel().postIsLoading(false);
    final AlertDialog alertDialog = new AlertDialog.Builder(mActivity)
      .setTitle(title)
      .setMessage(msg)
      .setPositiveButton(R.string.button_dismiss, null)
      .setNegativeButton(R.string.button_details, this)
      .create();

    alertDialog.show();
  }

  /**
   * Display confirmation dialog on undoable actions
   * @param msgId    resource id of dialog message
   * @param buttonId resource id of dialog positive button
   */
  private void showDialog(int msgId, int buttonId) {
    final AlertDialog alertDialog = new AlertDialog.Builder(mActivity)
      .setMessage(msgId)
      .setTitle(R.string.backup_dialog_title)
      .setPositiveButton(buttonId, this)
      .setNegativeButton(R.string.button_cancel, null)
      .create();

    alertDialog.show();
  }
}
