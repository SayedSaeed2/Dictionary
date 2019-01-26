package com.example.android.dictionarydemo;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.android.dictionarydemo.adapter.WordsRecyclerAdapter;
import com.example.android.dictionarydemo.model.Word;
import com.example.android.dictionarydemo.threading.MyThread;
import com.example.android.dictionarydemo.util.Constants;
import com.example.android.dictionarydemo.util.FakeData;
import com.example.android.dictionarydemo.util.VerticalSpacingItemDecorator;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.example.android.dictionarydemo.util.Constants.WORDS_RETRIEVE;
import static com.example.android.dictionarydemo.util.Constants.WORDS_RETRIEVE_FAIL;
import static com.example.android.dictionarydemo.util.Constants.WORDS_RETRIEVE_SUCCESS;
import static com.example.android.dictionarydemo.util.Constants.WORD_DELETE;
import static com.example.android.dictionarydemo.util.Constants.WORD_DELETE_FAIL;
import static com.example.android.dictionarydemo.util.Constants.WORD_DELETE_SUCCESS;
import static com.example.android.dictionarydemo.util.Constants.WORD_INSERT_FAIL;
import static com.example.android.dictionarydemo.util.Constants.WORD_INSERT_NEW;
import static com.example.android.dictionarydemo.util.Constants.WORD_INSERT_SUCCESS;
import static com.example.android.dictionarydemo.util.Constants.WORD_UPDATE;

public class DictionaryActivity extends AppCompatActivity implements
        WordsRecyclerAdapter.OnWordListener,
        View.OnClickListener,
        SwipeRefreshLayout.OnRefreshListener,
        Handler.Callback {

    private static final String TAG = "DictionaryActivity";

    //ui components
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefresh;

    //vars
    private ArrayList<Word> mWords = new ArrayList<>();
    private WordsRecyclerAdapter mWordRecyclerAdapter;
    private FloatingActionButton mFab;
    private String mSearchQuery = "";
    private MyThread mMyThread;
    private Handler mMainThreadHandler = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        restoreInstanceState(savedInstanceState);

        setContentView(R.layout.activity_dictionary);
        mRecyclerView = findViewById(R.id.recycler_view);
        mFab = findViewById(R.id.fab);
        mSwipeRefresh = findViewById(R.id.swipe_refresh);

        mFab.setOnClickListener(this);
        mSwipeRefresh.setOnRefreshListener(this);

        mMainThreadHandler = new Handler(this);

        setupRecyclerView();
    }


    private void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mWords = savedInstanceState.getParcelableArrayList("words");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("words", mWords);
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: called.");
        super.onStart();
//        retrieveWords("");
        mMyThread = new MyThread(this, mMainThreadHandler);
        mMyThread.start();
    }

    private void sendTestMessage() {
        Log.d(TAG, "sendTestMessage: sending test message: " + Thread.currentThread().getName());
        Message message = Message.obtain(null, Constants.WORDS_RETRIEVE);
        mMyThread.sendMessageToBackgroundThread(message);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: called.");
        super.onStop();
        mMyThread.quitThread();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mSearchQuery.length() > 2) {
            onRefresh();
        }
//        sendTestMessage();
    }

    private void retrieveWords(String title) {
        Log.d(TAG, "retrieveWords: called.");
//        mWords.addAll(Arrays.asList(FakeData.words));

        Message message = Message.obtain(null, WORDS_RETRIEVE);
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        message.setData(bundle);
        mMyThread.sendMessageToBackgroundThread(message);
    }


    public void deleteWord(Word word) {
        Log.d(TAG, "deleteWord: called.");
        mWords.remove(word);
        mWordRecyclerAdapter.getFilteredWords().remove(word);
        mWordRecyclerAdapter.notifyDataSetChanged();


        Message message = Message.obtain(null, WORD_DELETE);
        Bundle bundle = new Bundle();
        bundle.putParcelable("word_delete", word);
        message.setData(bundle);
        mMyThread.sendMessageToBackgroundThread(message);

    }


    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView: called.");
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        VerticalSpacingItemDecorator itemDecorator = new VerticalSpacingItemDecorator(10);
        mRecyclerView.addItemDecoration(itemDecorator);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView);
        mWordRecyclerAdapter = new WordsRecyclerAdapter(mWords, this);
        mRecyclerView.setAdapter(mWordRecyclerAdapter);
    }

    @Override
    public void onWordClick(int position) {
        Intent intent = new Intent(this, EditWordActivity.class);
        intent.putExtra("selected_word", mWords.get(position));
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.fab: {
                Intent intent = new Intent(this, EditWordActivity.class);
                startActivity(intent);
                break;
            }
        }
    }


    // to delete the word when swiping it
    ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            deleteWord(mWords.get(mWords.indexOf(mWordRecyclerAdapter.getFilteredWords().get(viewHolder.getAdapterPosition()))));
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dictionary_activity_actions, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView =
                (SearchView) searchItem.getActionView();

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // filter recycler view when query submitted
                if (query.length() > 2) {
                    mSearchQuery = query;
                    retrieveWords(mSearchQuery);
                } else {
                    clearWords();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
                if (query.length() > 2) {
                    mSearchQuery = query;
                    retrieveWords(mSearchQuery);
                } else {
                    clearWords();
                }
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    private void clearWords() {
        if (mWords != null) {
            if (mWords.size() > 0) {
                mWords.clear();
            }
        }
        mWordRecyclerAdapter.getFilter().filter(mSearchQuery);
    }

    @Override
    public void onRefresh() {
        retrieveWords(mSearchQuery);
        mSwipeRefresh.setRefreshing(false);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case WORDS_RETRIEVE_SUCCESS: {
                Log.d(TAG, "handleMessage: successfully retrieved words. This is from thread: : " + Thread.currentThread().getName());
                // execute code required to insert new word

                clearWords();

                ArrayList<Word> words = new ArrayList<>(msg.getData().<Word>getParcelableArrayList("words_retrieve"));
                mWords.addAll(words);
                mWordRecyclerAdapter.getFilter().filter(mSearchQuery);
                break;
            }
            case WORDS_RETRIEVE_FAIL: {
                Log.d(TAG, "handleMessage: unable to  retrieve words. This is from thread: : " + Thread.currentThread().getName());

                clearWords();

                break;
            }
            case WORD_INSERT_SUCCESS: {
                Log.d(TAG, "handleMessage: successfully inserted new word. This is from thread: : " + Thread.currentThread().getName());
                break;
            }
            case WORD_INSERT_FAIL: {
                Log.d(TAG, "handleMessage: unable to  insert new word: " + Thread.currentThread().getName());
                break;
            }

            case WORD_DELETE_SUCCESS: {
                Log.d(TAG, "handleMessage: successfully deleted a word: " + Thread.currentThread().getName());
                break;
            }
            case WORD_DELETE_FAIL: {
                Log.d(TAG, "handleMessage: unable to  delete a word: " + Thread.currentThread().getName());
                break;
            }
        }
        return true;
    }
}


