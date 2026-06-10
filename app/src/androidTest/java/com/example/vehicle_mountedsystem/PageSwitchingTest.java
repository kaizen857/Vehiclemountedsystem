package com.example.vehicle_mountedsystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.vehicle_mountedsystem.ui.pages.VehicleControlPageController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PageSwitchingTest {

    @Before
    public void clearHvacState() {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("hvac_state", Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(VehicleControlPageController.PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void primaryTabsReplacePageHostContent() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            assertPage(R.id.pageDashboardRoot, R.id.dashboardPageTitle, R.string.title_overview, R.string.page_dashboard_badge);

            onView(withId(R.id.tabMinimal)).perform(click());
            assertPage(R.id.pageMinimalVehicleRoot, R.id.minimalPageTitle, R.string.title_minimal, R.string.page_minimal_badge);

            onView(withId(R.id.tabNavigation)).perform(click());
            assertPage(R.id.pageNavigationRoot, R.id.navigationPageTitle, R.string.title_navigation, R.string.page_navigation_badge);

            onView(withId(R.id.tabSensors)).perform(click());
            assertPage(R.id.pageSensorRoot, R.id.sensorPageTitle, R.string.title_sensors, R.string.page_sensor_badge);

            onView(withId(R.id.tabHvac)).perform(click());
            assertPage(R.id.pageHvacRoot, R.id.hvacPageTitle, R.string.title_hvac, R.string.page_hvac_badge);

            onView(withId(R.id.tabMedia)).perform(click());
            assertPage(R.id.pageMediaRoot, R.id.mediaPageTitle, R.string.title_media, R.string.page_media_badge);
            assertMediaStateVisible();

            onView(withId(R.id.tabControls)).perform(click());
            assertPage(R.id.pageControlRoot, R.id.controlPageTitle, R.string.title_controls, R.string.page_control_badge);

            onView(withId(R.id.tabSettings)).perform(click());
            assertPage(R.id.pageSettingsRoot, R.id.settingsPageTitle, R.string.title_settings, R.string.page_settings_badge);

            onView(withId(R.id.tabOverview)).perform(click());
            assertPage(R.id.pageDashboardRoot, R.id.dashboardPageTitle, R.string.title_overview, R.string.page_dashboard_badge);
        }
    }

    @Test
    public void hvacStateSurvivesActivityRecreate() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.tabHvac)).perform(click());
            assertPage(R.id.pageHvacRoot, R.id.hvacPageTitle, R.string.title_hvac, R.string.page_hvac_badge);
            assertHvacState("24°C", "2", "自动", "AC 关闭", "外循环");
            assertCapabilityMessage();

            onView(withId(R.id.hvacTemperatureIncrease)).perform(scrollTo(), click());
            onView(withId(R.id.hvacTemperatureIncrease)).perform(scrollTo(), click());
            onView(withId(R.id.hvacFanIncrease)).perform(scrollTo(), click());
            onView(withId(R.id.hvacModeCool)).perform(scrollTo(), click());
            onView(withId(R.id.hvacAcToggle)).perform(scrollTo(), click());
            onView(withId(R.id.hvacCirculationToggle)).perform(scrollTo(), click());
            assertHvacState("26°C", "3", "制冷", "AC 开启", "内循环");

            scenario.recreate();
            onView(withId(R.id.tabHvac)).perform(click());
            assertPage(R.id.pageHvacRoot, R.id.hvacPageTitle, R.string.title_hvac, R.string.page_hvac_badge);
            assertHvacState("26°C", "3", "制冷", "AC 开启", "内循环");
            assertCapabilityMessage();
        }
    }

    @Test
    public void controlPageUpdatesDemoStateAndSurvivesActivityRecreate() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.tabControls)).perform(click());
            assertPage(R.id.pageControlRoot, R.id.controlPageTitle, R.string.title_controls, R.string.page_control_badge);
            onView(allOf(withId(R.id.controlDemoNotice), withText(R.string.control_demo_notice))).check(matches(isDisplayed()));
            assertControlState(
                    "本地演示：左前窗已关闭",
                    "本地演示：左后视镜已展开",
                    "本地演示：座椅加热已关闭");

            onView(withId(R.id.windowFrontLeftAction)).perform(scrollTo(), click());
            onView(withId(R.id.mirrorLeftAction)).perform(scrollTo(), click());
            onView(withId(R.id.seatHeatAction)).perform(scrollTo(), click());
            assertControlState(
                    "本地演示：左前窗已打开",
                    "本地演示：左后视镜已折叠",
                    "本地演示：座椅加热已开启");

            scenario.recreate();
            onView(withId(R.id.tabControls)).perform(click());
            assertPage(R.id.pageControlRoot, R.id.controlPageTitle, R.string.title_controls, R.string.page_control_badge);
            assertControlState(
                    "本地演示：左前窗已打开",
                    "本地演示：左后视镜已折叠",
                    "本地演示：座椅加热已开启");
        }
    }

    @Test
    public void settingsPageShowsNonEmptyCapabilityStatuses() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.tabSettings)).perform(click());
            assertPage(R.id.pageSettingsRoot, R.id.settingsPageTitle, R.string.title_settings, R.string.page_settings_badge);
            assertNonEmptyText(R.id.settingNotificationStatus);
            assertNonEmptyText(R.id.settingSensorStatus);
            assertNonEmptyText(R.id.settingIrStatus);
            assertNonEmptyText(R.id.settingMediaStatus);
            assertNonEmptyText(R.id.settingDataSource);
        }
    }

    private static void assertPage(int rootId, int pageBadgeId, int shellTitleResId, int badgeTextResId) {
        onView(withId(rootId)).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.pageTitle), withText(shellTitleResId))).check(matches(isDisplayed()));
        onView(allOf(withId(pageBadgeId), withText(badgeTextResId))).check(matches(isDisplayed()));
    }

    private static void assertHvacState(
            String temperature,
            String fan,
            String mode,
            String ac,
            String circulation) {
        assertText(R.id.hvacTemperatureValue, temperature);
        assertText(R.id.hvacFanValue, fan);
        assertText(R.id.hvacModeValue, mode);
        assertText(R.id.hvacAcState, ac);
        assertText(R.id.hvacCirculationState, circulation);
    }

    private static void assertCapabilityMessage() {
        onView(withId(R.id.hvacIrCapabilityMessage)).perform(scrollTo());
        onView(allOf(withId(R.id.hvacIrCapabilityMessage), withText(R.string.hvac_ir_capability_local_only)))
                .check(matches(isDisplayed()));
    }

    private static void assertMediaStateVisible() {
        assertText(R.id.mediaTitleValue, "无媒体");
        assertText(R.id.mediaArtistValue, "未知艺术家");
        assertText(R.id.mediaPlaybackValue, "已暂停/未播放");
        assertNonEmptyText(R.id.mediaConnectionModeValue);
        assertNonEmptyText(R.id.mediaStatusMessage);
    }

    private static void assertControlState(String windowState, String mirrorState, String seatHeatState) {
        assertText(R.id.windowFrontLeftState, windowState);
        assertText(R.id.mirrorLeftState, mirrorState);
        assertText(R.id.seatHeatState, seatHeatState);
    }

    private static void assertText(int viewId, String text) {
        onView(withId(viewId)).perform(scrollTo());
        onView(allOf(withId(viewId), withText(text))).check(matches(isDisplayed()));
    }

    private static void assertNonEmptyText(int viewId) {
        onView(withId(viewId)).perform(scrollTo());
        onView(allOf(withId(viewId), withText(not(isEmptyString())))).check(matches(isDisplayed()));
    }
}
