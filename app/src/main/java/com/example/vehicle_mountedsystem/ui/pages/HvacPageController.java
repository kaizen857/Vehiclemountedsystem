package com.example.vehicle_mountedsystem.ui.pages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.vehicle_mountedsystem.R;
import com.example.vehicle_mountedsystem.data.hvac.HvacRepository;
import com.example.vehicle_mountedsystem.model.HvacState;

public final class HvacPageController {
    private final HvacRepository repository;

    private TextView temperatureValue;
    private TextView fanValue;
    private TextView modeValue;
    private TextView acState;
    private TextView circulationState;
    private TextView capabilityMessage;

    public HvacPageController(HvacRepository repository) {
        this.repository = repository;
    }

    public View createView(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_hvac, parent, false);
        bindViews(view);
        bindActions(view);
        render(repository.load());
        return view;
    }

    private void bindViews(View view) {
        temperatureValue = view.findViewById(R.id.hvacTemperatureValue);
        fanValue = view.findViewById(R.id.hvacFanValue);
        modeValue = view.findViewById(R.id.hvacModeValue);
        acState = view.findViewById(R.id.hvacAcState);
        circulationState = view.findViewById(R.id.hvacCirculationState);
        capabilityMessage = view.findViewById(R.id.hvacIrCapabilityMessage);
    }

    private void bindActions(View view) {
        click(view, R.id.hvacTemperatureDecrease, () -> render(repository.changeTemperature(-1)));
        click(view, R.id.hvacTemperatureIncrease, () -> render(repository.changeTemperature(1)));
        click(view, R.id.hvacFanDecrease, () -> render(repository.changeFanLevel(-1)));
        click(view, R.id.hvacFanIncrease, () -> render(repository.changeFanLevel(1)));
        click(view, R.id.hvacModeAuto, () -> render(repository.setMode(HvacRepository.MODE_AUTO)));
        click(view, R.id.hvacModeCool, () -> render(repository.setMode(HvacRepository.MODE_COOL)));
        click(view, R.id.hvacModeHeat, () -> render(repository.setMode(HvacRepository.MODE_HEAT)));
        click(view, R.id.hvacModeDefog, () -> render(repository.setMode(HvacRepository.MODE_DEFOG)));
        click(view, R.id.hvacModeFan, () -> render(repository.setMode(HvacRepository.MODE_FAN)));
        click(view, R.id.hvacAcToggle, () -> render(repository.setAcEnabled(!repository.load().isAcEnabled())));
        click(view, R.id.hvacCirculationToggle, () -> render(repository.setInnerCirculationEnabled(!repository.load().isInnerCirculationEnabled())));
    }

    private void render(HvacState state) {
        temperatureValue.setText(state.getTemperatureCelsius() + "°C");
        fanValue.setText(String.valueOf(state.getFanLevel()));
        modeValue.setText(state.getModeLabel());
        acState.setText(state.isAcEnabled() ? "AC 开启" : "AC 关闭");
        circulationState.setText(state.isInnerCirculationEnabled() ? "内循环" : "外循环");
        capabilityMessage.setText(R.string.hvac_ir_capability_local_only);
    }

    private static void click(View root, int id, Action action) {
        root.findViewById(id).setOnClickListener(v -> action.run());
    }

    private interface Action {
        void run();
    }
}
