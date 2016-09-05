package com.android.fmradio.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toolbar;

import com.android.fmradio.R;

/**
 * Created by puming1 on 6/16/16.
 */
public class XToolBar extends Toolbar {
    private RelativeLayout mRelativeLayout;
    private TextView mTextView;
    private ImageView mImageViewDelete;
    private ImageView mImageViewRename;
    private CheckBox mCheckBox;

    private View mView;

    private OnXToolbarClickListener listener;

    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (listener == null) {
                return;
            }
            switch (v.getId()) {
                case R.id.iv_delete:
                    listener.deleteClick();
                    break;
                case R.id.iv_rename:
                    listener.renameClick();
                    break;
            }
        }
    };

    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener=new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (listener == null) {
                return;
            }
            listener.onCheckedChanged(buttonView,isChecked);
        }
    };

    public interface OnXToolbarClickListener {
        void deleteClick();

        void renameClick();

        void onCheckedChanged(CompoundButton buttonView, boolean isChecked);
    }

    public void setOnXToolbarClickListener(OnXToolbarClickListener listener) {
        this.listener = listener;
    }



    public XToolBar(Context context) {
        super(context);
        initView();
    }

    public XToolBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();

    }

    public XToolBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    public XToolBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView();
    }

    private void initView() {
        if (mView == null) {
            mView = LayoutInflater.from(getContext()).inflate(R.layout.toolbar_layout, null);

            mTextView= (TextView) mView.findViewById(R.id.tv_title);
            mRelativeLayout = (RelativeLayout) mView.findViewById(R.id.rl_menu);
            mImageViewDelete = (ImageView) mView.findViewById(R.id.iv_delete);
            mImageViewRename = (ImageView) mView.findViewById(R.id.iv_rename);
            mCheckBox = (CheckBox) mView.findViewById(R.id.cb_check);

            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_VERTICAL);
            addView(mView, layoutParams);
        }
        mImageViewDelete.setOnClickListener(mOnClickListener);
        mImageViewRename.setOnClickListener(mOnClickListener);
        mCheckBox.setOnCheckedChangeListener(mOnCheckedChangeListener);
    }

    /**
     *
     */
    public void showToolBarMenu() {
        if (mRelativeLayout != null) {
            mRelativeLayout.setVisibility(VISIBLE);
        }
    }

    public void hideToolBarMenu() {
        if (mRelativeLayout != null) {
            mRelativeLayout.setVisibility(GONE);
        }
    }

    @Override
    public void setTitle(int resId) {
        super.setTitle(resId);
        if(mTextView!=null){
        mTextView.setText(resId);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        if(mTextView!=null){
        mTextView.setText(title);
        }
    }

    public void setDeleteEnable(boolean enable){
        if(mImageViewDelete!=null){
            mImageViewDelete.setEnabled(enable);
        }
    }

    public void setRenameEnabled(boolean enable) {
        if (mImageViewRename != null) {
            mImageViewRename.setEnabled(enable);
        }
    }

    public void setCheckBoxChecked(boolean checked) {
        if (mCheckBox != null) {
            mCheckBox.setChecked(checked);
        }
    }
}
