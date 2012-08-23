/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.tomahawk_android;

import java.io.IOException;

import org.acra.annotation.ReportsCrashes;
import org.tomahawk.libtomahawk.LocalCollection;
import org.tomahawk.libtomahawk.Source;
import org.tomahawk.libtomahawk.SourceList;
import org.tomahawk.libtomahawk.audio.PlaybackService;
import org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceBinder;
import org.tomahawk.libtomahawk.network.TomahawkServerConnection;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;


/**
 * This class contains represents the Application core.
 */
@ReportsCrashes(formKey = "")
public class TomahawkApp extends Application implements AccountManagerCallback<Bundle> {

    private static final String TAG = TomahawkApp.class.getName();
    private static Context sApplicationContext;

    private TomahawkServerConnection mTomahawkServerConnection;
    private PlaybackService mPlaybackService;
    private AccountManager mAccountManager;
    private SourceList mSourceList;

    /** Allow communication to the PlaybackService. */
    private ServiceConnection mPlaybackServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            PlaybackServiceBinder binder = (PlaybackServiceBinder) service;
            mPlaybackService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mPlaybackService = null;
        }
    };

    /**
     * Called when this Application is created.
     */
    @Override
    public void onCreate() {
//        TomahawkExceptionReporter.init(this);
        super.onCreate();
        sApplicationContext = getApplicationContext();

        mSourceList = new SourceList();

        initialize();

        Intent playbackIntent = new Intent(this, PlaybackService.class);
        bindService(playbackIntent, mPlaybackServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Initialize the Tomahawk app.
     */
    public void initialize() {
        initLocalCollection();
    }

    /**
     * Initializes a new Collection of all local tracks.
     */
    public void initLocalCollection() {
        Log.d(TAG, "Initializing Local Collection.");
        Source src = new Source(new LocalCollection(), 0, "My Collection");
        mSourceList.setLocalSource(src);
    }

    /**
     * Returns the Tomahawk AccountManager;
     */
    public AccountManager getAccountManager() {
        return mAccountManager;
    }

    /**
     * Return the list of Sources for this TomahawkApp.
     * 
     * @return SourceList
     */
    public SourceList getSourceList() {
        return mSourceList;
    }

    /**
     * Returns the PlaybackService.
     */
    public PlaybackService getPlaybackService() {
        return mPlaybackService;
    }

    /**
     * Starting Unbind the application from the PlaybackService.
     */
    public void unbindService() {
        unbindService(mPlaybackServiceConnection);
    }

    /**
     * Returns the context for the application
     * 
     * @return
     */
    public static Context getContext() {
        return sApplicationContext;
    }

    /**
     * This method is called when the Authenticator has finished.
     * 
     * Ideally, we start the Tomahawk web service here.
     */
    @Override
    public void run(AccountManagerFuture<Bundle> result) {

        try {

            String token = result.getResult().getString(AccountManager.KEY_AUTHTOKEN);
            String userid = result.getResult().getString(AccountManager.KEY_ACCOUNT_NAME);
            if (token == null) {
                Intent i = new Intent(getApplicationContext(), TomahawkAccountAuthenticatorActivity.class);
                startActivity(i);
            } else {
                Log.d(TAG, "Starting Tomahawk Service: " + token);
                mTomahawkServerConnection = TomahawkServerConnection.get(userid, token);
            }

        } catch (OperationCanceledException e) {
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

