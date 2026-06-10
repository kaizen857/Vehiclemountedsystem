package com.example.vehicle_mountedsystem.data.hvac;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.vehicle_mountedsystem.model.HvacState;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class HvacRepository {
    public static final HvacMode MODE_AUTO = new HvacMode("AUTO", "自动");
    public static final HvacMode MODE_COOL = new HvacMode("COOL", "制冷");
    public static final HvacMode MODE_HEAT = new HvacMode("HEAT", "制热");
    public static final HvacMode MODE_DEFOG = new HvacMode("DEFOG", "除雾");
    public static final HvacMode MODE_FAN = new HvacMode("FAN", "送风");

    private static final String PREFERENCES_NAME = "hvac_state";
    private static final String KEY_TEMPERATURE = "temperatureCelsius";
    private static final String KEY_FAN = "fanLevel";
    private static final String KEY_MODE = "mode";
    private static final String KEY_MODE_LABEL = "modeLabel";
    private static final String KEY_AC = "acEnabled";
    private static final String KEY_INNER_CIRCULATION = "innerCirculationEnabled";
    private static final List<HvacMode> MODES = Collections.unmodifiableList(Arrays.asList(
            MODE_AUTO,
            MODE_COOL,
            MODE_HEAT,
            MODE_DEFOG,
            MODE_FAN));

    private final HvacStorage storage;

    public HvacRepository(Context context) {
        this(new SharedPreferencesHvacStorage(context.getApplicationContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)));
    }

    public HvacRepository(HvacStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    public static List<HvacMode> modes() {
        return MODES;
    }

    public HvacState load() {
        HvacState defaultState = HvacState.defaultState();
        try {
            HvacMode mode = modeFor(storage.getString(KEY_MODE, defaultState.getMode()));
            return new HvacState(
                    storage.getInt(KEY_TEMPERATURE, defaultState.getTemperatureCelsius()),
                    storage.getInt(KEY_FAN, defaultState.getFanLevel()),
                    mode.getCode(),
                    mode.getLabel(),
                    storage.getBoolean(KEY_AC, defaultState.isAcEnabled()),
                    storage.getBoolean(KEY_INNER_CIRCULATION, defaultState.isInnerCirculationEnabled()));
        } catch (RuntimeException ignored) {
            return defaultState;
        }
    }

    public HvacState save(HvacState state) {
        HvacState normalized = normalize(state);
        storage.putInt(KEY_TEMPERATURE, normalized.getTemperatureCelsius());
        storage.putInt(KEY_FAN, normalized.getFanLevel());
        storage.putString(KEY_MODE, normalized.getMode());
        storage.putString(KEY_MODE_LABEL, normalized.getModeLabel());
        storage.putBoolean(KEY_AC, normalized.isAcEnabled());
        storage.putBoolean(KEY_INNER_CIRCULATION, normalized.isInnerCirculationEnabled());
        return normalized;
    }

    public HvacState changeTemperature(int deltaCelsius) {
        HvacState state = load();
        return save(new HvacState(
                state.getTemperatureCelsius() + deltaCelsius,
                state.getFanLevel(),
                state.getMode(),
                state.getModeLabel(),
                state.isAcEnabled(),
                state.isInnerCirculationEnabled()));
    }

    public HvacState changeFanLevel(int delta) {
        HvacState state = load();
        return save(new HvacState(
                state.getTemperatureCelsius(),
                state.getFanLevel() + delta,
                state.getMode(),
                state.getModeLabel(),
                state.isAcEnabled(),
                state.isInnerCirculationEnabled()));
    }

    public HvacState setMode(HvacMode mode) {
        HvacState state = load();
        HvacMode normalizedMode = modeFor(Objects.requireNonNull(mode, "mode").getCode());
        return save(new HvacState(
                state.getTemperatureCelsius(),
                state.getFanLevel(),
                normalizedMode.getCode(),
                normalizedMode.getLabel(),
                state.isAcEnabled(),
                state.isInnerCirculationEnabled()));
    }

    public HvacState setAcEnabled(boolean enabled) {
        HvacState state = load();
        return save(new HvacState(
                state.getTemperatureCelsius(),
                state.getFanLevel(),
                state.getMode(),
                state.getModeLabel(),
                enabled,
                state.isInnerCirculationEnabled()));
    }

    public HvacState setInnerCirculationEnabled(boolean enabled) {
        HvacState state = load();
        return save(new HvacState(
                state.getTemperatureCelsius(),
                state.getFanLevel(),
                state.getMode(),
                state.getModeLabel(),
                state.isAcEnabled(),
                enabled));
    }

    private static HvacState normalize(HvacState state) {
        HvacState requiredState = Objects.requireNonNull(state, "state");
        HvacMode mode = modeFor(requiredState.getMode());
        return new HvacState(
                requiredState.getTemperatureCelsius(),
                requiredState.getFanLevel(),
                mode.getCode(),
                mode.getLabel(),
                requiredState.isAcEnabled(),
                requiredState.isInnerCirculationEnabled());
    }

    private static HvacMode modeFor(String code) {
        for (HvacMode mode : MODES) {
            if (mode.getCode().equals(code)) {
                return mode;
            }
        }
        return MODE_AUTO;
    }

    private static final class SharedPreferencesHvacStorage implements HvacStorage {
        private final SharedPreferences preferences;

        private SharedPreferencesHvacStorage(SharedPreferences preferences) {
            this.preferences = preferences;
        }

        @Override
        public int getInt(String key, int defaultValue) {
            return preferences.getInt(key, defaultValue);
        }

        @Override
        public String getString(String key, String defaultValue) {
            String value = preferences.getString(key, defaultValue);
            return value == null ? defaultValue : value;
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            return preferences.getBoolean(key, defaultValue);
        }

        @Override
        public void putInt(String key, int value) {
            preferences.edit().putInt(key, value).commit();
        }

        @Override
        public void putString(String key, String value) {
            preferences.edit().putString(key, value).commit();
        }

        @Override
        public void putBoolean(String key, boolean value) {
            preferences.edit().putBoolean(key, value).commit();
        }
    }

    public static final class HvacMode {
        private final String code;
        private final String label;

        private HvacMode(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }
    }
}
