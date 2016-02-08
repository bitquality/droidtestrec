package com.vpedak.testsrecorder.core;

import android.util.Log;

import com.vpedak.testsrecorder.core.events.RecordingEvent;

import java.util.ArrayList;
import java.util.List;

public class EventWriter {
    private long uniqueId;
    public static final String ANDRIOD_TEST_RECORDER = "AndriodTestRecorder";
    public String tag;
    private List<RecordingEvent> delayedEvents = new ArrayList<RecordingEvent>();
    private EventWriterListener listener;

    public EventWriter(long uniqueId, EventWriterListener listener) {
        this.uniqueId = uniqueId;
        this.listener = listener;
        tag = ANDRIOD_TEST_RECORDER+uniqueId;
    }

    public synchronized void writeEvent(RecordingEvent event) {
        if (delayedEvents.size() > 0) {
            for(RecordingEvent delayedEvent : delayedEvents) {
                Log.d(tag, delayedEvent.toString());
                listener.onEventWritten(delayedEvent);
            }
            delayedEvents.clear();
        }
        Log.d(tag, event.toString());
        listener.onEventWritten(event);
    }

    public synchronized void addDelayedEvent(RecordingEvent delayedEvent) {
        delayedEvents.add(delayedEvent);
    }

    public static interface EventWriterListener {
        void onEventWritten(RecordingEvent event);
    }
}
