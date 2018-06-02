package ru.society.waterflower;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.melnykov.fab.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ru.society.waterflower.adapter.AdvancedItemListAdapter;
import ru.society.waterflower.app.App;
import ru.society.waterflower.constants.Constants;
import ru.society.waterflower.dialogs.MyPostActionDialog;
import ru.society.waterflower.dialogs.PostActionDialog;
import ru.society.waterflower.dialogs.PostDeleteDialog;
import ru.society.waterflower.dialogs.PostReportDialog;
import ru.society.waterflower.dialogs.PostShareDialog;
import ru.society.waterflower.model.Item;
import ru.society.waterflower.model.Profile;
import ru.society.waterflower.util.Api;
import ru.society.waterflower.util.CustomRequest;
import ru.society.waterflower.util.ItemInterface;

public class StreamFragment extends Fragment implements Constants, SwipeRefreshLayout.OnRefreshListener, ItemInterface {

    private static final String STATE_LIST = "State Adapter Data";

    private static final int PROFILE_NEW_POST = 4;

    RecyclerView mRecyclerView;

    SearchView searchView = null;

    TextView mMessage, mHeaderText;

    SwipeRefreshLayout mItemsContainer;

    FloatingActionButton mFabButton;

    LinearLayout mHeaderContainer;


    private ArrayList<Item> itemsList;
    private AdvancedItemListAdapter itemsAdapter;

    private int userId = 0;
    public int itemCount;
    private int itemId = 0;
    private int arrayLength = 0;
    private int searchId = 0;
    private Boolean loadingMore = false;
    private Boolean viewMore = false;
    private Boolean restore = false;
    private Boolean preload = true;
    ////////////////////////////////////////////////////////////////////////////////////////
    private int search_gender = -1, search_online = -1, search_photo = -1, preload_gender = -1;
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public String queryText, currentQuery, oldQuery;

    int pastVisiblesItems = 0, visibleItemCount = 0, totalItemCount = 0;

    public StreamFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        setHasOptionsMenu(true);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_stream, container, false);

        if (savedInstanceState != null) {

            itemsList = savedInstanceState.getParcelableArrayList(STATE_LIST);
            itemsAdapter = new AdvancedItemListAdapter(getActivity(), itemsList);

            currentQuery = queryText = savedInstanceState.getString("queryText");

            preload = savedInstanceState.getBoolean("preload");
            restore = savedInstanceState.getBoolean("restore");
            itemId = savedInstanceState.getInt("itemId");
            searchId = savedInstanceState.getInt("searchId");
            itemCount = savedInstanceState.getInt("itemCount");

        } else {

            itemsList = new ArrayList<>();
            itemsAdapter = new AdvancedItemListAdapter(getActivity(), itemsList);

            currentQuery = queryText = "";

            preload = true;
            restore = false;
            itemId = 0;
            searchId = 0;
            itemCount = 0;

        }

        mItemsContainer = (SwipeRefreshLayout) rootView.findViewById(R.id.container_items);
        mItemsContainer.setOnRefreshListener(this);

        mHeaderContainer = (LinearLayout) rootView.findViewById(R.id.container_header);

        mHeaderText = (TextView) rootView.findViewById(R.id.headerText);

        mMessage = (TextView) rootView.findViewById(R.id.message);

        mFabButton = (FloatingActionButton) rootView.findViewById(R.id.fabButton);
        mFabButton.setImageResource(R.drawable.ic_action_new);

        mFabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getActivity(), NewItemActivity.class);
                startActivityForResult(intent, STREAM_NEW_POST);
            }
        });

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        final GridLayoutManager mLayoutManager = new GridLayoutManager(getActivity(), 1);

        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.setAdapter(itemsAdapter);
        /****************************************************
        *************************************
        ******************************************************/
        itemsAdapter.setOnMoreButtonClickListener(new AdvancedItemListAdapter.OnItemMenuButtonClickListener() {

            @Override
            public void onItemClick(View v, Item obj, int actionId, int position) {

                switch (actionId) {

                    case R.id.action_repost: {

                        if (obj.getFromUserId() != App.getInstance().getId()) {

                            if (obj.getRePostFromUserId() != App.getInstance().getId()) {

                                repost(position);

                            } else {

                                Toast.makeText(getActivity(), getActivity().getString(R.string.msg_not_make_repost), Toast.LENGTH_SHORT).show();
                            }

                        } else {

                            Toast.makeText(getActivity(), getActivity().getString(R.string.msg_not_make_repost), Toast.LENGTH_SHORT).show();
                        }

                        break;
                    }

                    case R.id.action_share: {

                        onPostShare(position);

                        break;
                    }

                    case R.id.action_report: {

                        report(position);

                        break;
                    }

                    case R.id.action_remove: {

                        remove(position);

                        break;
                    }
                }
            }
        });



        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

