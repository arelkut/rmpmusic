package com.musicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.musicapp.databinding.FragmentSearchBinding;
import com.musicapp.model.GenreOut;
import com.musicapp.model.SearchResponse;
import com.musicapp.model.TrackOut;
import com.musicapp.network.ApiClient;
import com.musicapp.network.ApiService;
import com.musicapp.ui.auth.LoginActivity;
import com.musicapp.ui.home.TrackRecentAdapter;
import com.musicapp.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
            return;
        }

        apiService = ApiClient.getApiService(requireContext());

        loadGenres();
        setupSearch();
        setupPopularQueries();
    }

    private void loadGenres() {
        apiService.getGenres().enqueue(new Callback<List<GenreOut>>() {
            @Override
            public void onResponse(@NonNull Call<List<GenreOut>> call,
                                   @NonNull Response<List<GenreOut>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<GenreOut> genres = response.body();
                    GenreAdapter adapter = new GenreAdapter(genres, genre -> {
                        // Поиск по жанру
                        performSearch(genre.name);
                        if (binding.etSearch != null)
                            binding.etSearch.setText(genre.name);
                    });
                    binding.rvGenres.setLayoutManager(new GridLayoutManager(getContext(), 2));
                    binding.rvGenres.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<GenreOut>> call, @NonNull Throwable t) { }
        });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    showDefault();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String q = binding.etSearch.getText() != null
                        ? binding.etSearch.getText().toString().trim() : "";
                if (!q.isEmpty()) performSearch(q);
                return true;
            }
            return false;
        });
    }

    private void performSearch(String query) {
        binding.layoutSearchResults.setVisibility(View.VISIBLE);
        binding.layoutDefault.setVisibility(View.GONE);

        apiService.search(query, 20).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call,
                                   @NonNull Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TrackOut> tracks = response.body().tracks != null
                            ? response.body().tracks : new ArrayList<>();

                    TrackRecentAdapter adapter = new TrackRecentAdapter(tracks, (track, position) -> {
                        if (requireActivity() instanceof MainActivity) {
                            MainActivity main = (MainActivity) requireActivity();
                            if (main.isServiceBound()) {
                                main.getMusicService().setQueue(tracks, position);
                            }
                        }
                    });
                    binding.rvSearchTracks.setLayoutManager(new LinearLayoutManager(getContext()));
                    binding.rvSearchTracks.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) { }
        });
    }

    private void showDefault() {
        binding.layoutSearchResults.setVisibility(View.GONE);
        binding.layoutDefault.setVisibility(View.VISIBLE);
    }

    private void setupPopularQueries() {
        String[] queries = {"Лучшие хиты 2024", "Расслабляющая музыка", "Тренировка", "Вечерний чилл"};
        View[] rows = {binding.query1, binding.query2, binding.query3, binding.query4};
        for (int i = 0; i < rows.length; i++) {
            final String q = queries[i];
            rows[i].setOnClickListener(v -> {
                binding.etSearch.setText(q);
                performSearch(q);
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}