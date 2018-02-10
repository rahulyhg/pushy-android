/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.db.entity.Label;

import java.util.ArrayList;
import java.util.List;

/** Generates data to pre-populate the database */
public class MainDBInitializer {
  private static final String[] TEXT = new String[]{
    App.INST().getString(R.string.default_clip_3),
    App.INST().getString(R.string.default_clip_2),
    App.INST().getString(R.string.default_clip_1)
  };

  private static final Boolean[] FAV = new Boolean[]{true, false, true};

  public static List<Clip> getClips() {
    List<Clip> clips = new ArrayList<>(TEXT.length);
    for (int i = 0; i < TEXT.length; i++) {
      final Clip clip = new Clip();
      clip.setText(TEXT[i]);
      clip.setFav(FAV[i]);
      // so dates aren't all the same
      clip.setDate(clip.getDate() + i + 1);
      clips.add(clip);
    }
    return clips;
  }

  public static Clip getLabeledClip() {
      final Clip clip = new Clip();
      clip.setText(App.INST().getString(R.string.default_clip_4));
      clip.setFav(true);
      clip.setDate(clip.getDate());
    return clip;
  }

  public static Label getLabel() {
    return new Label(App.INST().getString(R.string.default_label_name));
  }

}
