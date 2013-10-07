package com.dozuki.ifixit.ui.search;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import com.actionbarsherlock.view.MenuItem;
import com.dozuki.ifixit.ui.BaseSearchMenuDrawerActivity;
import com.dozuki.ifixit.MainApplication;
import com.dozuki.ifixit.R;
import com.dozuki.ifixit.model.search.Search;
import com.dozuki.ifixit.ui.topic_view.TopicActivity;
import com.dozuki.ifixit.util.APIEndpoint;
import com.dozuki.ifixit.util.APIEvent;
import com.dozuki.ifixit.util.APIService;
import com.squareup.otto.Subscribe;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class SearchActivity extends BaseSearchMenuDrawerActivity {
   private static final int GUIDES_POSITION = 0;
   private static final int TOPIC_POSITION = 1;
   private static final String TOPIC_SEARCH_FRAGMENT = "TOPIC_SEARCH_FRAGMENT";
   private static final String GUIDE_SEARCH_FRAGMENT = "GUIDE_SEARCH_FRAGMENT";
   private String mQuery = "";
   private Spinner mSpinner;
   private int mSpinnerPosition = 0;
   private String mCurrentTag;
   private TextView mResultCount;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      setMenuDrawerSlideDrawable(R.drawable.abs__ic_ab_back_holo_dark);

      setContentView(R.layout.search);

      showLoading(R.id.search_results_container);

      handleIntent(getIntent(), false);

      mResultCount = (TextView) findViewById(R.id.search_result_count);
      mSpinner = (Spinner) findViewById(R.id.search_type_spinner);

      ArrayList<String> searchTypes = new ArrayList<String>();
      searchTypes.add(GUIDES_POSITION, getString(R.string.guides));
      searchTypes.add(TOPIC_POSITION, MainApplication.get().getSite().getObjectNamePlural());

      ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
       android.R.layout.simple_spinner_dropdown_item, searchTypes);
      mSpinner.setAdapter(spinnerAdapter);

      mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
         @Override
         public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mSpinnerPosition = position;
            handleSearch(buildQuery(mQuery));
         }

         @Override
         public void onNothingSelected(AdapterView<?> parent) { }
      });
   }

   @Override
   public void onNewIntent(Intent intent) {
      setIntent(intent);
      handleIntent(intent, true);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         // Respond to the action bar's Up/Home button
         case android.R.id.home:
            Intent intent = new Intent(this, TopicActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
      }
      return super.onOptionsItemSelected(item);
   }

   @SuppressWarnings("unused")
   @Subscribe
   public void onSearchResults(APIEvent.Search event) {
      hideLoading();

      if (!event.hasError()) {
         Search search = event.getResult();

         mResultCount.setText(getString(R.string.result_count, search.mTotalResults));

         FragmentManager fm = getSupportFragmentManager();
         FragmentTransaction ft = fm.beginTransaction();

         Fragment frag = fm.findFragmentByTag(mCurrentTag);

         if (frag == null) {
            frag = SearchFragment.newInstance(search);
            ft.replace(R.id.search_results_container, frag, mCurrentTag).commit();
         } else {
            ((SearchFragment)frag).setSearchResults(search);
         }
      } else {
         APIService.getErrorDialog(this, event).show();
      }
   }

   public String buildQuery(String query) {
      switch (mSpinnerPosition) {
         case GUIDES_POSITION:
            mCurrentTag = GUIDE_SEARCH_FRAGMENT;
            query += "?filter=guide,teardowns";

            break;
         case TOPIC_POSITION:
            mCurrentTag = TOPIC_SEARCH_FRAGMENT;
            query += "?filter=device";

            break;
      }

      return query;
   }

   private void handleSearch(String query) {
      showLoading(R.id.search_results_container);
      APIService.call(this, APIService.getSearchAPICall(APIEndpoint.SEARCH, query));
   }

   private void handleIntent(Intent intent, boolean sendQuery) {
      if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
         search(intent.getStringExtra(SearchManager.QUERY), sendQuery);
      }
   }

   private void search(final String query, boolean sendQuery) {
      getSupportActionBar().setTitle(query);

      SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
       SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
      suggestions.saveRecentQuery(query, null);

      try {
         mQuery = URLEncoder.encode(query, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         e.printStackTrace();
      }

      if (sendQuery) {
         handleSearch(buildQuery(mQuery));
      }
   }
}