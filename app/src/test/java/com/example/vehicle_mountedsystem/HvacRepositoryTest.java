package com.example.vehicle_mountedsystem;

import com.example.vehicle_mountedsystem.data.hvac.HvacRepository;
import com.example.vehicle_mountedsystem.data.hvac.HvacStorage;
import com.example.vehicle_mountedsystem.model.HvacState;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HvacRepositoryTest {
    @Test
    public void load_emptyStorageUsesDefaultState() {
        HvacState state = new HvacRepository(new FakeHvacStorage()).load();

        assertEquals(24, state.getTemperatureCelsius());
        assertEquals(2, state.getFanLevel());
        assertEquals("AUTO", state.getMode());
        assertEquals("自动", state.getModeLabel());
        assertFalse(state.isAcEnabled());
        assertFalse(state.isInnerCirculationEnabled());
    }

    @Test
    public void save_thenLoadReloadsState() {
        FakeHvacStorage storage = new FakeHvacStorage();
        HvacRepository repository = new HvacRepository(storage);

        repository.save(new HvacState(26, 3, "COOL", "制冷", true, true));
        HvacState reloaded = new HvacRepository(storage).load();

        assertEquals(26, reloaded.getTemperatureCelsius());
        assertEquals(3, reloaded.getFanLevel());
        assertEquals("COOL", reloaded.getMode());
        assertEquals("制冷", reloaded.getModeLabel());
        assertTrue(reloaded.isAcEnabled());
        assertTrue(reloaded.isInnerCirculationEnabled());
    }

    @Test
    public void load_corruptStoredTypesFallsBackToDefaultState() {
        FakeHvacStorage storage = new FakeHvacStorage();
        storage.putString("temperatureCelsius", "hot");
        storage.putInt("fanLevel", 4);
        storage.putString("mode", "COOL");
        storage.putBoolean("acEnabled", true);
        storage.putBoolean("innerCirculationEnabled", true);

        HvacState state = new HvacRepository(storage).load();

        assertEquals(HvacState.defaultState(), state);
    }

    @Test
    public void load_outOfRangeStoredValuesAreClamped() {
        FakeHvacStorage storage = new FakeHvacStorage();
        storage.putInt("temperatureCelsius", 99);
        storage.putInt("fanLevel", -4);
        storage.putString("mode", "HEAT");
        storage.putString("modeLabel", "制热");
        storage.putBoolean("acEnabled", true);
        storage.putBoolean("innerCirculationEnabled", true);

        HvacState state = new HvacRepository(storage).load();

        assertEquals(30, state.getTemperatureCelsius());
        assertEquals(0, state.getFanLevel());
        assertEquals("HEAT", state.getMode());
        assertEquals("制热", state.getModeLabel());
        assertTrue(state.isAcEnabled());
        assertTrue(state.isInnerCirculationEnabled());
    }

    @Test
    public void changeActionsPersistClampedState() {
        HvacRepository repository = new HvacRepository(new FakeHvacStorage());

        repository.changeTemperature(-20);
        repository.changeFanLevel(10);
        repository.setMode(HvacRepository.MODE_DEFOG);
        repository.setAcEnabled(true);
        repository.setInnerCirculationEnabled(true);
        HvacState state = repository.load();

        assertEquals(16, state.getTemperatureCelsius());
        assertEquals(5, state.getFanLevel());
        assertEquals("DEFOG", state.getMode());
        assertEquals("除霜", state.getModeLabel());
        assertTrue(state.isAcEnabled());
        assertTrue(state.isInnerCirculationEnabled());
    }

    private static final class FakeHvacStorage implements HvacStorage {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public int getInt(String key, int defaultValue) {
            Object value = values.get(key);
            return value == null ? defaultValue : (Integer) value;
        }

        @Override
        public String getString(String key, String defaultValue) {
            Object value = values.get(key);
            return value == null ? defaultValue : (String) value;
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            Object value = values.get(key);
            return value == null ? defaultValue : (Boolean) value;
        }

        @Override
        public void putInt(String key, int value) {
            values.put(key, value);
        }

        @Override
        public void putString(String key, String value) {
            values.put(key, value);
        }

        @Override
        public void putBoolean(String key, boolean value) {
            values.put(key, value);
        }
    }
}
