package com.example.vehicle_mountedsystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.allOf;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainShellSmokeTest {

    @Test
    public void launchShowsMainShellWithOverviewSelected() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.tabRail)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
            onView(withId(R.id.pageHost)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
            onView(allOf(withId(R.id.pageTitle), withText(R.string.title_overview)))
                    .check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));

            onView(allOf(withId(R.id.tabOverview), withText(R.string.tab_overview)))
                    .check(androidx.test.espresso.assertion.ViewAssertions.matches(isSelected()));
            onView(withId(R.id.tabMinimal)).check(androidx.test.espresso.assertion.ViewAssertions.matches(not(isSelected())));
            onView(withId(R.id.tabNavigation)).check(androidx.test.espresso.assertion.ViewAssertions.matches(not(isSelected())));
            onView(withId(R.id.tabSensors)).check(androidx.test.espresso.assertion.ViewAssertions.matches(not(isSelected())));
            onView(withId(R.id.tabHvac)).check(androidx.test.espresso.assertion.ViewAssertions.matches(not(isSelected())));
            onView(withId(R.id.tabMedia)).check(androidx.test.espresso.assertion.ViewAssertions.matches(not(isSelected())));
            onView(withId(R.id.tabControls)).check(androidx.test.espresso.assertion.ViewAssertions.matches(not(isSelected())));
            onView(withId(R.id.tabSettings)).check(androidx.test.espresso.assertion.ViewAssertions.matches(not(isSelected())));
        }
    }
}
