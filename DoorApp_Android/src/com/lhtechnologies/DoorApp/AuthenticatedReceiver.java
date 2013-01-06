/*
 * Copyright © 2012 Ludger Heide ludger.heide@gmail.com
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details..
 */

package com.lhtechnologies.DoorApp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import static com.lhtechnologies.DoorApp.CommonStuff.*;

public class AuthenticatedReceiver extends BroadcastReceiver {

    private Timer timer;
    private NotificationManager mNotificationManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(this.getClass().getSimpleName(), "Received Broadcast!");
        if (intent.getAction().equals(AuthenticationFinishedBroadCast)) {
            String reason;
            mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            //Start the notification
            NotificationCompat.Builder notiBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(context.getString(R.string.app_name));

            if (intent.hasExtra(AuthenticatorReturnCode)) {
                String returnCode = intent.getStringExtra(AuthenticatorReturnCode);
                if (returnCode.equals(ServerReturnSuccess)) {
                    if (intent.hasExtra(FlatDoor)) {
                        notiBuilder.setContentText(context.getString(R.string.StatusBuzzing))
                                .setOngoing(true)
                                .setDefaults(Notification.FLAG_SHOW_LIGHTS)
                                .setLights(0xFF00FF00, 1000, 1000);

                        mNotificationManager.notify(BuzzingNotificationId, notiBuilder.build());

                        class RemoveBuzzingNotification extends TimerTask {
                            public void run() {
                                mNotificationManager.cancel(BuzzingNotificationId);
                                timer.cancel(); //Terminate the timer thread
                            }
                        }

                        timer = new Timer();
                        timer.schedule(new RemoveBuzzingNotification(), buzzerTimeout * 1000);
                    } else {
                        notiBuilder.setContentText(context.getString(R.string.StatusAuthenticated))
                                .setOngoing(true)
                                .setDefaults(Notification.FLAG_SHOW_LIGHTS)
                                .setLights(0xFF00FF00, 1000, 1000);

                        mNotificationManager.notify(SuccessNotificationId, notiBuilder.build());

                        class RemoveSuccessNotification extends TimerTask {
                            public void run() {
                                mNotificationManager.cancel(SuccessNotificationId);
                                timer.cancel(); //Terminate the timer thread
                            }
                        }

                        timer = new Timer();
                        timer.schedule(new RemoveSuccessNotification(), timeout * 1000);
                    }
                } else {
                    String result = intent.getStringExtra(AuthenticatorReturnCode);
                    if (result.equals(ServerReturnAuthFailure))
                        reason = context.getString(R.string.AuthFailureExplanantion);
                    else if (result.equals(ServerReturnInternalFailure))
                        reason = context.getString(R.string.InternalFailureExplanantion);
                    else if (result.equals(ServerReturnRegistrationStarted))
                        reason = context.getString(R.string.RegistrationStartedExplanantion);
                    else if (result.equals(ServerReturnRegistrationPending))
                        reason = context.getString(R.string.RegistrationPendingExplanantion);
                    else if (result.equals(ClientErrorUndefined))
                        reason = context.getString(R.string.ClientErrorExplanantion) + "\n" + intent.getStringExtra(AuthenticatorErrorDescription);
                    else if (result.equals(ClientErrorMalformedURL))
                        reason = context.getString(R.string.MalformedURLExplanation);
                    else
                        reason = context.getString(R.string.UnknownErrorExplanation);

                    notiBuilder.setContentText(reason)
                            .setOngoing(false)
                            .setDefaults(Notification.FLAG_SHOW_LIGHTS | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                            .setLights(0xFFFF0000, 500, 500);

                    // Creates an explicit intent for an Activity in your app
                    Intent resultIntent = new Intent(context, MainActivity.class);

                    // The stack builder object will contain an artificial back stack for the
                    // started Activity.
                    // This ensures that navigating backward from the Activity leads out of
                    // your application to the Home screen.
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    // Adds the back stack for the Intent (but not the Intent itself)
                    stackBuilder.addParentStack(MainActivity.class);
                    // Adds the Intent that starts the Activity to the top of the stack
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    notiBuilder.setContentIntent(resultPendingIntent);

                    mNotificationManager.notify(FailureNotificationId, notiBuilder.build());
                }
            } else {
                reason = context.getString(R.string.AuthFailureExplanantion) + "\n" + "NO_EXTRA";

                notiBuilder.setContentText(reason)
                        .setOngoing(false)
                        .setDefaults(Notification.FLAG_SHOW_LIGHTS | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                        .setLights(0xFFFF0000, 500, 500);

                // Creates an explicit intent for an Activity in your app
                Intent resultIntent = new Intent(context, MainActivity.class);

                // The stack builder object will contain an artificial back stack for the
                // started Activity.
                // This ensures that navigating backward from the Activity leads out of
                // your application to the Home screen.
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                // Adds the back stack for the Intent (but not the Intent itself)
                stackBuilder.addParentStack(MainActivity.class);
                // Adds the Intent that starts the Activity to the top of the stack
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
                notiBuilder.setContentIntent(resultPendingIntent);

                mNotificationManager.notify(FailureNotificationId, notiBuilder.build());
            }
            abortBroadcast();
        }
    }
}
