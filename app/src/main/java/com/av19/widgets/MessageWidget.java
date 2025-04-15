package com.av19.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.av19.R;
import com.av19.models.Message;
import com.av19.models.MessageQueue;
import com.av19.ui.SplashActivity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageWidget extends AppWidgetProvider {
    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        List<Message> messages = MessageQueue.getInstance(context).getAllMessages();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

        if (messages.isEmpty()) {
            // Mostrar estado vacío
            views.setViewVisibility(R.id.empty_state, View.VISIBLE);
            views.setViewVisibility(R.id.messages_container, View.GONE);
        } else {
            // Ocultar estado vacío
            views.setViewVisibility(R.id.empty_state, View.GONE);
            views.setViewVisibility(R.id.messages_container, View.VISIBLE);

            for (int i = 0; i < 5; i++) {
                int senderTimeId = getIdentifier(context, "sender_time_", i);
                int messageBodyId = getIdentifier(context, "message_body_", i);
                int messageItemId = getIdentifier(context, "message_item_", i);

                if (i < messages.size()) {
                    Message msg = messages.get(i);
                    String time = TIME_FORMAT.format(msg.getSentAt());
                    String senderTime = msg.getSender() + " • " + time;

                    views.setTextViewText(senderTimeId, senderTime);
                    views.setTextViewText(messageBodyId, msg.getMessage());
                    views.setViewVisibility(messageItemId, View.VISIBLE);
                } else {
                    views.setViewVisibility(messageItemId, View.GONE);
                }
            }
        }

        Intent launchIntent = new Intent(context, SplashActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

        // Actualizar widgets
        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

    private int getIdentifier(Context context, String prefix, int index) {
        return context.getResources().getIdentifier(
                prefix + index,
                "id",
                context.getPackageName()
        );
    }
}