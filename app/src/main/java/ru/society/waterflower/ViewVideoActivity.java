package ru.society.waterflower;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import ru.society.waterflower.common.ActivityBase;
import ru.society.waterflower.dialogs.CommentActionDialog;
import ru.society.waterflower.dialogs.CommentDeleteDialog;
import ru.society.waterflower.dialogs.MyCommentActionDialog;
import ru.society.waterflower.dialogs.MyPhotoActionDialog;
import ru.society.waterflower.dialogs.PhotoActionDialog;
import ru.society.waterflower.dialogs.PhotoDeleteDialog;
import ru.society.waterflower.dialogs.PhotoReportDialog;


public class ViewVideoActivity extends ActivityBase implements CommentDeleteDialog.AlertPositiveListener, CommentActionDialog.AlertPositiveListener, MyCommentActionDialog.AlertPositiveListener, PhotoDeleteDialog.AlertPositiveListener, PhotoReportDialog.AlertPositiveListener, MyPhotoActionDialog.AlertPositiveListener, PhotoActionDialog.AlertPositiveListener {

    Toolbar mToolbar;

    Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_video);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        if (savedInstanceState != null) {

            fragment = getSupportFragmentManager().getFragment(savedInstanceState, "currentFragment");

        } else {

            fragment = new ViewVideoFragment();
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container_body, fragment).commit();
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {

        super.onSaveInstanceState(outState);

        getSupportFragmentManager().putFragment(outState, "currentFragment", fragment);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        fragment.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {

        ViewVideoFragment p = (ViewVideoFragment) fragment;
        p.hideEmojiKeyboard();

        super.onPause();
    }

    @Override
    public void onPhotoDelete(int position) {

        ViewVideoFragment p = (ViewVideoFragment) fragment;
        p.onVideoDelete(position);
    }

    @Override
    public void onPhotoReport(int position, int reasonId) {

        ViewVideoFragment p = (ViewVideoFragment) fragment;
        p.onVideoReport(position, reasonId);
    }

    @Override
    public void onPhotoRemoveDialog(int position) {

        ViewVideoFragment p = (ViewVideoFragment) fragment;
        p.remove(position);
    }

    @Override
    public void onPhotoReportDialog(int position) {

        ViewVideoFragment p = (ViewVideoFragment) fragment;
        p.report(position);
    }

    @Override
    public void onCommentRemove(int position) {

        ViewVideoFragment p = (ViewVideoFragment) fragment;
        p.onCommentRemove(position);
    }

    @Override
    public void onCommentDelete(int position) {

        ViewVideoFragment p = (ViewVideoFragment) fragment;
        p.onCommentDelete(position);
    }

    @Override
    public void onCommentReply(int position) {

        ViewVideoFragment p = (ViewVideoFragment) fragment;
        p.onCommentReply(position);
    }

    @Override
    public void onBackPressed(){

        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {

            case android.R.id.home: {

                finish();
                return true;
            }

            default: {

                return super.onOptionsItemSelected(item);
            }
        }
    }
}
