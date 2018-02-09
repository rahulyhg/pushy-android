/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.entity.Backup;
import com.weebly.opus1269.clipman.repos.BackupRepo;

import java.util.List;

/** ViewModel for BackupEntitys */
public class BackupsViewModel extends BaseRepoViewModel<BackupRepo> {
  /** BackupFile list */
  private final MediatorLiveData<List<Backup>> backups;

  public BackupsViewModel(@NonNull Application app) {
    super(app, BackupRepo.INST(app));

    backups = new MediatorLiveData<>();
    backups.setValue(null);
    this.backups.addSource(mRepo.getBackups(), this.backups::setValue);
  }

  public LiveData<List<Backup>> getBackups() {
    return backups;
  }
}
