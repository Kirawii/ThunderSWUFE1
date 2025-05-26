package com.kirawii.thunderswufe.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import com.kirawii.thunderswufe.databinding.FragmentSettingsBinding;
import com.kirawii.thunderswufe.ui.viewmodels.SettingsViewModel;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        observeViewModel();
    }

    private void setupViews() {
        binding.saveButton.setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        String roomNo = binding.roomNoEditText.getText().toString().trim();
        String buildingNo = binding.buildingNoEditText.getText().toString().trim();
        String areaNo = binding.areaNoEditText.getText().toString().trim();

        if (roomNo.isEmpty() || buildingNo.isEmpty() || areaNo.isEmpty()) {
            Snackbar.make(binding.getRoot(), "请填写完整信息", Snackbar.LENGTH_SHORT).show();
            return;
        }

        viewModel.savePreferences(roomNo, buildingNo, areaNo);
    }

    private void observeViewModel() {
        viewModel.getUserPreferences().observe(getViewLifecycleOwner(), preferences -> {
            if (preferences != null) {
                binding.roomNoEditText.setText(preferences.getRoomNo());
                binding.buildingNoEditText.setText(preferences.getBuildingNo());
            }
        });

        viewModel.getIsSaving().observe(getViewLifecycleOwner(), isSaving -> {
            binding.saveButton.setEnabled(!isSaving);
            binding.progressBar.setVisibility(isSaving ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getSaved().observe(getViewLifecycleOwner(), saved -> {
            if (saved != null && saved) {
                Snackbar.make(binding.getRoot(), "保存成功", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 