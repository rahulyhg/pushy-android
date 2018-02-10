/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.repos.MainRepo;

import java.util.List;

/** ViewModel for Labels */
public class LabelsViewModel extends BaseRepoViewModel<MainRepo> {
  /** Labels list */
  private final MediatorLiveData<List<Label>> labels;

  public LabelsViewModel(@NonNull Application app) {
    super(app, MainRepo.INST(app));

    labels = new MediatorLiveData<>();
    labels.setValue(mRepo.getLabels().getValue());
    labels.addSource(mRepo.getLabels(), labels::setValue);
  }

  @Override
  protected void initRepo() {
    super.initRepo();
    mRepo.setErrorMsg(null);
  }

  public MutableLiveData<List<Label>> getLabels() {
    return labels;
  }
}
