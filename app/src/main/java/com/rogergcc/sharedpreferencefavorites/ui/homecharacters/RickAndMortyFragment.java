/*
 * Created by rogergcc
 * Copyright Ⓒ 2021 . All rights reserved.
 */

package com.rogergcc.sharedpreferencefavorites.ui.homecharacters;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rogergcc.sharedpreferencefavorites.R;
import com.rogergcc.sharedpreferencefavorites.databinding.FragmentRickandmortyListBinding;
import com.rogergcc.sharedpreferencefavorites.model.RickMorty;
import com.rogergcc.sharedpreferencefavorites.model.RickMortyResponse;
import com.rogergcc.sharedpreferencefavorites.remote.CommonApiUrl;
import com.rogergcc.sharedpreferencefavorites.ui.helpers.MySharedPreference;
import com.rogergcc.sharedpreferencefavorites.ui.utils.AppLogger;
import com.rogergcc.sharedpreferencefavorites.ui.utils.CommonUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

import static com.rogergcc.sharedpreferencefavorites.ui.helpers.PaginationListener.PAGE_START;


public class RickAndMortyFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    int firstVisibleItem, visibleItemCount, totalItemCount;
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private LinearLayoutManager linearLayoutManager;
    private List<RickMorty> rickMortyCharactersList;
    private ProgressDialog pd;
    private ListCharactersAdapter adapterapi;
    private ArrayList<RickMorty> mFavoritesList;
    private int currentPage;
    private boolean isLastPage = false;
    private int totalPage = 2;
    private boolean isLoading = false;
    private FragmentRickandmortyListBinding binding;
    private Context mcontext;
    private ProgressDialog mProgressDialog;
    private boolean loading = true;

    private HomeCharactersViewModel viewModel;

    public RickAndMortyFragment() {
    }

    public void hideLoading() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.cancel();
        }
    }

    public void showLoading() {
        //hideLoading();
        mProgressDialog = CommonUtils.showLoadingDialog(mcontext);
    }
    public void toggleLoadingList(){
        if (mProgressDialog == null) {
            mProgressDialog = CommonUtils.showLoadingDialog(mcontext);
        } else {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            } else {
                mProgressDialog.show();
            }
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLogger.e("RickAndMortyFragment" + "=>onCreate");
        viewModel = new ViewModelProvider(this).get(HomeCharactersViewModel.class);

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_rickandmorty_list, container, false);

        binding = FragmentRickandmortyListBinding.inflate(getLayoutInflater());
        mcontext = this.getActivity();
        AppLogger.e("RickAndMortyFragment" + "=>oncreateView");
        AppLogger.e("RickAndMortyFragment" + "Page=>" + currentPage);


        View view = binding.getRoot();

//        binding.list = view.findViewById(R.id.list);



        binding.list.setHasFixedSize(true);
        linearLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);

        binding.list.setLayoutManager(linearLayoutManager);

        rickMortyCharactersList = new ArrayList<>();

        loadFavoritesData();

        adapterapi = new ListCharactersAdapter(mFavoritesList, rickMortyCharactersList);
        binding.list.setAdapter(adapterapi);

        currentPage = PAGE_START;
//        getCharacters();
        getMyListCharactersViewModel();
//        binding.list.addOnScrollListener(new PaginationListener(linearLayoutManager) {
//            @Override
//            protected void loadMoreItems() {
//                isLoading = true;
//                currentPage++;
//                getCharacters();
//            }
//
//            @Override
//            public boolean isLastPage() {
//                return isLastPage;
//            }
//
//            @Override
//            public boolean isLoading() {
//                return isLoading;
//            }
//        });

        binding.list.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (!binding.list.canScrollVertically(1)) {
                    if (currentPage <= totalPage) {
                        currentPage++;
//                        getCharacters();
                        getMyListCharactersViewModel();
                    }
                }
            }
        });

        return view;
    }

    private void getMyListCharactersViewModel() {
        if (currentPage == 1)
            showLoading();
        else
            showProgressLoadingMore();

        viewModel.getHomeCharacters(currentPage).observe(getViewLifecycleOwner(), rickMortyResponse -> {
            if (currentPage == 1)
                hideLoading();
            else
                hideProgressLoadingMore();

            if (rickMortyResponse==null)return;
            if (rickMortyResponse.getResults()==null)return;
            totalPage = rickMortyResponse.getInfo().getPages();
            int oldCount = rickMortyCharactersList.size();
            rickMortyCharactersList.addAll(rickMortyResponse.getResults());
            adapterapi.notifyItemRangeInserted(oldCount, rickMortyCharactersList.size());
//            rickMortyCharactersList.addAll(rickMortyResponse.getResults());
//            adapterapi.notifyDataSetChanged();

        });
    }

    public void getCharacters() {

        if (currentPage == 1) showLoading();
        else showProgressLoadingMore();

        //region REGION SEND POST RETROFIT
        Call<RickMortyResponse> call = CommonApiUrl.getGeoJsonData().getCharacters(currentPage);
        call.enqueue(new Callback<RickMortyResponse>() {

            @Override
            public void onResponse(@NonNull Call<RickMortyResponse> call, retrofit2.Response<RickMortyResponse> response) {
                if (currentPage == 1) hideLoading();
                else hideProgressLoadingMore();
                if (response.body() == null) return;
                RickMortyResponse rickMortyResponse = response.body();
                //rickMortyCharactersList = rickMortyResponse.getResults();
                totalPage = rickMortyResponse.getInfo().getPages();

                int oldCount = rickMortyCharactersList.size();
                rickMortyCharactersList.addAll(rickMortyResponse.getResults());
                adapterapi.notifyItemRangeInserted(oldCount, rickMortyCharactersList.size());
            }

            @Override
            public void onFailure(@NonNull Call<RickMortyResponse> call, Throwable t) {
                hideLoading();
                Toast.makeText(mcontext, getString(R.string.message_somethin_wrong), Toast.LENGTH_SHORT).show();
                AppLogger.e("LISTA_ERROR: " + t.getMessage());
            }
        });
        //endregion


    }
//    private void toggleLoading() {
//        //https://github.com/Groestlcoin/groestlcoin-samourai-wallet-android/blob/master/app/src/main/java/com/samourai/wallet/CreateWalletActivity.java
//    }

    private void toggleLoadingMore() {

        int isLoadingMore = binding.pbLoadMore.getVisibility();

        if (currentPage != 1) {
            if (binding.pbLoadMore.getVisibility() == View.GONE) {
                hideProgressLoadingMore();
            } else {
                showProgressLoadingMore();
            }
        }

    }

    /*
     * Get Characters of api Rick & Morty
     * */

    public void showProgressLoadingMore() {

        binding.pbLoadMore.setVisibility(View.VISIBLE);
    }

    public void hideProgressLoadingMore() {

        binding.pbLoadMore.setVisibility(View.GONE);
    }


    private void loadFavoritesData() {
        MySharedPreference sharedPreference = new MySharedPreference(mcontext);
        String productsFromCart = sharedPreference.retrieveFavorites();
        Type type = new TypeToken<ArrayList<RickMorty>>() {
        }.getType();

        mFavoritesList = new Gson().fromJson(productsFromCart, type);
        if (mFavoritesList == null) {
            mFavoritesList = new ArrayList<>();
        }
    }

}
