package com.vpedak.testsrecorder.core;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.vpedak.testsrecorder.core.events.ClickAction;
import com.vpedak.testsrecorder.core.events.LongClickAction;
import com.vpedak.testsrecorder.core.events.RecordingEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ActivityProcessor {
    private long uniqueId;
    public static final String ANDRIOD_TEST_RECORDER = "Andriod Test Recorder";
    private List<View> allViews = new ArrayList<View>();
    private EventWriter eventWriter;
    private Activity activity;
    private MenuProcessor menuProcessor;

    public ActivityProcessor(long uniqueId) {
        this.uniqueId = uniqueId;
        eventWriter = new EventWriter(uniqueId);
        menuProcessor = new MenuProcessor(this);
    }

    public EventWriter getEventWriter() {
        return eventWriter;
    }

    public void processActivity(Activity activity) {
        this.activity = activity;

        menuProcessor.processActivity(activity);

        View view = activity.getWindow().getDecorView();
        processView(view);
    }

    private void processViews(View[] views) {
        for (int i = 0; i < views.length; i++) {
            processView(views[i]);
        }
    }

    private void processView(View view) {
        if (allViews.contains(view)) {
            return;
        }

        menuProcessor.processView(view);

        processClick(view);

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;

            /*ViewGroup.OnHierarchyChangeListener li = null;
            try {
                Field f = ViewGroup.class.getDeclaredField("mOnHierarchyChangeListener");
                f.setAccessible(true);
                li = (ViewGroup.OnHierarchyChangeListener) f.get(viewGroup);
            } catch (IllegalAccessException e) {
                Log.e("TEST", "IllegalAccessException", e);
            } catch (NoSuchFieldException e) {
                Log.e("TEST", "NoSuchFieldException", e);
            }

            final ViewGroup.OnHierarchyChangeListener finalLi = li;
            viewGroup.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, View child) {
                    if (finalLi != null) {
                        finalLi.onChildViewAdded(parent, child);
                    }

                    Log.wmFieldName("TEST", "New View added - " + child.toString());
                    processView(child);
                }

                @Override
                public void onChildViewRemoved(View parent, View child) {
                    if (finalLi != null) {
                        finalLi.onChildViewRemoved(parent, child);
                    }
                }
            });
            Log.wmFieldName("TEST", "HierarchyChangeListener added for view - " + viewGroup.toString());*/

            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);

                processView(child);
            }
        }

        allViews.add(view);
    }

    private void processClick(final View view) {
        View.OnClickListener listener = null;
        View.OnLongClickListener longListener = null;
        try {
            Field f = View.class.getDeclaredField("mListenerInfo");
            f.setAccessible(true);
            Object li = f.get(view);

            if (li != null) {
                Field f2 = li.getClass().getDeclaredField("mOnClickListener");
                f2.setAccessible(true);
                listener = (View.OnClickListener) f2.get(li);

                Field f3 = li.getClass().getDeclaredField("mOnLongClickListener");
                f3.setAccessible(true);
                longListener = (View.OnLongClickListener) f3.get(li);
            }
        } catch (IllegalAccessException e) {
            Log.e(ANDRIOD_TEST_RECORDER, "IllegalAccessException", e);
        } catch (NoSuchFieldException e) {
            Log.e(ANDRIOD_TEST_RECORDER, "NoSuchFieldException", e);
        }

        if (view.isClickable() && listener != null) {
            final View.OnClickListener finalListener = listener;
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String viewId = resolveId(view.getId());
                    if (viewId != null) {
                        String descr;
                        if (view instanceof Button) {
                            descr = "Click at button with id " + viewId;
                        } else {
                            descr = "Click at view with id " + viewId;
                        }
                        eventWriter.writeEvent(new RecordingEvent(new com.vpedak.testsrecorder.core.events.View(viewId), new ClickAction(), descr));

                        finalListener.onClick(v);
                        processViews(getWMViews());
                    }
                }
            });
        }

        if (view.isLongClickable()) {
            final View.OnLongClickListener finalLongListener = longListener;
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    String viewId = resolveId(view.getId());
                    if (viewId != null) {
                        String descr;
                        if (view instanceof Button) {
                            descr = "Long click at button with id " + viewId;
                        } else {
                            descr = "Long click at view with id " + viewId;
                        }
                        eventWriter.writeEvent(new RecordingEvent(new com.vpedak.testsrecorder.core.events.View(viewId), new LongClickAction(), descr));

                        if (finalLongListener != null) {
                            return finalLongListener.onLongClick(v);
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            });
        }
    }

    @Nullable
    public String resolveId(int id) {
        if (id < 0) {
            return null;
        }

        try {
            String viewId = activity.getResources().getResourceEntryName(id);
            String pkg = activity.getResources().getResourcePackageName(id);
            String type = activity.getResources().getResourceTypeName(id);
            if (pkg.equals("android")) {
                viewId = "android.R." + type + "." + viewId;
            } else {
                viewId = "R." + type + "." + viewId;
            }
            return viewId;
        } catch (Resources.NotFoundException e) {
            return String.valueOf(id);
        }
    }

    static String wmFieldName;
    static Class wmClass;

    static {
        try {
            if (Build.VERSION.SDK_INT >= 17) {
                wmFieldName = "sDefaultWindowManager";
                wmClass = Class.forName("android.view.WindowManagerGlobal");
            } else if (Build.VERSION.SDK_INT >= 13) {
                wmFieldName = "sWindowManager";
                wmClass = Class.forName("android.view.WindowManagerImpl");
            } else {
                wmFieldName = "mWindowManager";
                wmClass = Class.forName("android.view.WindowManagerImpl");
            }
        } catch (ClassNotFoundException localClassNotFoundException) {
            Log.e(ANDRIOD_TEST_RECORDER, "Couldn't find android.view.WindowManagerImpl - fatal!", localClassNotFoundException);
        } catch (SecurityException localSecurityException) {
            Log.e(ANDRIOD_TEST_RECORDER, "Couldn't get android.view.WindowManagerImpl", localSecurityException);
        }
    }

    private static View[] getWMViews() {
        try {
            Field localField = wmClass.getDeclaredField("mViews");
            Object localObject = wmClass.getDeclaredField(wmFieldName);
            localField.setAccessible(true);
            ((Field) localObject).setAccessible(true);
            localObject = ((Field) localObject).get(null);
            if (Build.VERSION.SDK_INT < 19) {
                return (View[]) localField.get(localObject);
            }
            return (View[]) ((ArrayList) localField.get(localObject)).toArray(new View[0]);
        } catch (SecurityException localSecurityException) {
            Log.e(ANDRIOD_TEST_RECORDER, "Couldn't get decor views!", localSecurityException);
        } catch (NoSuchFieldException localNoSuchFieldException) {
            Log.e(ANDRIOD_TEST_RECORDER, "Couldn't get decor views!", localNoSuchFieldException);
        } catch (IllegalArgumentException localIllegalArgumentException) {
            Log.e(ANDRIOD_TEST_RECORDER, "Couldn't get decor views!", localIllegalArgumentException);
        } catch (IllegalAccessException localIllegalAccessException) {
            Log.e(ANDRIOD_TEST_RECORDER, "Couldn't get decor views!", localIllegalAccessException);
        }

        return null;
    }
}