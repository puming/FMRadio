package com.android.fmradio;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.fmradio.views.XToolBar;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class FmRecordListActivity extends Activity {
    private static final String TAG = "FmRecordListActivity";

    String dir = FmUtils.getDefaultStoragePath() + File.separator
            + FmRecorder.FM_RECORD_FOLDER;
    private String mPath;
    private RecordBaseAdapter mAdapter;
    private List<SoundRecorder> mSoundRecorders;
    private ListView mListView;
    private TextView mTextView;
    private XToolBar mToolbar;
    private MediaPlayer mMediaPlayer;
    private AudioManager mAudioManager;

    private boolean mIsShowToolbarMenu = false;//默认不显示编辑按钮，长按item才显示编辑按钮
    private HashSet<SoundRecorder> mDelFiles;
    private int mMediaPlayerCurrentIndex = 0;
    private boolean checkedListen = true;
    private EditText mEditText;
    private AlertDialog mDialog;

    private AdapterView.OnItemLongClickListener mOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (mMediaPlayer.isPlaying()) {
                return true;
            }
            if (!mIsShowToolbarMenu) {
                mIsShowToolbarMenu = true;
                if (mDelFiles == null) {
                    mDelFiles = new HashSet<SoundRecorder>();
                }
                mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                mListView.setItemChecked(position, true);
                mMediaPlayerCurrentIndex = position;
                mDelFiles.add(mSoundRecorders.get(position));
                if (mSoundRecorders.size() == 1) {
                    mToolbar.setCheckBoxChecked(true);
                }

                if(mDelFiles.size()==1){
                    mToolbar.setRenameEnabled(true);
                }

                if(mDelFiles.size()>0){
                    mToolbar.setDeleteEnable(true);
                }
                mToolbar.showToolBarMenu();
                mToolbar.setTitle(R.string.selected_title);
            } else {
                cancelMultiple();
            }
            mAdapter.notifyDataSetChanged();
            return true;
        }
    };

    private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mIsShowToolbarMenu) {
                if (mListView.isItemChecked(position)) {
                    mDelFiles.add(mSoundRecorders.get(position));
                } else {
                    mDelFiles.remove(mSoundRecorders.get(position));
                }

                if (mListView.getCheckedItemCount() == mListView.getCount()) {
                    mToolbar.setCheckBoxChecked(true);
                } else {
                    checkedListen = false;
                    mToolbar.setCheckBoxChecked(false);
                    checkedListen = true;
                }

                if (mListView.getCheckedItemCount() == 1) {
                    mToolbar.setRenameEnabled(true);
                    SparseBooleanArray checkStates = mListView.getCheckedItemPositions();
                    for (int i = 0; i < checkStates.size(); i++) {
                        if (mListView.isItemChecked(checkStates.keyAt(i))) {
                            Log.e(TAG, i + "checked");
                            mMediaPlayerCurrentIndex = checkStates.keyAt(i);
                            break;
                        }
                    }
                    int size = mListView.getCheckedItemPositions().size();
                } else {
                    mToolbar.setRenameEnabled(false);
                }

                if (mListView.getCheckedItemCount() > 0) {
                    mToolbar.setDeleteEnable(true);
                    SparseBooleanArray mcheckStates = mListView.getCheckedItemPositions();
                    for (int i = 0; i < mcheckStates.size(); i++) {
                        if (mListView.isItemChecked(mcheckStates.keyAt(i))) {
                            Log.e(TAG, i + "checked");
                            mMediaPlayerCurrentIndex = mcheckStates.keyAt(i);
                            break;
                        }
                    }
                } else {
                    cancelMultiple();
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    private XToolBar.OnXToolbarClickListener mOnXToolbarClickListener = new XToolBar.OnXToolbarClickListener() {
        @Override
        public void deleteClick() {
            createDeleteDialog();
        }

        @Override
        public void renameClick() {
            createRenameDialog();
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (checkedListen) {
                if (isChecked) {
                    mDelFiles.clear();
                    for (int i = 0; i < mAdapter.getCount(); i++) {
                        mListView.setItemChecked(i, true);
                        mDelFiles.add(mSoundRecorders.get(i));
                    }
                    if (mDelFiles.size() > 1) {
                        mToolbar.setRenameEnabled(false);
                    }
                    if (mDelFiles.size() > 0) {
                        mToolbar.setDeleteEnable(true);
                    } else {
                        mToolbar.setDeleteEnable(false);
                    }
                } else {
                    mDelFiles.clear();
                    mListView.clearChoices();
                    mToolbar.setDeleteEnable(false);
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fmrecord_list);
        initComponent();
        mMediaPlayer = new MediaPlayer();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mSoundRecorders = new ArrayList<>();
//        initData();
        registerClickListener();
    }

    private void initComponent() {
        mListView = (ListView) findViewById(R.id.listView);
        mTextView = (TextView) findViewById(R.id.tv_empty);
        mToolbar = (XToolBar) findViewById(R.id.toolbar);
        setActionBar(mToolbar);
        mToolbar.setNavigationIcon(R.drawable.icon_btn_back_rest);
        mToolbar.setTitle(R.string.record_title);
    }

    private void initData() {
        if (FmUtils.getSoundRecorders(dir) != null && FmUtils.getSoundRecorders(dir).size() > 0) {
            mSoundRecorders = FmUtils.getSoundRecorders(dir);
            mPath = mSoundRecorders.get(0).getPath();
            initPlay(mPath);
        }
        mAdapter = new RecordBaseAdapter(FmRecordListActivity.this, mSoundRecorders);
        mListView.setAdapter(mAdapter);
        mListView.setEmptyView(mTextView);
    }

    private void registerClickListener() {
        mListView.setOnItemLongClickListener(mOnItemLongClickListener);
        mListView.setOnItemClickListener(mOnItemClickListener);
        mToolbar.setOnXToolbarClickListener(mOnXToolbarClickListener);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * Play or pause
     *
     * @param position
     */
    public void onPlayClick(int position) {
        if (mIsShowToolbarMenu) {
            return;
        }
        mAdapter.setCurrentPosition(position);
        mAdapter.setIsPlay(mMediaPlayer.isPlaying());
        mAdapter.notifyDataSetChanged();
        play(mSoundRecorders.get(position).getPath());
    }

    private void cancelMultiple() {
        mIsShowToolbarMenu = false;
        mListView.clearChoices();
        mToolbar.hideToolBarMenu();
        mToolbar.setTitle(R.string.record_title);
        mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        mDelFiles.clear();
    }

    private View renameDialogView(String deviceName) {
        View view = LayoutInflater.from(FmRecordListActivity.this).inflate(R.layout.rename_edittext, null);
        mEditText = (EditText) view.findViewById(R.id.et_rename);
        mEditText.setText(deviceName);
        Editable editable = mEditText.getText();
        Selection.setSelection(editable, editable.length());

        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (v.length() != 0 && !(v.getText().toString().trim().isEmpty())) {
                    }
                    mDialog.dismiss();
                    return true;    // action handled
                } else {
                    return false;   // not handled
                }
            }
        });
        return view;
    }

    private void createRenameDialog() {
        final String fileName = mSoundRecorders.get(mMediaPlayerCurrentIndex).getFileName();
        final String filePath = mSoundRecorders.get(mMediaPlayerCurrentIndex).getPath();
        final String suffix = filePath.substring(filePath.lastIndexOf('.'));
        mDialog = new AlertDialog.Builder(FmRecordListActivity.this)
                .setMessage(R.string.recording_name)
                .setView(renameDialogView(fileName))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    private String deviceName;

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deviceName = mEditText.getText().toString().trim();
                        if (deviceName.equals("")) {

                        } else {
                            mSoundRecorders.get(mMediaPlayerCurrentIndex).setFileName(deviceName);
                            int i = FmUtils.renameFile(dir, fileName + ".3gpp", deviceName + ".3gpp");
                            mSoundRecorders=FmUtils.getSoundRecorders(dir);
                            mAdapter.setRecords(mSoundRecorders);
                            mListView.clearChoices();
                            mToolbar.setDeleteEnable(false);
                            mToolbar.setRenameEnabled(false);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Field mField = mDialog.getClass().getSuperclass().getDeclaredField("mShowing");
                            mField.setAccessible(true);
                            mField.set(mDialog, true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mDialog.dismiss();
                    }
                }).create();

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s,
                                      int start, int before, int count) {
                // TODO Auto-generated method stub
                Log.e("on", "onTextChanged");
            }

            @Override
            public void beforeTextChanged(CharSequence s,
                                          int start, int count, int after) {
                // TODO Auto-generated method stub
                Log.e("on", "beforeTextChanged");

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
                Log.e("on", "afterTextChanged");
                if (s.length() == 0) {
                    Button possitiveBotton = ((AlertDialog) mDialog).getButton(AlertDialog.BUTTON_POSITIVE);
                    possitiveBotton.setTextColor(Color.parseColor("#45605e"));
                    try {
                        Field mField = mDialog.getClass().getSuperclass().getDeclaredField("mShowing");
                        mField.setAccessible(true);
                        mField.set(mDialog, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Button possitiveBotton = ((AlertDialog) mDialog).getButton(AlertDialog.BUTTON_POSITIVE);
                    possitiveBotton.setTextColor(Color.parseColor("#80cbc4"));
                    try {
                        Field mField = mDialog.getClass().getSuperclass().getDeclaredField("mShowing");
                        mField.setAccessible(true);
                        mField.set(mDialog, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        mDialog.show();
    }

    public void createDeleteDialog(){
        AlertDialog deleteDialog = new AlertDialog.Builder(FmRecordListActivity.this)
                .setTitle(R.string.delete_hint)
                .setPositiveButton(R.string.btn_delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Object[] array = mDelFiles.toArray();
                        for (int i = 0; i < array.length; i++) {
                            String deleteItemPath = ((SoundRecorder) array[i]).getPath();
                            Log.e(TAG, array.length + "--------------" + deleteItemPath);
                            File mfile = new File(deleteItemPath);
                            mfile.delete();
                        }
                        mSoundRecorders.removeAll(mDelFiles);
                        mAdapter.setRecords(mSoundRecorders);
                        mListView.invalidate();
                        mListView.clearChoices();

                        mToolbar.setDeleteEnable(false);
                        mToolbar.setRenameEnabled(false);
                        if (mListView.getCount() == 0) {
                            mToolbar.hideToolBarMenu();
                            mToolbar.setTitle(R.string.record_title);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        deleteDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestPermission();
    }

    private void requestPermission() {
        int checkSelfPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 111);
        } else {
            initData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mListView!=null){
            mListView.clearChoices();
        }

        if (mDelFiles != null) {
            mDelFiles.clear();
        }

        if (mToolbar != null&&mIsShowToolbarMenu) {
            mIsShowToolbarMenu=false;
            mToolbar.hideToolBarMenu();
            mToolbar.setTitle(R.string.record_title);
        }
    }

    @Override
    protected void onDestroy() {
        mMediaPlayer.release();
        if (mMediaPlayer != null) {
            mMediaPlayer = null;
        }
        mAdapter.notifyStop();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 111) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initData();
            } else {
                finish();
            }
        }
    }

    /**
     * @param path
     * @return
     */
    public void initPlay(String path) {
        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param path
     */
    public void play(String path) {
        if (mPath == path) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mAdapter.notifyStop();
            } else {
                mMediaPlayer.start();
                mAdapter.notifyUpdate();
            }
        } else {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            initPlay(mPath = path);
            mMediaPlayer.start();
            mAdapter.notifyUpdate();
        }
    }

    /**
     * Created by puming1 on 4/26/16.
     */
    class RecordBaseAdapter extends BaseAdapter {
        private static final String TAG = "RecordBaseAdapter";

        private int mCurrentPosition = -1;
        private boolean mIsPlay = false;
        private int mCurrentTime;

        private Context mContext;
        private List<SoundRecorder> mSoundRecorders;

        private ViewHolder viewHolder = null;
        private ImageView mPlaySwitch;
        private TextView mForTime;
        private TextView mAllTime;
        private SeekBar mSeekBar;

        private Handler mHandler = new Handler();

        private Thread mThread = new Thread() {
            @Override
            public void run() {
                updateUI(getCurrentTime());
                mHandler.postDelayed(mThread, 100);
                super.run();
            }
        };

        /**
         * @return
         */
        private int getCurrentTime() {
            if (mMediaPlayer.isPlaying()) {
                return mMediaPlayer.getCurrentPosition();
            } else {
                return mCurrentTime;
            }
        }

        /**
         * @param time
         * @return
         */
        private String formatTime(int time) {
            int secondSum = time / 1000;
            int minute = secondSum / 60;
            int second = secondSum % 60;
            String format = "";
            if (minute < 10) {
                format = "0";
            }
            format = format + minute + ":";
            if (second < 10) {
                format = format + "0";
            }
            format = format + second;
            return format;
        }

        /**
         * @param currentPosition
         */
        public void setCurrentPosition(int currentPosition) {
            this.mCurrentPosition = currentPosition;
        }

        /**
         * @param isPlay
         */
        public void setIsPlay(boolean isPlay) {
            this.mIsPlay = isPlay;
        }

        /**
         * @param currentProgress
         */
        private void updateUI(int currentProgress) {
            mForTime.setText(formatTime(currentProgress));
            mSeekBar.setProgress(currentProgress);
        }

        /**
         * notify update ui
         */
        public void notifyUpdate() {
            int firstVisiblePosition = mListView.getFirstVisiblePosition();
            View currentView = mListView.getChildAt(mCurrentPosition - firstVisiblePosition);
            if (currentView == null) {
                Log.e(TAG, "currentView==null");
                return;
            }
            mPlaySwitch = (ImageView) currentView.findViewById(R.id.play_switch);
            mForTime = (TextView) currentView.findViewById(R.id.for_time);
            mAllTime = (TextView) currentView.findViewById(R.id.all_time);
            mSeekBar = (SeekBar) currentView.findViewById(R.id.seekBar);

            mCurrentTime = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    mPlaySwitch.setImageResource(R.drawable.icon_btn_play_rest);
                    notifyStop();
                }
            });
            mHandler.post(mThread);
        }

        /**
         * notify stop
         */
        public void notifyStop() {
            mHandler.removeCallbacks(mThread);
            mCurrentTime = 0;
            if (mSeekBar != null) {
                mSeekBar.setProgress(mCurrentTime);
            }
            if (mForTime != null) {
                mForTime.setText(formatTime(mCurrentTime));
                notifyDataSetChanged();
            }
        }

        /**
         * @param location
         */
        public void deleteItem(int location) {
            mSoundRecorders.remove(location);
            notifyDataSetChanged();
        }

        public void setRecords(List<SoundRecorder> soundRecorders) {
            this.mSoundRecorders=soundRecorders;
            notifyDataSetChanged();
        }

        public RecordBaseAdapter(Context context, List<SoundRecorder> soundRecorders) {
            Log.e(TAG, "RecordBaseAdapter");
            this.mContext = context;
            this.mSoundRecorders = soundRecorders;
        }

        @Override
        public int getCount() {
            int ret = 0;
            if (mSoundRecorders != null) {
                ret = mSoundRecorders.size();
            }
            return ret;
        }

        @Override
        public Object getItem(int position) {
            return mSoundRecorders.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.record_list_item, null);
                viewHolder.ivPlaySwitch = (ImageView) convertView.findViewById(R.id.play_switch);
                viewHolder.tvRecordName = (TextView) convertView.findViewById(R.id.tv_fm_name);
                viewHolder.tvAuthor = (TextView) convertView.findViewById(R.id.tv_fm_author);
                //The hidden view by default
                viewHolder.llPlayProgress = (LinearLayout) convertView.findViewById(R.id.ll_play_progress);
                viewHolder.tvForTime = (TextView) convertView.findViewById(R.id.for_time);
                viewHolder.tvAllTime = (TextView) convertView.findViewById(R.id.all_time);
                viewHolder.seekBar = (SeekBar) convertView.findViewById(R.id.seekBar);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            final SoundRecorder soundRecorder = mSoundRecorders.get(position);

            viewHolder.tvRecordName.setText(soundRecorder.getFileName());
            viewHolder.tvAllTime.setText(formatTime(mMediaPlayer.getDuration()));
            viewHolder.tvForTime.setText("");
            viewHolder.seekBar.setMax(mMediaPlayer.getDuration());
            viewHolder.seekBar.setProgress(mMediaPlayer.getCurrentPosition());

            viewHolder.ivPlaySwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPlayClick(position);
                }
            });

            viewHolder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mMediaPlayer.seekTo(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            if (mCurrentPosition == position && mMediaPlayer.isPlaying()) {
                if (viewHolder.llPlayProgress.getVisibility() == View.GONE) {
                    viewHolder.llPlayProgress.setVisibility(View.VISIBLE);
                }
                viewHolder.ivPlaySwitch.setImageResource(R.drawable.icon_btn_pause_rest);
            } else {
                if (viewHolder.llPlayProgress.getVisibility() == View.VISIBLE) {
                    viewHolder.llPlayProgress.setVisibility(View.GONE);
                }
                viewHolder.ivPlaySwitch.setImageResource(R.drawable.icon_btn_play_rest);
            }
            updateItemBackground(position, convertView);
            return convertView;
        }

        /**
         * If the item is selected, update the background color of the item
         *
         * @param position
         * @param view
         */
        private void updateItemBackground(int position, View view) {
            int color;
            if (mListView.isItemChecked(position)) {
                color = Color.parseColor("#252525");
            } else {
                color = 0;
            }
            view.setBackgroundColor(color);
        }
    }

    static final class ViewHolder {
        ImageView ivPlaySwitch;
        TextView tvRecordName;
        TextView tvAuthor;
        LinearLayout llPlayProgress;
        TextView tvForTime;
        TextView tvAllTime;
        SeekBar seekBar;
    }
}
