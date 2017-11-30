/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.ClipTable;
import com.weebly.opus1269.clipman.db.LabelTables;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.LastError;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.msg.MessagingClient;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.clipviewer.ClipViewerActivity;
import com.weebly.opus1269.clipman.ui.clipviewer.ClipViewerFragment;
import com.weebly.opus1269.clipman.ui.devices.DevicesActivity;
import com.weebly.opus1269.clipman.ui.errorviewer.ErrorViewerActivity;
import com.weebly.opus1269.clipman.ui.help.HelpActivity;
import com.weebly.opus1269.clipman.ui.helpers.MenuTintHelper;
import com.weebly.opus1269.clipman.model.Notifications;
import com.weebly.opus1269.clipman.ui.labels.LabelsEditActivity;
import com.weebly.opus1269.clipman.ui.labels.LabelsSelectActivity;
import com.weebly.opus1269.clipman.ui.settings.SettingsActivity;
import com.weebly.opus1269.clipman.ui.signin.SignInActivity;

import java.util.List;

/** Top level Activity for the app */
public class MainActivity extends BaseActivity implements
  NavigationView.OnNavigationItemSelectedListener,
  View.OnLayoutChangeListener,
  ClipViewerFragment.OnClipChanged,
  DeleteDialogFragment.DeleteDialogListener,
  SortTypeDialogFragment.SortTypeDialogListener,
  SharedPreferences.OnSharedPreferenceChangeListener {

  /** The selected position in the list, delegated to ClipCursorAdapter */
  private static final String STATE_POS = "pos";

  /** The database _ID of the selected item, delegated to ClipCursorAdapter */
  private static final String STATE_ITEM_ID = "item_id";

  /** Delegate for RecyclerView */
  private ClipLoaderManager mLoaderManager;

  /** Items from last delete operation */
  private ClipItem[] mUndoItems = null;

  /** AppBar setting for pin favs */
  private Boolean mPinFav = false;

  /** AppBar setting for fav filter */
  private Boolean mFavFilter = false;

  /** Label filter */
  private String mLabelFilter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_main;

    // We are the big dog, no need for this
    mHomeUpEnabled = false;

    super.onCreate(savedInstanceState);

    final Intent intent = getIntent();
    // peak at intent to see if we need to show a clip notification
    if (intent.hasExtra(Intents.EXTRA_CLIP_ITEM)) {
      final int msgCt = intent.getIntExtra(Intents.EXTRA_CLIP_COUNT, 0);
      if (msgCt > 1) {
        // we will show them, reset LabelFilter
        Prefs.INST(this).setLabelFilter("");
      }
    }

    mPinFav = Prefs.INST(this).isPinFav();
    mFavFilter = Prefs.INST(this).isFavFilter();
    mLabelFilter = Prefs.INST(this).getLabelFilter();

    // listen for preference changes
    PreferenceManager
      .getDefaultSharedPreferences(this)
      .registerOnSharedPreferenceChangeListener(this);

    // handles the adapter and RecyclerView
    mLoaderManager = new ClipLoaderManager(this);

    // Check whether we're recreating a previously destroyed instance
    if (savedInstanceState != null) {
      final int pos = savedInstanceState.getInt(STATE_POS);
      final long id = savedInstanceState.getLong(STATE_ITEM_ID);
      mLoaderManager.getAdapter().restoreSelection(pos, id);
    }

    setupNavigationView();

    if (AppUtils.isDualPane(this)) {
      // create the clip viewer for the two pane option
      final ClipViewerFragment fragment =
        ClipViewerFragment.newInstance(new ClipItem(), "");
      getSupportFragmentManager().beginTransaction()
        .replace(R.id.clip_viewer_container, fragment)
        .commit();
    }

    setFabVisibility(false);

    final DrawerLayout drawer = findViewById(R.id.drawer_layout);
    final Toolbar toolbar = findViewById(R.id.toolbar);
    final ActionBarDrawerToggle toggle =
      new ActionBarDrawerToggle(this, drawer, toolbar,
        R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawer.addDrawerListener(toggle);
    toggle.syncState();

    final NavigationView navigationView = findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);

    handleIntent();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    final ClipCursorAdapter adapter = mLoaderManager.getAdapter();
    outState.putInt(STATE_POS, adapter.getSelectedPos());
    outState.putLong(STATE_ITEM_ID, adapter.getSelectedItemID());
  }

  @Override
  protected void onResume() {
    super.onResume();

    setTitle();

    updateNavView();

    updateOptionsMenu();

    Notifications.INST(this).removeClips();

    // so relative dates get updated
    mLoaderManager.getAdapter().notifyDataSetChanged();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    final boolean ret;

    mOptionsMenuID = R.menu.menu_main;

    ret = super.onCreateOptionsMenu(menu);

    updateOptionsMenu();

    return ret;
  }

  @Override
  protected boolean setQueryString(String queryString) {
    boolean ret = false;
    if (super.setQueryString(queryString)) {
      getSupportLoaderManager().restartLoader(0, null, mLoaderManager);
      ret = true;
    }
    return ret;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    // stop listening for preference changes
    PreferenceManager.getDefaultSharedPreferences(this)
      .unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onBackPressed() {
    if (!TextUtils.isEmpty(mLabelFilter)) {
      // if filtered, create unfiltered MainActivity on Back
      Prefs.INST(this).setLabelFilter("");
      startActivity(MainActivity.class);
      finish();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();

    mUndoItems = null;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean processed = true;

    final int id = item.getItemId();
    switch (id) {
      case R.id.home:
        NavUtils.navigateUpFromSameTask(this);
        setQueryString("");
        break;
      case R.id.action_send:
        sendClipboardContents();
        break;
      case R.id.action_pin:
        mPinFav = !mPinFav;
        Prefs.INST(this).setPinFav(mPinFav);
        updateOptionsMenu();
        // reload clips
        getSupportLoaderManager().restartLoader(0, null, mLoaderManager);
        break;
      case R.id.action_fav_filter:
        mFavFilter = !mFavFilter;
        Prefs.INST(this).setFavFilter(mFavFilter);
        updateOptionsMenu();
        // reload clips
        getSupportLoaderManager().restartLoader(0, null, mLoaderManager);
        break;
      case R.id.action_labels:
        final Intent intent = new Intent(this, LabelsSelectActivity.class);
        intent.putExtra(Intents.EXTRA_CLIP_ITEM, this.getClipItemClone());
        AppUtils.startActivity(this, intent);
        break;
      case R.id.action_delete:
        showDeleteDialog();
        break;
      case R.id.action_sort:
        showSortTypeDialog();
        break;
      case R.id.action_settings:
        startActivity(SettingsActivity.class);
        break;
      case R.id.action_help:
        startActivity(HelpActivity.class);
        break;
      default:
        processed = false;
        break;
    }

    if (processed) {
      Analytics.INST(this).menuClick(TAG, item);
    }

    return processed || super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    boolean processed = true;
    final int id = item.getItemId();

    switch (id) {
      case R.id.nav_account:
        startActivity(SignInActivity.class);
        break;
      case R.id.nav_devices:
        startActivity(DevicesActivity.class);
        break;
      case R.id.nav_settings:
        startActivity(SettingsActivity.class);
        break;
      case R.id.nav_labels_edit:
        startActivity(LabelsEditActivity.class);
        break;
      case R.id.nav_clips:
        if (!AppUtils.isWhitespace(mLabelFilter)) {
          Prefs.INST(this).setLabelFilter("");
          startActivity(MainActivity.class);
          finish();
        }
        break;
      case Menu.NONE:
        // all Labels items
        final String label = item.getTitle().toString();
        if (!label.equals(mLabelFilter)) {
          Prefs.INST(this).setLabelFilter(label);
          startActivity(MainActivity.class);
          finish();
        }
        break;
      case R.id.nav_error:
        startActivity(ErrorViewerActivity.class);
        break;
      case R.id.nav_help:
        startActivity(HelpActivity.class);
        break;
      case R.id.nav_chrome_extension:
        AppUtils.showWebUrl(this, getString(R.string.chrome_extension_url));
        break;
      case R.id.rate_app:
        AppUtils.showInPlayStore(this);
        break;
      default:
        processed = false;
        break;
    }

    if (processed) {
      Analytics.INST(this).menuClick(TAG, item);
    }

    final DrawerLayout drawer = findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);

    return true;
  }

  /** Set NavigationView header aspect ratio to 16:9 */
  @Override
  public void onLayoutChange(View v, int left, int top, int right, int bottom,
                             int oldLeft, int oldTop, int oldRight,
                             int oldBottom) {
    final NavigationView navigationView = findViewById(R.id.nav_view);

    if (v.equals(navigationView)) {
      final int oldWidth = oldRight - oldLeft;
      final int width = right - left;
      final View hView = navigationView.getHeaderView(0);
      if ((hView != null) && (oldWidth != width)) {
        hView.getLayoutParams().height = Math.round((9.0F / 16.0F) * width);
      }
    }
  }

  @Override
  public void clipChanged(ClipItem clipItem) {
    setFabVisibility(!ClipItem.isWhitespace(clipItem));
    setTitle();
  }

  @Override
  public void onDeleteDialogPositiveClick(Boolean deleteFavs) {
    // save items for undo
    mUndoItems = ClipTable.INST(this).getAll(deleteFavs, mLabelFilter);

    final int nRows = ClipTable.INST(this).deleteAll(deleteFavs, mLabelFilter);

    String message = nRows + getResources().getString(R.string.items_deleted);
    switch (nRows) {
      case 0:
        message = getResources().getString(R.string.item_delete_empty);
        break;
      case 1:
        message = getResources().getString(R.string.item_deleted_one);
        break;
      default:
        break;
    }
    final Snackbar snack =
      Snackbar.make(findViewById(R.id.fab), message, Snackbar.LENGTH_LONG);
    if (nRows > 0) {
      snack.setAction(R.string.button_undo, new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          final Context ctxt = v.getContext();
          Analytics.INST(ctxt)
            .imageClick(getTAG(), getString(R.string.button_undo));
          ClipTable.INST(ctxt).insertClipItems(mUndoItems);
        }
      }).addCallback(new Snackbar.Callback() {

        @Override
        public void onShown(Snackbar snackbar) {
        }

        @Override
        public void onDismissed(Snackbar snackbar, int event) {
          mUndoItems = null;
        }
      });
    }
    snack.show();
  }

  @Override
  public void onSortTypeSelected() {
    getSupportLoaderManager().restartLoader(0, null, mLoaderManager);
  }

  @Override
  public void
  onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals(LastError.PREF_LAST_ERROR)) {
      updateNavView();
    }
  }

  Boolean getFavFilter() {return mFavFilter;}

  String getLabelFilter() {return mLabelFilter;}

  String getQueryString() {return mQueryString;}

  ClipLoaderManager getClipLoaderManager() {return mLoaderManager;}

  /**
   * Start the {@link ClipViewerActivity}
   * or update the {@link ClipViewerFragment}
   * @param clipItem item to display
   */
  void startOrUpdateClipViewer(ClipItem clipItem) {
    if (AppUtils.isDualPane(this)) {
      final ClipViewerFragment fragment = getClipViewerFragment();
      if (fragment != null) {
        fragment.setClipItem(clipItem);
        fragment.setHighlightText(mQueryString);
      }
    } else {
      final Intent intent = new Intent(this, ClipViewerActivity.class);
      intent.putExtra(Intents.EXTRA_CLIP_ITEM, clipItem);
      intent.putExtra(Intents.EXTRA_TEXT, mQueryString);
      AppUtils.startActivity(this, intent);
    }
  }

  /** Process intents we know about */
  private void handleIntent() {
    final Intent intent = getIntent();
    final String action = intent.getAction();
    final String type = intent.getType();

    if (Intent.ACTION_SEND.equals(action) && (type != null)) {
      // Share from other app
      if (ClipItem.TEXT_PLAIN.equals(type)) {
        final String sharedText =
          intent.getStringExtra(Intent.EXTRA_TEXT);
        if (!TextUtils.isEmpty(sharedText)) {
          final ClipItem item = new ClipItem(sharedText);
          item.save();
          startOrUpdateClipViewer(item);
          MessagingClient.send(item);
        }
      }
    } else if (intent.hasExtra(Intents.EXTRA_CLIP_ITEM)) {
      // from clip notification
      final int msgCt = intent.getIntExtra(Intents.EXTRA_CLIP_COUNT, 0);
      if (msgCt == 1) {
        // if 1 message open Clipviewer, otherwise show in us
        final ClipItem item =
          (ClipItem) intent.getSerializableExtra(Intents.EXTRA_CLIP_ITEM);
        intent.removeExtra(Intents.EXTRA_CLIP_ITEM);
        startOrUpdateClipViewer(item);
      }
    }
  }

  /**
   * Start an {@link Activity}
   * @param cls Class of Activity
   */
  private void startActivity(Class cls) {
    final Intent intent = new Intent(this, cls);
    AppUtils.startActivity(this, intent);
  }

  /** Send the clipboard contents to our {@link Devices} */
  private void sendClipboardContents() {
    ClipItem.sendClipboardContents(findViewById(R.id.fab));
  }

  /** Show the {@link DeleteDialogFragment} for verifying delete all */
  private void showDeleteDialog() {
    final DialogFragment dialog = new DeleteDialogFragment();
    dialog.show(getSupportFragmentManager(), "DeleteDialogFragment");
  }

  /** Show the {@link SortTypeDialogFragment} for selecting list sort type */
  private void showSortTypeDialog() {
    final DialogFragment dialog = new SortTypeDialogFragment();
    dialog.show(getSupportFragmentManager(), "SortTypeDialogFragment");
  }

  /** Initialize the NavigationView */
  private void setupNavigationView() {
    final NavigationView navigationView = findViewById(R.id.nav_view);
    if (navigationView == null) {
      return;
    }
    navigationView.addOnLayoutChangeListener(this);

    // Handle click on header
    final View hView = navigationView.getHeaderView(0);
    hView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null) {
          drawer.closeDrawer(GravityCompat.START);
        }
        startActivity(SignInActivity.class);
      }
    });
  }

  /** Set title based on currently selected {@link ClipItem} */
  private void setTitle() {
    String prefix = getString(R.string.title_activity_main);
    if (!AppUtils.isWhitespace(mLabelFilter)) {
      prefix = mLabelFilter;
    }
    if (AppUtils.isDualPane(this)) {
      final ClipItem clipItem = getClipItemClone();
      if (clipItem.isRemote()) {
        setTitle(getString(R.string.title_activity_main_remote_fmt, prefix,
          clipItem.getDevice()));
      } else {
        setTitle(getString(R.string.title_activity_main_local_fmt, prefix));
      }
    } else {
      setTitle(prefix);
    }
  }

  /** Get our {@link ClipViewerFragment} */
  private ClipViewerFragment getClipViewerFragment() {
    return (ClipViewerFragment) getSupportFragmentManager()
      .findFragmentById(R.id.clip_viewer_container);
  }

  /** Get copy of currently selected {@link ClipItem} */
  private ClipItem getClipItemClone() {
    return getClipViewerFragment().getClipItemClone();
  }

  /** Update the Navigation View */
  private void updateNavView() {
    final NavigationView navigationView = findViewById(R.id.nav_view);
    if (navigationView == null) {
      return;
    }
    final View hView = navigationView.getHeaderView(0);
    if (hView == null) {
      return;
    }

    final Menu menu = navigationView.getMenu();

    // set Devices menu state
    MenuItem menuItem = menu.findItem(R.id.nav_devices);
    menuItem.setEnabled(User.INST(this).isLoggedIn());

    // set Error Viewer menu state
    menuItem = menu.findItem(R.id.nav_error);
    menuItem.setEnabled(LastError.exists(this));

    // Create Labels sub menu
    List<Label> labels = LabelTables.INST(this).getLabels();
    menu.setGroupVisible(R.id.nav_group_labels, (labels.size() > 0));
    SubMenu labelMenu = menu.findItem(R.id.nav_labels_sub_menu).getSubMenu();
    labelMenu.clear();
    for (Label label : labels) {
      final MenuItem labelItem = labelMenu.add(R.id.nav_group_labels,
        Menu.NONE, Menu.NONE, label.getName());
      labelItem.setIcon(R.drawable.ic_label);
    }

    User.INST(this).setNavigationHeaderView(hView);
  }

  /** Set Option Menu icons enabled state */
  private void updateOptionsMenu() {
    if (mOptionsMenu != null) {

      // Hide labels menu if not dual pane
      if (!AppUtils.isDualPane(this)) {
        MenuItem labelItem = mOptionsMenu.findItem(R.id.action_labels);
        labelItem.setVisible(false);
      }

      // enabled state of send button
      Boolean enabled = false;
      Integer alpha = 64;
      if (User.INST(this).isLoggedIn() && Prefs.INST(this).isPushClipboard()) {
        enabled = true;
        alpha = 255;
      }
      final MenuItem sendItem = mOptionsMenu.findItem(R.id.action_send);
      MenuTintHelper.colorMenuItem(sendItem, null, alpha);
      sendItem.setEnabled(enabled);

      // pin fav state
      final MenuItem pinMenu =
        mOptionsMenu.findItem(R.id.action_pin);
      if (mPinFav) {
        pinMenu.setIcon(R.drawable.ic_pin);
        pinMenu.setTitle(R.string.action_no_pin);
      } else {
        pinMenu.setIcon(R.drawable.ic_no_pin);
        pinMenu.setTitle(R.string.action_pin);
      }
      final int pinColor = ContextCompat.getColor(this, R.color.icons);
      MenuTintHelper.colorMenuItem(pinMenu, pinColor, 255);

      // fav filter state
      final MenuItem favFilterMenu =
        mOptionsMenu.findItem(R.id.action_fav_filter);
      int colorID;
      if (mFavFilter) {
        favFilterMenu.setIcon(R.drawable.ic_favorite_black_24dp);
        favFilterMenu.setTitle(R.string.action_show_all);
        colorID = R.color.red_500_translucent;
      } else {
        favFilterMenu.setIcon(R.drawable.ic_favorite_border_black_24dp);
        favFilterMenu.setTitle(R.string.action_show_favs);
        colorID = R.color.icons;
      }
      final int favFilterColor = ContextCompat.getColor(this, colorID);
      MenuTintHelper.colorMenuItem(favFilterMenu, favFilterColor, 255);
    }
  }
}
