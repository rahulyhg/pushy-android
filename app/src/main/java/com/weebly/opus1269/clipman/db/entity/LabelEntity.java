/*
 *
 * Copyright 2016 Michael A Updike
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.weebly.opus1269.clipman.db.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import com.weebly.opus1269.clipman.model.AdapterItem;
import com.weebly.opus1269.clipman.model.LabelNew;

import java.io.Serializable;

/** A Label for categorizing clips */
@Entity(tableName = "labels", indices = {@Index(value = "name", unique = true)})
public class LabelEntity implements LabelNew, AdapterItem, Serializable {
  @PrimaryKey(autoGenerate = true)
  private long id;

  private String name;

  public LabelEntity(String name) {
    this.name = name;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LabelEntity label = (LabelEntity) o;

    return name.equals(label.name) && (id == label.id);
  }

  @Override
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
