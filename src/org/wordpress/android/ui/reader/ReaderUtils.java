package org.wordpress.android.ui.reader;

import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;

import java.util.List;

public class ReaderUtils {
    /*
     * used by ReaderPostDetailFragment to enter/exit full screen mode
     */
    static interface FullScreenListener {
        boolean onRequestFullScreen(boolean enable);
        boolean isFullScreen();
        boolean isFullScreenSupported();
    }

    /*
     * used with TextViews that have the ReaderTextView.Follow style to show
     * the passed follow state
     */
    public static void showFollowStatus(final TextView txtFollow, boolean isFollowed) {
        // selected state is same as followed state, so do nothing if they already match
        if (txtFollow == null || txtFollow.isSelected() == isFollowed) {
            return;
        }

        if (isFollowed) {
            txtFollow.setText(txtFollow.getContext().getString(R.string.reader_btn_unfollow));
        } else {
            txtFollow.setText(txtFollow.getContext().getString(R.string.reader_btn_follow));
        }

        int drawableId = (isFollowed ? R.drawable.note_icon_following : R.drawable.note_icon_follow);
        txtFollow.setCompoundDrawablesWithIntrinsicBounds(drawableId, 0, 0, 0);

        txtFollow.setSelected(isFollowed);
    }

    /*
     * returns true if the passed view's tag is the same as the passed string - this is used
     * with imageViews that show network images, to avoid reloading the image if the imageView
     * is already tagged with the image url
     */
    public static boolean viewHasTag(final View view, final String tag) {
        if (view == null || tag == null) {
            return false;
        } else {
            return tag.equals(view.getTag());
        }
    }

    /*
     * return the path to use for the /batch/ endpoint from the list of request urls
     * https://developer.wordpress.com/docs/api/1/get/batch/
     */
    public static String getBatchEndpointForRequests(List<String> requestUrls) {
        StringBuilder sbBatch = new StringBuilder("/batch/");
        if (requestUrls != null) {
            boolean isFirst = true;
            for (String url : requestUrls) {
                if (!TextUtils.isEmpty(url)) {
                    if (isFirst) {
                        isFirst = false;
                        sbBatch.append("?");
                    } else {
                        sbBatch.append("&");
                    }
                    sbBatch.append("urls%5B%5D=").append(Uri.encode(url));
                }
            }
        }
        return sbBatch.toString();
    }

    /*
     * adds a transparent header to the passed listView
     */
    static View addListViewHeader(ListView listView, int height) {
        if (listView == null) {
            return null;
        }
        RelativeLayout header = new RelativeLayout(listView.getContext());
        header.setLayoutParams(new AbsListView.LayoutParams(
                               AbsListView.LayoutParams.MATCH_PARENT,
                               height));
        listView.addHeaderView(header, null, false);
        return header;
    }

    /*
     * adds a rule which tells the view with targetId to be placed below layoutBelowId - only
     * works if viewParent is a RelativeLayout
     */
    static void layoutBelow(ViewGroup viewParent, int targetId, int layoutBelowId) {
        if (viewParent == null || !(viewParent instanceof RelativeLayout)) {
            return;
        }

        View target = viewParent.findViewById(targetId);
        if (target == null) {
            return;
        }

        if (target.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) target.getLayoutParams();
            params.addRule(RelativeLayout.BELOW, layoutBelowId);
        }
    }
}
