package com.av19.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import com.av19.R;
import com.av19.models.Message;
import com.av19.models.MessageQueue;

import java.util.List;

public class MessageWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        List<Message> messages = new MessageQueue(context).getAllMessages();

        // Construir la vista del widget con los mensajes
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

        // Actualizar los elementos de la UI con los mensajes
        for (int i = 0; i < Math.min(messages.size(), 5); i++) {
            Message msg = messages.get(i);
            views.setTextViewText(context.getResources().getIdentifier("message_text_" + i, "id", context.getPackageName()), msg.getMessage());
        }

        // Actualizar todos los widgets
        for (int widgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }
}