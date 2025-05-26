package com.kirawii.thunderswufe.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.snackbar.Snackbar;
import com.kirawii.thunderswufe.data.ElectricityData;
import com.kirawii.thunderswufe.databinding.FragmentHomeBinding;
import com.kirawii.thunderswufe.ui.viewmodels.HomeViewModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        setupChart();
        observeViewModel();
    }

    private void setupViews() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refreshData());
    }

    private void setupChart() {
        binding.usageChart.getDescription().setEnabled(false);
        binding.usageChart.setTouchEnabled(true);
        binding.usageChart.setDragEnabled(true);
        binding.usageChart.setScaleEnabled(true);
        binding.usageChart.setPinchZoom(true);
        binding.usageChart.setDrawGridBackground(false);

        XAxis xAxis = binding.usageChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // 简化处理，仅显示索引
                return String.valueOf((int) value);
            }
        });

        YAxis leftAxis = binding.usageChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        binding.usageChart.getAxisRight().setEnabled(false);
        binding.usageChart.getLegend().setEnabled(false);
    }

    private void updateChart(List<ElectricityData> usageHistory) {
        ArrayList<Entry> values = new ArrayList<>();
        for (int i = 0; i < usageHistory.size(); i++) {
            values.add(new Entry(i, (float) usageHistory.get(i).getBalance()));
        }

        LineDataSet set;
        if (binding.usageChart.getData() != null &&
            binding.usageChart.getData().getDataSetCount() > 0) {
            set = (LineDataSet) binding.usageChart.getData().getDataSetByIndex(0);
            set.setValues(values);
            binding.usageChart.getData().notifyDataChanged();
            binding.usageChart.notifyDataSetChanged();
        } else {
            set = new LineDataSet(values, "电费余额");
            set.setDrawIcons(false);
            set.setColor(Color.BLUE);
            set.setCircleColor(Color.BLUE);
            set.setLineWidth(2f);
            set.setCircleRadius(3f);
            set.setDrawCircleHole(false);
            set.setValueTextSize(9f);
            set.setDrawFilled(true);
            set.setFormLineWidth(1f);
            set.setFormSize(15.f);
            set.setFillColor(Color.BLUE);
            set.setFillAlpha(30);

            LineData data = new LineData(set);
            binding.usageChart.setData(data);
        }

        binding.usageChart.invalidate();
    }

    private void observeViewModel() {
        viewModel.getCurrentBalance().observe(getViewLifecycleOwner(), electricityData -> {
            if (electricityData != null) {
                binding.balanceTextView.setText(String.format(Locale.CHINA, "%.2f", electricityData.getBalance()));
                binding.lastUpdateTextView.setText(electricityData.getLastUpdateTime());
            }
        });

        viewModel.getUsageHistory().observe(getViewLifecycleOwner(), usageHistory -> {
            if (usageHistory != null && !usageHistory.isEmpty()) {
                updateChart(usageHistory);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.swipeRefreshLayout.setRefreshing(isLoading);
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 