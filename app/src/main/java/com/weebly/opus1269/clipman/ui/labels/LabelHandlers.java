/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.repos.MainRepo;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.base.BaseHandlers;
import com.weebly.opus1269.clipman.viewmodel.LabelViewModel;

/** Handlers for UI events */
public class LabelHandlers extends BaseHandlers {
  private final BaseActivity mActivity;
  private final String TAG;
  private LabelEntity mLabelEntity;

  LabelHandlers(BaseActivity baseActivity) {
    super();
    this.mActivity = baseActivity;
    this.TAG = baseActivity.getTAG();
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    final Button button = ((AlertDialog) dialog).getButton(which);
    final String btnText = button.getText().toString();

    Analytics.INST(button.getContext()).buttonClick(TAG, button);

    if (mActivity.getString(R.string.button_delete).equals(btnText)) {
      Log.logD(TAG, "delete clicked");
      MainRepo.INST(App.INST()).removeLabel(mLabelEntity);
    }
  }

  /**
   * Click on delete button
   * @param context     A context
   * @param labelEntity The Label
   */
  public void onDeleteClick(Context context, LabelEntity labelEntity) {
    Analytics.INST(context).imageClick(TAG, "deleteLabel");
    mLabelEntity = labelEntity;
    showConfirmationDialog(context, R.string.label_delete_dialog_title,
      R.string.label_delete_dialog_message, R.string.button_delete);
  }

  /**
   * Listen for FocusChange events on the Label name
   * @param vm The ViewModel
   * @return The listener
   */
  public View.OnFocusChangeListener OnFocusChangeListener(LabelViewModel vm) {
    return (view, isFocused) -> {
      if (isFocused) {
        return;
      }
      updateName(vm);
    };
  }

  private void updateName(@NonNull LabelViewModel vm) {
    String name = vm.getName().getValue();
    if (!TextUtils.isEmpty(name)) {
      name = name.trim();
      if (!TextUtils.equals(name, vm.originalName)) {
        // update label
        vm.changeName(name, vm.originalName);
      } else {
        vm.resetName();
      }
    } else {
      vm.resetName();
    }
  }
}
