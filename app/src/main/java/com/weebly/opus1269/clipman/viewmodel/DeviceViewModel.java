/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.entity.DeviceEntity;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.repos.DeviceRepo;

/** ViewModel for a {@link Device} */
public class DeviceViewModel extends BaseRepoViewModel<DeviceRepo> {
  /** Our Device */
  @NonNull
  private final DeviceEntity device;

  public DeviceViewModel(@NonNull Application app,
                         @NonNull DeviceEntity device) {
    super(app, DeviceRepo.INST(app));

    this.device = device;
  }

  @NonNull
  public Device getDevice() {
    return device;
  }

  public void remove() {
    mRepo.remove(device);
  }
}