//                if(dy > 0) {
//
//                    visibleItemCount = mLayoutManager.getChildCount();
//                    totalItemCount = mLayoutManager.getItemCount();
//                    pastVisiblesItems = mLayoutManager.findFirstVisibleItemPosition();
//
//                    if (!loadingMore) {
//
//                        if ((visibleItemCount + pastVisiblesItems) >= totalItemCount && (viewMore) && !(mItemsContainer.isRefreshing())) {
//
//                            loadingMore = true;
//                            Log.e("...", "Last Item Wow !");
//
//                            getItems();
//                        }
//                    }
//                }

                if (dy > 0) {

                    visibleItemCount = mLayoutManager.getChildCount();
                    totalItemCount = mLayoutManager.getItemCount();
                    pastVisiblesItems = mLayoutManager.findFirstVisibleItemPosition();

                    if (!loadingMore) {

                        if ((visibleItemCount + pastVisiblesItems) >= totalItemCount && (viewMore) && !(mItemsContainer.isRefreshing())) {

                            Log.e("...", "Last Item Wow !");

                            if (preload) {

                                loadingMore = true;

                                preload();

                            } else {

                                currentQuery = getCurrentQuery();

                                if (currentQuery.equals(oldQuery)) {

                                    loadingMore = true;

                                    search();
                                }
                            }
                        }
                    }
                }

            }
        });


        if (itemsAdapter.getItemCount() == 0) {

            showMessage(getText(R.string.label_empty_list).toString());

        } else {

            hideMessage();
        }
/***********************************************************************************************************
 *
 *
 *
 *
 ***********************************************************************************************************/

        if (queryText.length() == 0) {

            if (itemsAdapter.getItemCount() == 0) {

                showMessage(getString(R.string.label_market_search_start_screen_msg));
                mHeaderText.setVisibility(View.GONE);
                mHeaderContainer.setVisibility(View.GONE);

            } else {

                if (preload) {

                    mHeaderText.setVisibility(View.GONE);
                    mHeaderContainer.setVisibility(View.GONE);

                } else {

                    mHeaderText.setVisibility(View.VISIBLE);
                    mHeaderContainer.setVisibility(View.VISIBLE);
                    mHeaderText.setText(getText(R.string.label_market_search_results) + " " + Integer.toString(itemCount));
                }

                hideMessage();
            }

        } else {

            if (itemsAdapter.getItemCount() == 0) {

                showMessage(getString(R.string.label_search_results_error));
                mHeaderText.setVisibility(View.GONE);
                mHeaderContainer.setVisibility(View.GONE);

            } else {

                mHeaderText.setVisibility(View.VISIBLE);
                mHeaderContainer.setVisibility(View.VISIBLE);
                mHeaderText.setText(getText(R.string.label_market_search_results) + " " + Integer.toString(itemCount));

                hideMessage();
            }
        }

