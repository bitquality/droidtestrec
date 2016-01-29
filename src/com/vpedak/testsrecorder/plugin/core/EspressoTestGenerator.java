package com.vpedak.testsrecorder.plugin.core;

import com.vpedak.testsrecorder.core.events.Data;
import com.vpedak.testsrecorder.core.TestGenerator;
import com.vpedak.testsrecorder.core.events.*;

import java.util.List;


public class EspressoTestGenerator implements TestGenerator {
    private boolean wasParentView = false;

    @Override
    public String generate(String activityClassName, String testClassName, String packageName, List<RecordingEvent> events) {
        Templates templates = Templates.getInstance();
        StringBuilder sb = new StringBuilder(templates.getTemplate("test_main"));
        replace(sb, "{PACKAGE}", packageName);
        replace(sb, "{TEST_CLASS}", testClassName);
        replace(sb, "{ACTIVITY_CLASS}", activityClassName);
        replace(sb, "{BODY}", generateBody(events));

        if (wasParentView) {
            replace(sb, "{ADDITION}", "\n\n"+nthChildOfTemplate);
        } else {
            replace(sb, "{ADDITION}", "");
        }
        return sb.toString();
    }

    @Override
    public void generateSubject(StringBuilder sb, Subject subject) {
        // do nothing
    }

    @Override
    public void generateActon(StringBuilder sb, Action action, Subject subject) {
        // do nothing
    }


    @Override
    public void generateSubject(StringBuilder sb, View subject) {
        sb.append("onView(withId(").append(subject.getId()).append(")).");
    }

    @Override
    public void generateSubject(StringBuilder sb, OptionsMenu subject) {
        sb.append("openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext())");
    }

    @Override
    public void generateSubject(StringBuilder sb, MenuItem subject) {
        sb.append("onView(withText(\"").append(subject.getTitle()).append("\")).");
    }

    @Override
    public void generateSubject(StringBuilder sb, ParentView subject) {
        wasParentView = true;
        sb.append("onView(nthChildOf(withId(").append(subject.getParentId()).append("), ").append(subject.getChildIndex()).append(")).");
    }

    @Override
    public void generateSubject(StringBuilder sb, Data subject) {
        if (subject.getValue() != null) {
            sb.append("onData(allOf(is(instanceOf(").append(subject.getClassName()).append(".class)), is(\"").
                    append(subject.getValue()).append("\"))).inAdapterView(withId(").append(subject.getAdapterId()).
                    append(")).");
        } else {
            StringBuilder tmp = new StringBuilder(dataTemplate);
            replace( tmp, "CLASS", subject.getClassName());
            replace( tmp, "ADAPTER_ID", subject.getAdapterId());
            sb.append(tmp);
        }
    }

    @Override
    public void generateActon(StringBuilder sb, ClickAction action, Subject subject) {
        if (!(subject instanceof OptionsMenu)) {
            sb.append("perform(click())");
        }
    }

    @Override
    public void generateActon(StringBuilder sb, LongClickAction action, Subject subject) {
        if (!(subject instanceof OptionsMenu)) {
            sb.append("perform(longClick())");
        }
    }

    @Override
    public void generateActon(StringBuilder sb, ReplaceTextAction action, Subject subject) {
        sb.append("perform(replaceText(\""+action.getText()+"\"))");
    }

    @Override
    public void generateActon(StringBuilder sb, SwipeUpAction action, Subject subject) {
        sb.append("perform(swipeUp())");
    }

    @Override
    public void generateActon(StringBuilder sb, SwipeDownAction action, Subject subject) {
        sb.append("perform(swipeDown())");
    }

    @Override
    public void generateActon(StringBuilder sb, SwipeLeftAction action, Subject subject) {
        sb.append("perform(swipeLeft())");
    }

    @Override
    public void generateActon(StringBuilder sb, SwipeRightAction action, Subject subject) {
        sb.append("perform(swipeRight())");
    }

    private String generateBody(List<RecordingEvent> events) {
        StringBuilder sb = new StringBuilder();
        for(RecordingEvent event : events) {
            event.accept(sb, this);
            sb.append(";\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    public void generateEvent(StringBuilder sb, RecordingEvent event) {
        sb.append("\t// ").append(event.getDescription()).append('\n');
        event.getSubject().accept(sb, this);
        event.getAction().accept(sb, this, event.getSubject());
    }

    private StringBuilder replace(StringBuilder sb, String from, String to) {
        int index = -1;
        while ((index = sb.lastIndexOf(from)) != -1) {
            sb.replace(index, index + from.length(), to);
        }
        return sb;
    }

    private String dataTemplate =
            "\t// see details at http://droidtestlab.com/adapterView.html\n" +
            "onData(allOf(is(new BoundedMatcher<Object, CLASS>(CLASS.class) {" +
            "    @Override" +
            "    public void describeTo(Description description) {" +
            "    }" +
            "    @Override" +
            "    protected boolean matchesSafely(CLASS obj) {\n" +
            "        /* TODO Implement comparison logic */\n" +
            "        return false;" +
            "    }" +
            "}))).inAdapterView(withId(ADAPTER_ID)).";

    /* TODO: If need it is possible to access List or Grids by position:
    onData(allOf(is(instanceOf(<DATA>.class))))
            .inAdapterView(withId(<LIST_GRID_ID>))
            .atPosition(0)
    .perform(click());
    */

    private String nthChildOfTemplate =
            "    public static Matcher<View> nthChildOf(final Matcher<View> parentMatcher, final int childPosition) {\n" +
            "        return new TypeSafeMatcher<View>() {\n" +
            "            @Override\n" +
            "            public void describeTo(Description description) {\n" +
            "            }\n" +
            "            @Override\n" +
            "            public boolean matchesSafely(View view) {\n" +
            "                if (!(view.getParent() instanceof ViewGroup)) {\n" +
            "                    return false;\n" +
            "                }\n" +
            "                ViewGroup group = (ViewGroup) view.getParent();\n" +
            "                return parentMatcher.matches(group) && view.equals(group.getChildAt(childPosition));\n" +
            "            }\n" +
            "        };\n" +
            "    }\n";
}
