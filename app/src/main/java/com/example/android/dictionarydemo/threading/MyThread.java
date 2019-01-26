package com.example.android.dictionarydemo.threading;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.android.dictionarydemo.model.Word;
import com.example.android.dictionarydemo.persistence.AppDatabase;
import com.example.android.dictionarydemo.util.Constants;

import java.util.ArrayList;
import java.util.List;

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
import static com.example.android.dictionarydemo.util.Constants.WORD_UPDATE_FAIL;
import static com.example.android.dictionarydemo.util.Constants.WORD_UPDATE_SUCCESS;

public class MyThread extends Thread {

    private static final String TAG = "MyThread";


    private MyThreadHandler myThreadHandler = null;
    private Handler mMainThreadHandler = null;
    private boolean isRunning = false;
    private AppDatabase mDb; // the object that I used to access database


    public MyThread(Context context, Handler mMainThreadHandler) {
        this.mMainThreadHandler = mMainThreadHandler;
        isRunning = true; // that means thread has started
        mDb = AppDatabase.getDatabase(context);
    }

    @Override
    public void run() {
        if (isRunning) {
            Looper.prepare();
            myThreadHandler = new MyThreadHandler(Looper.myLooper());
            Looper.loop();
        }
    }

    // quit from thread
    public void quitThread() {
        isRunning = false;
        mMainThreadHandler = null;
    }

    public void sendMessageToBackgroundThread(Message message) {
        while (true) {
            try {
                myThreadHandler.sendMessage(message);
                break;
            } catch (NullPointerException e) {
                Log.e(TAG, "sendMessageToBackgroundThread: Null pointer: " + e.getMessage());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private long[] saveNewWord(Word word) {
        long[] returnValue = mDb.wordDataDao().insertWords(word);
        if (returnValue.length > 0) {
            Log.d(TAG, "saveNewWord: return value: " + returnValue.toString());
        }
        return returnValue;
    }


    private List<Word> retrieveWords(String title) {
        return mDb.wordDataDao().getWords(title);
    }

    private int updateWord(Word word) {
        return mDb.wordDataDao().updateWord(word.getTitle(), word.getContent(), word.getTimestamp(), word.getUid());
    }

    private int deleteWord(Word word) {
        return mDb.wordDataDao().delete(word);
    }


    private class MyThreadHandler extends Handler {

        public MyThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WORD_INSERT_NEW: {
                    Log.d(TAG, "handleMessage: saving word on thread: " + Thread.currentThread().getName());
                    // execute code required to insert new word
                    Word word = msg.getData().getParcelable("word_new");
                    Message message = null;
                    if (saveNewWord(word).length > 0) {
                        message = Message.obtain(mMainThreadHandler, WORD_INSERT_SUCCESS);
                    } else {
                        message = Message.obtain(null, WORD_INSERT_FAIL);
                    }
                    mMainThreadHandler.sendMessage(message);
                    break;
                }
                case WORD_UPDATE: {
                    Log.d(TAG, "handleMessage: updating word on thread: " + Thread.currentThread().getName());
                    Word word = msg.getData().getParcelable("word_update");
                    Message message = null;
                    int updateInt = updateWord(word);
                    if (updateInt > 0) {
                        message = Message.obtain(mMainThreadHandler, WORD_UPDATE_SUCCESS);
                    } else {
                        message = Message.obtain(mMainThreadHandler, WORD_UPDATE_FAIL);
                    }
                    mMainThreadHandler.sendMessage(message);
                    break;
                }
                case WORDS_RETRIEVE: {
                    Log.d(TAG, "handleMessage: retrieving word on thread: " + Thread.currentThread().getName());
                    String title = msg.getData().getString("title");
                    ArrayList<Word> words = new ArrayList<>(retrieveWords(title));
                    Message message = null;
                    if (words.size() > 0) {
                        message = Message.obtain(null, WORDS_RETRIEVE_SUCCESS);
                        Bundle bundle = new Bundle();
                        bundle.putParcelableArrayList("words_retrieve", words);
                        message.setData(bundle);
                    } else {
                        message = Message.obtain(null, WORDS_RETRIEVE_FAIL);
                    }
                    mMainThreadHandler.sendMessage(message);
                    break;
                }
                case WORD_DELETE: {
                    Log.d(TAG, "handleMessage: deleting word on thread: " + Thread.currentThread().getName());
                    Word word = msg.getData().getParcelable("word_delete");
                    Message message = null;
                    if (deleteWord(word) > 0) {
                        message = Message.obtain(mMainThreadHandler, WORD_DELETE_SUCCESS);
                    } else {
                        message = Message.obtain(mMainThreadHandler, WORD_DELETE_FAIL);
                    }
                    mMainThreadHandler.sendMessage(message);
                    break;
                }
            }
        }
    }


}