/************************************************************************************************************
 *
 *
 *
 *
 ********************************************************************************************************/
        if (!restore) {

//            showMessage(getText(R.string.msg_loading_2).toString());
//
//            getItems();
            if (preload) {///////////////////////////////////////////////////////////////////////////
                preload();////////////////////////////////////////////////////////////////////////////
            }
        }

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == STREAM_NEW_POST && resultCode == getActivity().RESULT_OK && null != data) {

            itemId = 0;
            //getItems();
            if (App.getInstance().isConnected() && currentQuery.length() != 0) {

                searchId = 0;
                search();

            } else {

                itemId = 0;
                preload();
            }

        } else if (requestCode == ITEM_EDIT && resultCode == getActivity().RESULT_OK) {

            int position = data.getIntExtra("position", 0);

            Item item = itemsList.get(position);

            item.setPost(data.getStringExtra("post"));
            item.setImgUrl(data.getStringExtra("imgUrl"));

            itemsAdapter.notifyDataSetChanged();

        } else if (requestCode == ITEM_REPOST && resultCode == getActivity().RESULT_OK) {

            int position = data.getIntExtra("position", 0);

            Item item = itemsList.get(position);

            item.setMyRePost(true);
            item.setRePostsCount(item.getRePostsCount() + 1);

            itemsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRefresh() {
//////////////////// modify section     //////////////////////
//        if (App.getInstance().isConnected()) {
//
//            itemId = 0;
//            getItems();
//
//        } else {
//
//            mItemsContainer.setRefreshing(false);
//        }

        currentQuery = queryText;

        currentQuery = currentQuery.trim();

        if (App.getInstance().isConnected() && currentQuery.length() != 0) {

            searchId = 0;
            search();

        } else {

            itemId = 0;
            preload();
        }
    }

    public String getCurrentQuery() {

        String searchText = searchView.getQuery().toString();
        searchText = searchText.trim();

        return searchText;
    }


    public void searchStart() {

        preload = false;

        currentQuery = getCurrentQuery();

        if (App.getInstance().isConnected()) {

            searchId = 0;
            search();

        } else {

            Toast.makeText(getActivity(), getText(R.string.msg_network_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        outState.putString("queryText", queryText);
        outState.putBoolean("restore", true);
        outState.putBoolean("preload", preload);
        outState.putInt("itemId", itemId);
        outState.putInt("searchId", searchId);
        outState.putInt("itemCount", itemCount);
        outState.putParcelableArrayList(STATE_LIST, itemsList);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_market_search, menu);

        MenuItem searchItem = menu.findItem(R.id.options_menu_main_search);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);

        if (searchItem != null) {

            searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        }

        if (searchView != null) {

            searchView.setQuery(queryText, false);

            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
            searchView.setIconifiedByDefault(false);
            searchView.setIconified(false);

            SearchView.SearchAutoComplete searchAutoComplete = (SearchView.SearchAutoComplete) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
            searchAutoComplete.setHint(getText(R.string.placeholder_search));
            searchAutoComplete.setHintTextColor(getResources().getColor(R.color.white));

            searchView.clearFocus();

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {

                    queryText = newText;

                    return false;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {

                    queryText = query;
                    searchStart();

                    return false;
                }
            });
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    /*********************************************************************************
     *
     *******************************************************************************/

    public void search() {

        mItemsContainer.setRefreshing(true);

        CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_STREAM_GET, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        if (!isAdded() || getActivity() == null) {

                            Log.e("ERROR", "StreamFragment Not Added to Activity");

                            return;
                        }

                        if (!loadingMore) {

                            itemsList.clear();
                        }

                        try {

                            arrayLength = 0;

                            if (!response.getBoolean("error")) {

                                itemCount = response.getInt("itemCount");
                                oldQuery = response.getString("query");
                                itemId = response.getInt("itemId");

                                if (response.has("items")) {

                                    JSONArray itemsArray = response.getJSONArray("items");

                                    arrayLength = itemsArray.length();

                                    if (arrayLength > 0) {

                                        for (int i = 0; i < itemsArray.length(); i++) {

                                            JSONObject itemObj = (JSONObject) itemsArray.get(i);

                                            Item item = new Item(itemObj);

                                            item.setAd(0);

                                            itemsList.add(item);

                                            // Ad after first item
                                            if (i == MY_AD_AFTER_ITEM_NUMBER && App.getInstance().getAdmob() == ENABLED) {

                                                Item ad = new Item(itemObj);

                                                ad.setAd(1);

                                                itemsList.add(ad);
                                            }
                                        }
                                    }
                                }
                            }

                        } catch (JSONException e) {

                            e.printStackTrace();

                        } finally {

                            loadingComplete();

                            Log.d("STREAM success", response.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                if (!isAdded() || getActivity() == null) {

                    Log.e("ERROR", "StreamFragment Not Added to Activity");

                    return;
                }

                loadingComplete();

                Log.e("STREAM error", error.toString());
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountId", Long.toString(App.getInstance().getId()));
                params.put("accessToken", App.getInstance().getAccessToken());
                params.put("itemId", Integer.toString(searchId));
                params.put("language", "en");
                params.put("query", currentQuery);

                return params;
            }
        };

        App.getInstance().addToRequestQueue(jsonReq);
    }
    /**************************************************************************************************
     *
     *********************************************************************************************/

    public void preload() {

        mItemsContainer.setRefreshing(true);

        CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_STREAM_GET, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        if (!isAdded() || getActivity() == null) {

                            Log.e("ERROR", "StreamFragment Not Added to Activity");

                            return;
                        }

                        if (!loadingMore) {

                            itemsList.clear();
                        }

                        try {

                            arrayLength = 0;

                            if (!response.getBoolean("error")) {

                                itemId = response.getInt("itemId");

                                if (response.has("items")) {

                                    JSONArray itemsArray = response.getJSONArray("items");

                                    arrayLength = itemsArray.length();

                                    if (arrayLength > 0) {

                                        for (int i = 0; i < itemsArray.length(); i++) {

                                            JSONObject itemObj = (JSONObject) itemsArray.get(i);

                                            Item item = new Item(itemObj);

                                            item.setAd(0);

                                            itemsList.add(item);

                                            // Ad after first item
                                            if (i == MY_AD_AFTER_ITEM_NUMBER && App.getInstance().getAdmob() == ENABLED) {

                                                Item ad = new Item(itemObj);

                                                ad.setAd(1);

                                                itemsList.add(ad);
                                            }
                                        }
                                    }
                                }
                            }

                        } catch (JSONException e) {

                            e.printStackTrace();

                        } finally {

                            loadingComplete();

                            Log.d("STREAM success", response.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                if (!isAdded() || getActivity() == null) {

                    Log.e("ERROR", "StreamFragment Not Added to Activity");

                    return;
                }

                loadingComplete();

                Log.e("STREAM error", error.toString());
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountId", Long.toString(App.getInstance().getId()));
                params.put("accessToken", App.getInstance().getAccessToken());
                params.put("itemId", Integer.toString(itemId));
                params.put("language", "en");

                return params;
            }
        };

        App.getInstance().addToRequestQueue(jsonReq);
    }

    public void loadingComplete() {

        if (arrayLength == LIST_ITEMS) {

            viewMore = true;

        } else {

            viewMore = false;
        }

        itemsAdapter.notifyDataSetChanged();

        if (itemsAdapter.getItemCount() == 0) {

            if (StreamFragment.this.isVisible()) {

                showMessage(getText(R.string.label_empty_list).toString());
            }

        } else {

            hideMessage();
        }

        loadingMore = false;
        mItemsContainer.setRefreshing(false);
    }

    public void report(int position) {

        android.app.FragmentManager fm = getActivity().getFragmentManager();

        PostReportDialog alert = new PostReportDialog();

        Bundle b  = new Bundle();
        b.putInt("position", position);
        b.putInt("reason", 0);

        alert.setArguments(b);
        alert.show(fm, "alert_dialog_post_report");
    }

    public void onPostReport(int position, int reasonId) {

        final Item item = itemsList.get(position);

        if (App.getInstance().isConnected()) {

            Api api = new Api(getActivity());

            api.postReport(item.getId(), reasonId);

        } else {

            Toast.makeText(getActivity(), getText(R.string.msg_network_error), Toast.LENGTH_SHORT).show();
        }
    }

    public void remove(int position) {

        android.app.FragmentManager fm = getActivity().getFragmentManager();

        PostDeleteDialog alert = new PostDeleteDialog();

        Bundle b  = new Bundle();
        b.putInt("position", position);

        alert.setArguments(b);
        alert.show(fm, "alert_dialog_post_delete");
    }

    public void onPostDelete(int position) {

        final Item item = itemsList.get(position);

        itemsList.remove(position);
        itemsAdapter.notifyDataSetChanged();

        if (itemsAdapter.getItemCount() == 0) {

            showMessage(getText(R.string.label_empty_list).toString());

        } else {

            hideMessage();
        }

        if (App.getInstance().isConnected()) {

            Api api = new Api(getActivity());

            api.postDelete(item.getId());

        } else {

            Toast.makeText(getActivity(), getText(R.string.msg_network_error), Toast.LENGTH_SHORT).show();
        }
    }

    public void onPostRemove(final int position) {

        /** Getting the fragment manager */
        android.app.FragmentManager fm = getActivity().getFragmentManager();

        /** Instantiating the DialogFragment class */
        PostDeleteDialog alert = new PostDeleteDialog();

        /** Creating a bundle object to store the selected item's index */
        Bundle b  = new Bundle();

        /** Storing the selected item's index in the bundle object */
        b.putInt("position", position);

        /** Setting the bundle object to the dialog fragment object */
        alert.setArguments(b);

        /** Creating the dialog fragment object, which will in turn open the alert dialog window */

        alert.show(fm, "alert_dialog_post_delete");
    }

    public void onPostShare(final int position) {

        final Item item = itemsList.get(position);

        Api api = new Api(getActivity());
        api.postShare(item);
    }

    public void onPostEdit(final int position) {

        Item item = itemsList.get(position);

        Intent i = new Intent(getActivity(), EditItemActivity.class);
        i.putExtra("position", position);
        i.putExtra("postId", item.getId());
        i.putExtra("post", item.getPost());
        i.putExtra("imgUrl", item.getImgUrl());
        startActivityForResult(i, ITEM_EDIT);
    }

    public void onPostCopyLink(final int position) {

        final Item item = itemsList.get(position);

        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(getActivity().CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("post url", item.getLink());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getActivity(), getText(R.string.msg_post_link_copied), Toast.LENGTH_SHORT).show();
    }

    public void action(int position) {

        final Item item = itemsList.get(position);

        if (item.getFromUserId() == App.getInstance().getId()) {

            /** Getting the fragment manager */
            android.app.FragmentManager fm = getActivity().getFragmentManager();

            /** Instantiating the DialogFragment class */
            MyPostActionDialog alert = new MyPostActionDialog();

            /** Creating a bundle object to store the selected item's index */
            Bundle b  = new Bundle();

            /** Storing the selected item's index in the bundle object */
            b.putInt("position", position);

            /** Setting the bundle object to the dialog fragment object */
            alert.setArguments(b);

            /** Creating the dialog fragment object, which will in turn open the alert dialog window */

            alert.show(fm, "alert_my_post_action");

        } else {

            /** Getting the fragment manager */
            android.app.FragmentManager fm = getActivity().getFragmentManager();

            /** Instantiating the DialogFragment class */
            PostActionDialog alert = new PostActionDialog();

            /** Creating a bundle object to store the selected item's index */
            Bundle b  = new Bundle();

            /** Storing the selected item's index in the bundle object */
            b.putInt("position", position);

            /** Setting the bundle object to the dialog fragment object */
            alert.setArguments(b);

            /** Creating the dialog fragment object, which will in turn open the alert dialog window */

            alert.show(fm, "alert_post_action");
        }
    }

    public void repost(int position) {

        android.app.FragmentManager fm = getActivity().getFragmentManager();

        PostShareDialog alert = new PostShareDialog();

        Bundle b  = new Bundle();

        b.putInt("position", position);

        alert.setArguments(b);

        alert.show(fm, "alert_repost_action");
    }

    public void onPostRePost(final int position) {

        Item item = itemsList.get(position);

        long rePostId = item.getId();

        if (item.getRePostId() != 0) {

            rePostId = item.getRePostId();
        }

        Intent i = new Intent(getActivity(), RePostItemActivity.class);
        i.putExtra("position", position);
        i.putExtra("rePostId", rePostId);
        startActivityForResult(i, ITEM_REPOST);
    }

    public void showMessage(String message) {

        mMessage.setText(message);
        mMessage.setVisibility(View.VISIBLE);
    }

    public void hideMessage() {

        mMessage.setVisibility(View.GONE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}