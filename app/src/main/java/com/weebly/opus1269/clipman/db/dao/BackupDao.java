/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import com.weebly.opus1269.clipman.db.entity.Backup;

import java.util.List;

/** Database access for backups table */
@Dao
public interface BackupDao extends BaseDao<Backup> {
  @Query("SELECT * FROM backups ORDER BY isMine DESC, date DESC")
  LiveData<List<Backup>> getAll();

  @Query("DELETE FROM backups")
  int deleteAll();

  @Query("DELETE FROM backups WHERE drive_id_invariant = :driveIdInvariant")
  int delete(String driveIdInvariant);
}
