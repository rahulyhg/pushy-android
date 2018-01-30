/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.repos.MainRepo;

/** ViewModel for creating a {@link LabelEntity} */
public class LabelCreateViewModel extends BaseRepoViewModel<MainRepo> {
  /** Name of Label */
  public final MutableLiveData<String> name;

  public LabelCreateViewModel(@NonNull Application app) {
    super(app, MainRepo.INST(app));

    name = new MutableLiveData<>();
    name.setValue("");
  }

  public void create() {
    String name = this.name.getValue();
    if (!AppUtils.isWhitespace(name)) {
      name = name.trim();
      this.name.setValue(name);
      mRepo.addIfNewAsync(new LabelEntity(name));
    }
  }
}