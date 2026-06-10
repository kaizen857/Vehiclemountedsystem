package com.example.vehicle_mountedsystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PageSwitchingTest {

    @Test
    public void fourPrimaryTabsReplacePageHostContent() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            assertPage(R.id.pageDashboardRoot, R.id.dashboardPageTitle, R.string.title_overview, R.string.page_dashboard_badge);

            onView(withId(R.id.tabMinimal)).perform(click());
            assertPage(R.id.pageMinimalVehicleRoot, R.id.minimalPageTitle, R.string.title_minimal, R.string.page_minimal_badge);

            onView(withId(R.id.tabNavigation)).perform(click());
            assertPage(R.id.pageNavigationRoot, R.id.navigationPageTitle, R.string.title_navigation, R.string.page_navigation_badge);

            onView(withId(R.id.tabSensors)).perform(click());
            assertPage(R.id.pageSensorRoot, R.id.sensorPageTitle, R.string.title_sensors, R.string.page_sensor_badge);

            onView(withId(R.id.tabOverview)).perform(click());
            assertPage(R.id.pageDashboardRoot, R.id.dashboardPageTitle, R.string.title_overview, R.string.page_dashboard_badge);
        }
    }

    private static void assertPage(int rootId, int pageBadgeId, int shellTitleResId, int badgeTextResId) {
        onView(withId(rootId)).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.pageTitle), withText(shellTitleResId))).check(matches(isDisplayed()));
        onView(allOf(withId(pageBadgeId), withText(badgeTextResId))).check(matches(isDisplayed()));
    }
}
