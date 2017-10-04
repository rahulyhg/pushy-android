/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Label;

import java.util.ArrayList;
import java.util.List;

/** Singleton to manage the Clips.db Label and LabelMap tables */
public enum LabelTables {
  INST;

  /**
   * Get the List of {@link Label} objects
   * @return List of Labels
   */
  public List<Label> getLabels() {
    final ArrayList<Label> list = new ArrayList<>(0);
    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    final String[] projection = {" * "};

    // query for all
    final Cursor cursor = resolver.query(ClipsContract.Label.CONTENT_URI,
      projection, null, null, null);
    if ((cursor == null) || (cursor.getCount() <= 0)) {
      return list;
    }

    try {
      while (cursor.moveToNext()) {
        final int idx = cursor.getColumnIndex(ClipsContract.Label.COL_NAME);
        list.add(new Label(cursor.getString(idx)));
      }
    } finally {
      cursor.close();
    }

    return list;
  }

  /**
   * Add the {@link Label} map for a group of {@link ClipItem} objects to the
   * database
   * @param clipItems the items to add Labels for
   * @return number of items added
   */
  public int insertLabelsMap(ClipItem[] clipItems) {
    if (clipItems == null) {
      return 0;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    // get total number of ClipItem/Label entrie
    int size = 0;
    for (ClipItem clipItem : clipItems) {
      size += clipItem.getLabels().size();
    }

    final ContentValues[] mapCVs = new ContentValues[size];
    int count = 0;
    for (ClipItem clipItem : clipItems) {
      for (Label label : clipItem.getLabels()) {
        ContentValues cv = new ContentValues();
        cv.put(ClipsContract.LabelMap.COL_CLIP_ID, clipItem.getId());
        cv.put(ClipsContract.LabelMap.COL_LABEL_NAME, label.getName());
        mapCVs[count] = cv;
        count++;
      }
    }

    return resolver.bulkInsert(ClipsContract.LabelMap.CONTENT_URI, mapCVs);
  }

  /**
   * Add a {@link ClipItem} and {@link Label} to the LabelMap table
   * @param clipItem the clip
   * @param label    the label
   * @return if true, added
   */
  public boolean insert(ClipItem clipItem, Label label) {
    if (AppUtils.isWhitespace(clipItem.getText()) ||
      AppUtils.isWhitespace(label.getName())) {
      return false;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    if (exists(resolver, clipItem, label)) {
      // already in db
      return false;
    }

    // insert Label
    label.save();

    // insert into LabelMap table
    final ContentValues cv = new ContentValues();
    cv.put(ClipsContract.LabelMap.COL_CLIP_ID, clipItem.getId());
    cv.put(ClipsContract.LabelMap.COL_LABEL_NAME, label.getName());

    resolver.insert(ClipsContract.LabelMap.CONTENT_URI, cv);

    return true;
  }

  /**
   * Delete a {@link ClipItem} and {@link Label} from the LabelMap table
   * @param clipItem the clip
   * @param label    the label
   */
  public void delete(ClipItem clipItem, Label label) {
    if (ClipItem.isWhitespace(clipItem) ||
      AppUtils.isWhitespace(label.getName())) {
      return;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    final long id = clipItem.getId();
    final String selection =
      ClipsContract.LabelMap.COL_LABEL_NAME + " = ? AND " +
      ClipsContract.LabelMap.COL_CLIP_ID + " = " + id;
    final String[] selectionArgs = {label.getName()};

    resolver.delete(ClipsContract.LabelMap.CONTENT_URI, selection,
      selectionArgs);
  }

  /**
   * Does the ClipItem and Label exist in the LabelMap table
   * @param resolver to db
   * @param label    Label to check
   * @return if true, already in db
   */
  private boolean exists(ContentResolver resolver, ClipItem clipItem,
                         Label label) {
    final String[] projection = {ClipsContract.LabelMap.COL_LABEL_NAME};
    final long id = clipItem.getId();
    final String selection =
      ClipsContract.LabelMap.COL_LABEL_NAME + " = ? AND " +
      ClipsContract.LabelMap.COL_CLIP_ID + " = " + id;
    final String[] selectionArgs = {label.getName()};

    final Cursor cursor = resolver.query(ClipsContract.LabelMap.CONTENT_URI,
      projection, selection, selectionArgs, null);
    if ((cursor != null) && (cursor.getCount() > 0)) {
      // found it
      cursor.close();
      return true;
    }
    return false;
  }
}
