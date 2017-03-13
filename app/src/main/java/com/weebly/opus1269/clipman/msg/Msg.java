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

package com.weebly.opus1269.clipman.msg;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;

/**
 * Static constants for messaging
 */
public class Msg {

    static final String WEB_CLIENT_ID =
        App.getContext().getString(R.string.default_web_client_id);

    // message types
    /** {@value} */
    public static final String ACTION = "act";
    /** {@value} */
    public static final String ACTION_PING = "ping_others";
    /** {@value} */
    public static final String ACTION_PING_RESPONSE = "respond_to_ping";
    /** {@value} */
    public static final String ACTION_DEVICE_ADDED = "add_our_device";
    /** {@value} */
    public static final String ACTION_DEVICE_REMOVED = "remove_our_device";
    /** {@value} */
    public static final String ACTION_MESSAGE = "m";

    /** Max length of fcm data message - {@value} */
    static final int MAX_MSG_LEN = 4096;

    static final String MSG_PING =
        App.getContext().getString(R.string.device_ping);
    static final String MSG_PING_RESPONSE =
        App.getContext().getString(R.string.device_ping_response);
    static final String MSG_DEVICE_ADDED =
        App.getContext().getString(R.string.device_added);
    static final String MSG_DEVICE_REMOVED =
        App.getContext().getString(R.string.device_removed);

    /** {@value} */
    public static final String MESSAGE = "message";
    /** {@value} */
    public static final String FAV = "fav";
    /** {@value} */
    public static final String DEVICE_MODEL = "dM";
    /** {@value} */
    public static final String DEVICE_SN = "dSN";
    /** {@value} */
    public static final String DEVICE_OS = "dOS";
    /** {@value} */
    public static final String DEVICE_NICKNAME = "dN";

    // Log messages

    /** {@value} */
    public static final String FCM_RECEIVED = "Message received";
    /** {@value} */
    public static final String FCM_SENT = "Upstream message sent";
    /** {@value} */
    public static final String FCM_DELETED = "Messages deleted";
    /** {@value} */
    public static final String FCM_SEND_ERROR = "Error sending upstream message ";

    /** {@value} */
    public static final String LOG_ACTION = "Action: ";
    /** {@value} */
    public static final String LOG_DEVICE_MODEL = "Device model: ";
    /** {@value} */
    public static final String LOG_DEVICE_SN = "Device SN: ";
    /** {@value} */
    public static final String LOG_DEVICE_OS = "Device OS: ";
    /** {@value} */
    public static final String LOG_DEVICE_NICKNAME = "Device Nickname: ";

    static final String ERROR_REGISTER = App.getContext().getString(R.string.err_register);
    static final String ERROR_UNREGISTER = App.getContext().getString(R.string.err_unregister);
    static final String ERROR_REFRESH = App.getContext().getString(R.string.err_refresh);
    static final String ERROR_DEVICES = App.getContext().getString(R.string.err_devices);
    static final String ERROR_CHANGE_DEVICE_NAME = App.getContext().getString(R.string.err_change_device_name);
    static final String ERROR_SEND = App.getContext().getString(R.string.err_send);
    static final String ERROR_UNKNOWN = App.getContext().getString(R.string.err_unknown);
    static final String ERROR_INVALID_REGID = App.getContext().getString(R.string.err_invalid_regid);
    static final String ERROR_NOT_REGISTERED = App.getContext().getString(R.string.err_not_registered);
    static final String ERROR_CREDENTIAL = App.getContext().getString(R.string.err_credential);

    private Msg() {
    }
}
