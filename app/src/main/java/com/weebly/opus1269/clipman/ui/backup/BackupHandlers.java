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
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.entity.Backup;
import com.weebly.opus1269.clipman.backup.BackupHelper;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.ErrorMsg;
import com.weebly.opus1269.clipman.repos.BackupRepo;
import com.weebly.opus1269.clipman.ui.base.BaseHandlers;
import com.weebly.opus1269.clipman.ui.errorviewer.ErrorViewerActivity;

/** Handlers for UI events */
public class BackupHandlers extends BaseHandlers {
  /** Our activity */
  private final BackupActivity mActivity;

  /** Backup that may be operated on */
  private Backup mBackup;

  BackupHandlers(BackupActivity backupActivity) {
    this.mActivity = backupActivity;
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    final Button button = ((AlertDialog) dialog).getButton(which);
    final Context context = button.getContext();
    final String btnText = button.getText().toString();

    Analytics.INST(context).buttonClick(mActivity.getTAG(), button);

    if (mActivity.getString(R.string.button_delete).equals(btnText)) {
      BackupHelper.INST(App.INST()).deleteBackupAsync(mBackup);
    } else if (mActivity.getString(R.string.button_restore).equals(btnText)) {
      BackupHelper.INST(context).restoreBackupAsync(mBackup);
    } else if (mActivity.getString(R.string.button_sync).equals(btnText)) {
      BackupHelper.INST(context).syncContentsAsync(mBackup);
    } else if (mActivity.getString(R.string.button_backup).equals(btnText)) {
      BackupHelper.INST(App.INST()).createBackupAsync();
    } else if (mActivity.getString(R.string.button_details).equals(btnText)) {
      final Intent intent = new Intent(context, ErrorViewerActivity.class);
      AppUtils.startActivity(context, intent);
    }
    mBackup = null;
  }

  /**
   * Click on fab button
   * @param context A context
   */
  public void onFabClick(Context context) {
    Analytics.INST(context)
      .imageClick(mActivity.getTAG(), "refreshBackups");
    BackupHelper.INST(context).getBackupsAsync();
  }

  /**
   * Click on backup menu
   * @param context The View
   * @param menu    The menu
   */
  public void onBackupClick(Context context, MenuItem menu) {
    Analytics.INST(context).menuClick(mActivity.getTAG(), menu);
    showConfirmationDialog(context, R.string.backup_dialog_title,
      R.string.backup_dialog_backup_message, R.string.button_backup);
  }

  /**
   * Click on restore button
   * @param context The View
   * @param backup  The file
   */
  public void onRestoreClick(Context context, Backup backup) {
    Analytics.INST(context).imageClick(mActivity.getTAG(), "restoreBackup");
    mBackup = backup;
    showConfirmationDialog(context, R.string.backup_dialog_title,
      R.string.backup_dialog_restore_message, R.string.button_restore);
  }

  /**
   * Click on sync button
   * @param context The View
   * @param backup  The file
   */
  public void onSyncClick(Context context, Backup backup) {
    Analytics.INST(context).imageClick(mActivity.getTAG(), "syncBackup");
    mBackup = backup;
    showConfirmationDialog(context, R.string.backup_dialog_title,
      R.string.backup_dialog_sync_message, R.string.button_sync);
  }

  /**
   * Click on delete button
   * @param context The View
   * @param backup  The file
   */
  public void onDeleteClick(Context context, Backup backup) {
    Analytics.INST(context).imageClick(mActivity.getTAG(), "deleteBackup");
    mBackup = backup;
    showConfirmationDialog(context, R.string.backup_dialog_title,
      R.string.backup_dialog_delete_message, R.string.button_delete);
  }

  /**
   * Display an error in a dialog
   * @param errorMsg the error
   */
  public void showErrorMessage(@NonNull ErrorMsg errorMsg) {
    // reset error
    BackupRepo.INST(App.INST()).postErrorMsg(null);
    BackupRepo.INST(App.INST()).postIsWorking(false);
    new AlertDialog.Builder(mActivity)
      .setTitle(errorMsg.title)
      .setMessage(errorMsg.msg)
      .setPositiveButton(R.string.button_dismiss, null)
      .setNegativeButton(R.string.button_details, this)
      .create()
      .show();
  }
}
