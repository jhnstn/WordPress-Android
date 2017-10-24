package org.wordpress.android.ui.uploads;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.editor.AztecEditorFragment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.UploadActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.UploadStore;
import org.wordpress.android.fluxc.store.UploadStore.ClearMediaPayload;
import org.wordpress.android.ui.media.services.MediaUploadReadyListener;
import org.wordpress.android.ui.posts.PostUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPMediaUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class UploadService extends Service {
    private static final String KEY_SHOULD_PUBLISH = "shouldPublish";
    private static final String KEY_SHOULD_RETRY = "shouldRetry";
    private static final String KEY_MEDIA_LIST = "mediaList";
    private static final String KEY_LOCAL_POST_ID = "localPostId";
    private static final String KEY_SHOULD_TRACK_ANALYTICS = "shouldTrackPostAnalytics";

    private static @Nullable UploadService sInstance;

    private MediaUploadHandler mMediaUploadHandler;
    private PostUploadHandler mPostUploadHandler;
    private PostUploadNotifier mPostUploadNotifier;

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject PostStore mPostStore;
    @Inject SiteStore mSiteStore;
    @Inject UploadStore mUploadStore;

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);
        AppLog.i(T.MAIN, "UploadService > Created");
        mDispatcher.register(this);
        sInstance = this;
        // TODO: Recover any posts/media uploads that were interrupted by the service being stopped
    }

    @Override
    public void onDestroy() {
        if (mMediaUploadHandler != null) {
            mMediaUploadHandler.cancelInProgressUploads();
            mMediaUploadHandler.unregister();
        }

        if (mPostUploadHandler != null) {
            mPostUploadHandler.cancelInProgressUploads();
            mPostUploadHandler.unregister();
        }

        // Update posts with any completed AND failed uploads in our post->media map
        updatePostModelsWithCompletedAndFailedUploads();

        for (PostModel pendingPost : mUploadStore.getPendingPosts()) {
            cancelQueuedPostUpload(pendingPost);
        }

        mDispatcher.unregister(this);
        sInstance = null;
        AppLog.i(T.MAIN, "UploadService > Destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Skip this request if no items to upload were given
        if (intent == null || (!intent.hasExtra(KEY_MEDIA_LIST) && !intent.hasExtra(KEY_LOCAL_POST_ID))) {
            AppLog.e(T.MAIN, "UploadService > Killed and restarted with an empty intent");
            stopServiceIfUploadsComplete();
            return START_NOT_STICKY;
        }

        if (mMediaUploadHandler == null) {
            mMediaUploadHandler = new MediaUploadHandler();
        }

        if (mPostUploadNotifier == null) {
            mPostUploadNotifier = new PostUploadNotifier(getApplicationContext(), this);
        }

        if (mPostUploadHandler == null) {
            mPostUploadHandler = new PostUploadHandler(mPostUploadNotifier);
        }

        if (intent.hasExtra(KEY_MEDIA_LIST)) {
            unpackMediaIntent(intent);
        }

        if (intent.hasExtra(KEY_LOCAL_POST_ID)) {
            unpackPostIntent(intent);
        }

        return START_REDELIVER_INTENT;
    }

    private void unpackMediaIntent(@NonNull Intent intent) {
        // TODO right now, in the case we had pending uploads and the app/service was restarted,
        // we don't really have a way to tell which media was supposed to be added to which post,
        // unless we open each draft post from the PostStore and try to see if there was any locally added media to try
        // and match their IDs.
        // So let's hold on a bit on this functionality, the service won't be recovering any
        // pending / missing / cancelled / interrupted uploads for now

//        // add local queued media from store
//        List<MediaModel> localMedia = mMediaStore.getLocalSiteMedia(site);
//        if (localMedia != null && !localMedia.isEmpty()) {
//            // uploading is updated to queued, queued media added to the queue, failed media added to completed list
//            for (MediaModel mediaItem : localMedia) {
//
//                if (MediaUploadState.UPLOADING.name().equals(mediaItem.getUploadState())) {
//                    mediaItem.setUploadState(MediaUploadState.QUEUED.name());
//                    mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(mediaItem));
//                }
//
//                if (MediaUploadState.QUEUED.name().equals(mediaItem.getUploadState())) {
//                    addUniqueMediaToQueue(mediaItem);
//                } else if (MediaUploadState.FAILED.name().equals(mediaItem.getUploadState())) {
//                    getCompletedItems().add(mediaItem);
//                }
//            }
//        }

        mPostUploadNotifier.cancelFinalNotificationForMedia();

        // add new media
        @SuppressWarnings("unchecked")
        List<MediaModel> mediaList = (List<MediaModel>) intent.getSerializableExtra(KEY_MEDIA_LIST);
        if (mediaList != null) {
            for (MediaModel media : mediaList) {
                mMediaUploadHandler.upload(media);
            }
            mPostUploadNotifier.addMediaInfoToForegroundNotification(mediaList);
        }
    }

    private void unpackPostIntent(@NonNull Intent intent) {
        PostModel post = mPostStore.getPostByLocalPostId(intent.getIntExtra(KEY_LOCAL_POST_ID, 0));
        if (post != null) {
            boolean shouldTrackAnalytics = intent.getBooleanExtra(KEY_SHOULD_TRACK_ANALYTICS, false);
            if (shouldTrackAnalytics) {
                mPostUploadHandler.registerPostForAnalyticsTracking(post);
            }

            // cancel any outstanding "end" notification for this Post before we start processing it again
            // i.e. dismiss success or error notification for the post.
            mPostUploadNotifier.cancelFinalNotification(post);

            // if the user tapped on the PUBLISH quick action, make this Post publishable and track
            // analytics before starting the upload process.
            if (intent.getBooleanExtra(KEY_SHOULD_PUBLISH, false)) {
                makePostPublishable(post);
                PostUtils.trackSavePostAnalytics(post, mSiteStore.getSiteByLocalId(post.getLocalSiteId()));
            }

            if (intent.getBooleanExtra(KEY_SHOULD_RETRY, false)) {
                if (AppPrefs.isAztecEditorEnabled()) {
                    if (!NetworkUtils.isNetworkAvailable(this)) {
                        rebuildNotificationError(post, getString(R.string.no_network_message));
                        return;
                    }
                    aztecRetryUpload(post);
                } else {
                    ToastUtils.showToast(this, R.string.retry_needs_aztec);
                }
                return;
            }

            // is this a new post? only add count to the notification when the post is totally new
            // i.e. it still doesn't have any tracked state in the UploadStore
            // or it's a failed one the user is actively retrying.
            if (isThisPostTotallyNewOrFailed(post)) {
                mPostUploadNotifier.addPostInfoToForegroundNotification(post, null);
            }

            if (!hasPendingOrInProgressMediaUploadsForPost(post)) {
                mPostUploadHandler.upload(post);
            } else {
                // Register the post (as PENDING) in the UploadStore, along with all media currently in progress for it
                // If the post is already registered, the new media will be added to its list
                List<MediaModel> activeMedia = MediaUploadHandler.getPendingOrInProgressMediaUploadsForPost(post);
                mUploadStore.registerPostModel(post, activeMedia);
            }
        }
    }

    public static void cancelFinalNotification(PostModel post){
        // cancel any outstanding "end" notification for this Post before we start processing it again
        // i.e. dismiss success or error notification for the post.
        if (sInstance != null && sInstance.mPostUploadNotifier != null) {
            sInstance.mPostUploadNotifier.cancelFinalNotification(post);
        }
    }

    private void makePostPublishable(@NonNull PostModel post) {
        PostUtils.updatePublishDateIfShouldBePublishedImmediately(post);
        post.setStatus(PostStatus.PUBLISHED.toString());
        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post));
    }

    private boolean isThisPostTotallyNewOrFailed(PostModel post){
        // if we have any tracks for this Post's UploadState, this means this Post is not new.
        // Conditions under which the UploadStore would contain traces of this Post's UploadState are:
        // - it's been cancelled by entering/exiting/entering the editor thus cancelling the queued post upload
        // to allow for the user to keep editing it before sending to the server
        // - it's a failed upload (due to some network issue, for example)
        // - it's a pending upload (it is currently registered for upload once the associated media finishes
        // uploading).
        return !mUploadStore.isRegisteredPostModel(post) || (mUploadStore.isFailedPost(post) || mUploadStore.isPendingPost(post));
    }


    public static Intent getUploadPostServiceIntent(Context context, @NonNull PostModel post, boolean trackAnalytics,
                                                    boolean publish, boolean isRetry) {
        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(KEY_LOCAL_POST_ID, post.getId());
        intent.putExtra(KEY_SHOULD_TRACK_ANALYTICS, trackAnalytics);
        intent.putExtra(KEY_SHOULD_PUBLISH, publish);
        intent.putExtra(KEY_SHOULD_RETRY, isRetry);
        return intent;
    }

    public static Intent getUploadMediaServiceIntent(Context context, @NonNull ArrayList<MediaModel> mediaList,
                                                     boolean isRetry) {
        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(UploadService.KEY_MEDIA_LIST, mediaList);
        intent.putExtra(KEY_SHOULD_RETRY, isRetry);
        return intent;
    }

    /**
     * Adds a post to the queue.
     */
    public static void uploadPost(Context context, @NonNull PostModel post) {
        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(KEY_LOCAL_POST_ID, post.getId());
        intent.putExtra(KEY_SHOULD_TRACK_ANALYTICS, false);
        context.startService(intent);
    }

    /**
     * Adds a post to the queue and tracks post analytics.
     * To be used only the first time a post is uploaded, i.e. when its status changes from local draft or remote draft
     * to published.
     */
    public static void uploadPostAndTrackAnalytics(Context context, @NonNull PostModel post) {
        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(KEY_LOCAL_POST_ID, post.getId());
        intent.putExtra(KEY_SHOULD_TRACK_ANALYTICS, true);
        context.startService(intent);
    }

    public static void setLegacyMode(boolean enabled) {
        PostUploadHandler.setLegacyMode(enabled);
    }

    public static void uploadMedia(Context context, @NonNull ArrayList<MediaModel> mediaList) {
        if (context == null) {
            return;
        }

        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(UploadService.KEY_MEDIA_LIST, mediaList);
        context.startService(intent);
    }

    /**
     * Returns true if the passed post is either currently uploading or waiting to be uploaded.
     * Except for legacy mode, a post counts as 'uploading' if the post content itself is being uploaded - a post
     * waiting for media to finish uploading counts as 'waiting to be uploaded' until the media uploads complete.
     */
    public static boolean isPostUploadingOrQueued(PostModel post) {
        if (sInstance == null || post == null) {
            return false;
        }

        // First check for posts uploading or queued inside the PostUploadManager
        if (PostUploadHandler.isPostUploadingOrQueued(post)) {
            return true;
        }

        // Then check the list of posts waiting for media to complete
        return sInstance.mUploadStore.isPendingPost(post);
    }

    public static boolean isPostQueued(PostModel post) {
        // Check for posts queued inside the PostUploadManager
        return sInstance != null && post != null && PostUploadHandler.isPostQueued(post);
    }

    /**
     * Returns true if the passed post is currently uploading.
     * Except for legacy mode, a post counts as 'uploading' if the post content itself is being uploaded - a post
     * waiting for media to finish uploading counts as 'waiting to be uploaded' until the media uploads complete.
     */
    public static boolean isPostUploading(PostModel post) {
        return sInstance != null && post != null && PostUploadHandler.isPostUploading(post);
    }

    public static void cancelQueuedPostUploadAndRelatedMedia(PostModel post) {
        if (post != null) {
            cancelQueuedPostUpload(post);
            EventBus.getDefault().post(new PostEvents.PostMediaCanceled(post));
        }
    }

    public static void cancelQueuedPostUpload(PostModel post) {
        if (sInstance != null && post != null) {
            // Mark the post as CANCELLED in the UploadStore
            sInstance.mDispatcher.dispatch(UploadActionBuilder.newCancelPostAction(post));
        }
    }

    public static PostModel updatePostWithCurrentlyCompletedUploads(PostModel post) {
        if (post != null && sInstance != null) {
            // now get the list of completed media for this post, so we can make post content
            // updates in one go and save only once
            MediaUploadReadyListener processor = new MediaUploadReadyProcessor();
            Set<MediaModel> completedMedia = sInstance.mUploadStore.getCompletedMediaForPost(post);
            for (MediaModel media : completedMedia) {
                post = updatePostWithMediaUrl(post, media, processor);
            }

            if (completedMedia != null && !completedMedia.isEmpty()) {
                // finally remove all completed uploads for this post, as they've been taken care of
                ClearMediaPayload clearMediaPayload = new ClearMediaPayload(post, completedMedia);
                sInstance.mDispatcher.dispatch(UploadActionBuilder.newClearMediaForPostAction(clearMediaPayload));
            }
        }
        return post;
    }

    public static PostModel updatePostWithCurrentlyFailedUploads(PostModel post) {
        if (post != null && sInstance != null) {
            // now get the list of failed media for this post, so we can make post content
            // updates in one go and save only once
            MediaUploadReadyListener processor = new MediaUploadReadyProcessor();
            Set<MediaModel> failedMedia = sInstance.mUploadStore.getFailedMediaForPost(post);
            for (MediaModel media : failedMedia) {
                post = updatePostWithFailedMedia(post, media, processor);
            }
            // Unlike completed media, we won't remove the failed media references, so we can look up their errors later
        }
        return post;
    }

    public static boolean hasInProgressMediaUploadsForPost(PostModel postModel) {
        return postModel != null && MediaUploadHandler.hasInProgressMediaUploadsForPost(postModel);
    }

    public static boolean hasPendingMediaUploadsForPost(PostModel postModel) {
        return postModel != null && MediaUploadHandler.hasPendingMediaUploadsForPost(postModel);
    }

    public static boolean hasPendingOrInProgressMediaUploadsForPost(PostModel postModel) {
        return postModel != null && MediaUploadHandler.hasPendingOrInProgressMediaUploadsForPost(postModel);
    }

    public static List<MediaModel> getPendingOrInProgressMediaUploadsForPost(PostModel post){
        return MediaUploadHandler.getPendingOrInProgressMediaUploadsForPost(post);
    }

    public static float getMediaUploadProgressForPost(PostModel postModel) {
        if (postModel == null || sInstance == null) {
            // If the UploadService isn't running, there's no progress for this post
            return 0;
        }

        Set<MediaModel> pendingMediaList = sInstance.mUploadStore.getUploadingMediaForPost(postModel);

        if (pendingMediaList.size() == 0) {
            return 1;
        }

        float overallProgress = 0;
        for (MediaModel pendingMedia : pendingMediaList) {
            overallProgress += getUploadProgressForMedia(pendingMedia);
        }
        overallProgress /= pendingMediaList.size();

        return overallProgress;
    }

    public static float getUploadProgressForMedia(MediaModel mediaModel) {
        if (mediaModel == null || sInstance == null) {
            // If the UploadService isn't running, there's no progress for this media
            return 0;
        }

        float uploadProgress = sInstance.mUploadStore.getUploadProgressForMedia(mediaModel);

        // If this is a video and video optimization is enabled, include the optimization progress in the outcome
        if (mediaModel.isVideo() && WPMediaUtils.isVideoOptimizationEnabled()) {
            return MediaUploadHandler.getOverallProgressForVideo(mediaModel, uploadProgress);
        }

        return uploadProgress;
    }

    public static @NonNull Set<MediaModel> getPendingMediaForPost(PostModel postModel) {
        if (postModel == null || sInstance == null) {
            return Collections.emptySet();
        }
        return sInstance.mUploadStore.getUploadingMediaForPost(postModel);
    }

    public static boolean isPendingOrInProgressMediaUpload(@NonNull MediaModel media) {
        return MediaUploadHandler.isPendingOrInProgressMediaUpload(media);
    }

    /**
     * Rechecks all media in the MediaStore marked UPLOADING/QUEUED against the UploadingService to see
     * if it's actually uploading or queued and change it accordingly, to recover from an inconsistent state
     */
    public static void sanitizeMediaUploadStateForSite(@NonNull MediaStore mediaStore, @NonNull Dispatcher dispatcher,
                                                       @NonNull SiteModel site) {
        List<MediaModel> uploadingMedia =
                mediaStore.getSiteMediaWithState(site, MediaModel.MediaUploadState.UPLOADING);
        List<MediaModel> queuedMedia =
                mediaStore.getSiteMediaWithState(site, MediaModel.MediaUploadState.QUEUED);
        List<MediaModel> uploadingOrQueuedMedia = new ArrayList<>();
        uploadingOrQueuedMedia.addAll(uploadingMedia);
        uploadingOrQueuedMedia.addAll(queuedMedia);

        for (final MediaModel media : uploadingOrQueuedMedia) {
            if (!UploadService.isPendingOrInProgressMediaUpload(media)) {
                // it is NOT being uploaded or queued in the actual UploadService, mark it failed
                media.setUploadState(MediaModel.MediaUploadState.FAILED);
                dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
            }
        }
    }

    private static synchronized PostModel updatePostWithMediaUrl(PostModel post, MediaModel media,
                                                                 MediaUploadReadyListener processor) {
        if (media != null && post != null && processor != null) {
            // actually replace the media ID with the media uri
            PostModel modifiedPost = processor.replaceMediaFileWithUrlInPost(post, String.valueOf(media.getId()),
                    FluxCUtils.mediaFileFromMediaModel(media));
            if (modifiedPost != null) {
                post = modifiedPost;
            }

            // we changed the post, so let’s mark this down
            if (!post.isLocalDraft()) {
                post.setIsLocallyChanged(true);
            }
            post.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));

        }
        return post;
    }

    private static synchronized PostModel updatePostWithFailedMedia(PostModel post, MediaModel media,
                                                                 MediaUploadReadyListener processor) {
        if (media != null && post != null && processor != null) {
            // actually mark the media failed within the Post
            PostModel modifiedPost = processor.markMediaUploadFailedInPost(post, String.valueOf(media.getId()),
                    FluxCUtils.mediaFileFromMediaModel(media));
            if (modifiedPost != null) {
                post = modifiedPost;
            }

            // we changed the post, so let’s mark this down
            if (!post.isLocalDraft()) {
                post.setIsLocallyChanged(true);
            }
            post.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));

        }
        return post;
    }

    private synchronized void stopServiceIfUploadsComplete() {
        if (mPostUploadHandler != null && mPostUploadHandler.hasInProgressUploads()) {
            return;
        }

        if (mMediaUploadHandler != null && mMediaUploadHandler.hasInProgressUploads()) {
            return;
        }

        if (!mUploadStore.getPendingPosts().isEmpty()) {
            return;
        }

        updatePostModelsWithCompletedAndFailedUploads();

        AppLog.i(T.MAIN, "UploadService > Completed");
        stopSelf();
    }

    private void updatePostModelsWithCompletedAndFailedUploads(){
        for (PostModel postModel : mUploadStore.getAllRegisteredPosts()) {
            // For each post with completed media uploads, update the content with the new remote URLs
            // This is done in a batch when all media is complete to prevent conflicts by updating separate images
            // at a time simultaneously for the same post
            updateOnePostModelWithCompletedAndFailedUploads(postModel);
        }
    }

    private PostModel updateOnePostModelWithCompletedAndFailedUploads(PostModel postModel) {
        PostModel updatedPost = updatePostWithCurrentlyCompletedUploads(postModel);
        // also do the same now with failed uploads
        updatedPost = updatePostWithCurrentlyFailedUploads(updatedPost);
        // finally, save the PostModel
        if (updatedPost != null) {
            mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(updatedPost));
        }
        return updatedPost;
    }

    private void cancelPostUploadMatchingMedia(@NonNull MediaModel media, String errorMessage, boolean showError) {
        PostModel postToCancel = mPostStore.getPostByLocalPostId(media.getLocalPostId());
        if (postToCancel == null) return;

        SiteModel site = mSiteStore.getSiteByLocalId(postToCancel.getLocalSiteId());
        mPostUploadNotifier.incrementUploadedPostCountFromForegroundNotification(postToCancel);

        if (showError || mUploadStore.isFailedPost(postToCancel)) {
            // Only show the media upload error notification if the post is NOT registered in the UploadStore
            // - otherwise if it IS registered in the UploadStore and we get a `cancelled` signal it means
            // the user actively cancelled it. No need to show an error then.
            String message = UploadUtils.getErrorMessage(this, postToCancel, errorMessage, true);
            mPostUploadNotifier.updateNotificationErrorForPost(postToCancel, site, message);
        }

        mPostUploadHandler.unregisterPostForAnalyticsTracking(postToCancel);
        EventBus.getDefault().post(new PostEvents.PostUploadCanceled(postToCancel.getLocalSiteId()));

    }

    private void rebuildNotificationError(PostModel post, String errorMessage) {
        Set<MediaModel> failedMedia = mUploadStore.getFailedMediaForPost(post);
        mPostUploadNotifier.setTotalMediaItems(post, failedMedia.size());
        mPostUploadNotifier.updateNotificationErrorForPost(post,
                mSiteStore.getSiteByLocalId(post.getLocalSiteId()), errorMessage);

    }

    private void aztecRetryUpload(PostModel post) {
        Set<MediaModel> failedMedia = mUploadStore.getFailedMediaForPost(post);
        ArrayList<MediaModel> mediaToRetry = new ArrayList<>(failedMedia);
        mPostUploadNotifier.prepareForegroundNotificationForRetry(post, mediaToRetry);
        if (!failedMedia.isEmpty()) {
            // reset these media items to QUEUED
            for (MediaModel media : failedMedia) {
                media.setUploadState(MediaModel.MediaUploadState.QUEUED);
                mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
            }

            // do the same within the Post content itself
            String postContentWithRestartedUploads = AztecEditorFragment.restartFailedMediaToUploading(this, post.getContent());
            post.setContent(postContentWithRestartedUploads);
            mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post));

            // no retry uploading the media items
            for (MediaModel media : mediaToRetry) {
                mMediaUploadHandler.upload(media);
            }

            // Register the post (as PENDING) in the UploadStore, along with all media currently in progress for it
            // If the post is already registered, the new media will be added to its list
            mUploadStore.registerPostModel(post, mediaToRetry);
            mPostUploadNotifier.addPostInfoToForegroundNotification(post, mediaToRetry);
        } else {
            // retry uploading the Post
            mPostUploadHandler.upload(post);
        }
    }

    /**
     * Has lower priority than the UploadHandlers, which ensures that the handlers have already received and
     * processed this OnMediaUploaded event. This means we can safely rely on their internal state being up to date.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 7)
    public void onMediaUploaded(OnMediaUploaded event) {
        if (event.media == null) {
            return;
        }

        if (event.isError()) {
            if (event.media.getLocalPostId() > 0) {
                AppLog.w(T.MAIN, "UploadService > Media upload failed for post " + event.media.getLocalPostId() + " : "
                        + event.error.type + ": " + event.error.message);
                String errorMessage = UploadUtils.getErrorMessageFromMediaError(this, event.media, event.error);
                cancelPostUploadMatchingMedia(event.media, errorMessage, true);
            } else {
                // this media item doesn't belong to a Post
                mPostUploadNotifier.incrementUploadedMediaCountFromProgressNotification(event.media.getId());
                // Only show the media upload error notification if the post is NOT registered in the UploadStore
                // - otherwise if it IS registered in the UploadStore and we get a `cancelled` signal it means
                // the user actively cancelled it. No need to show an error then.
                String message = UploadUtils.getErrorMessageFromMediaError(this, event.media, event.error);

                int siteLocalId = AppPrefs.getSelectedSite();
                SiteModel selectedSite = mSiteStore.getSiteByLocalId(siteLocalId);

                List<MediaModel> failedStandAloneMedia = getRetriableStandaloneMedia(selectedSite);
                if (failedStandAloneMedia.isEmpty()) {
                    // if we couldn't get the failed media from the MediaStore, at least we know
                    // for sure we're handling the event for this specific media item, so throw an error
                    // notification for this particular media item travelling in event.media
                    failedStandAloneMedia.add(event.media);
                }

                mPostUploadNotifier.updateNotificationErrorForMedia(failedStandAloneMedia,
                        selectedSite,message);
            }
            stopServiceIfUploadsComplete();
            return;
        }

        if (event.canceled) {
            if (event.media.getLocalPostId() > 0) {
                AppLog.i(T.MAIN, "UploadService > Upload cancelled for post with id " + event.media.getLocalPostId()
                        + " - a media upload for this post has been cancelled, id: " + event.media.getId());
                cancelPostUploadMatchingMedia(event.media, getString(R.string.error_media_canceled), false);
            }
            stopServiceIfUploadsComplete();
            return;
        }

        if (event.completed) {
            if (event.media.getLocalPostId() != 0) {
                AppLog.i(T.MAIN, "UploadService > Processing completed media with id " + event.media.getId()
                        + " and local post id " + event.media.getLocalPostId());
                // If this was the last media upload a post was waiting for, update the post content
                // This done for pending as well as cancelled and failed posts
                for (PostModel postModel : mUploadStore.getAllRegisteredPosts()) {
                    if (!UploadService.hasPendingOrInProgressMediaUploadsForPost(postModel)) {
                        // Replace local with remote media in the post content
                        PostModel updatedPost = updatePostWithCurrentlyCompletedUploads(postModel);
                        // also do the same now with failed uploads
                        updatedPost = updatePostWithCurrentlyFailedUploads(updatedPost);
                        // finally, save the PostModel
                        if (updatedPost != null) {
                            mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(updatedPost));
                            if (mUploadStore.isPendingPost(postModel)) {
                                // TODO Should do some extra validation here
                                // e.g. what if the post has local media URLs but no pending media uploads?
                                mPostUploadHandler.upload(updatedPost);
                            }
                        }
                    }
                }
            }
            mPostUploadNotifier.incrementUploadedMediaCountFromProgressNotification(event.media.getId());
            stopServiceIfUploadsComplete();
        } else {
            // in-progress upload
            // Progress update
            mPostUploadNotifier.updateNotificationProgressForMedia(event.media, event.progress);
        }
    }

    private List<MediaModel> getRetriableStandaloneMedia(SiteModel selectedSite) {
        // get all retriable media ? To retry or not to retry, that is the question
        List<MediaModel> failedMedia = null;
        List<MediaModel> failedStandAloneMedia = new ArrayList<>();
        if (selectedSite != null) {
            failedMedia = mMediaStore.getSiteMediaWithState(
                    selectedSite, MediaModel.MediaUploadState.FAILED);
        }

        // only take into account those media items that do not belong to any Post
        for (MediaModel media: failedMedia) {
            if (media.getLocalPostId() == 0) {
                failedStandAloneMedia.add(media);
            }
        }

        return failedStandAloneMedia;
    }

    /**
     * Has lower priority than the PostUploadHandler, which ensures that the handler has already received and
     * processed this OnPostUploaded event. This means we can safely rely on its internal state being up to date.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 7)
    public void onPostUploaded(OnPostUploaded event) {
        stopServiceIfUploadsComplete();
    }

    public static class UploadErrorEvent {
        public final PostModel post;
        public final List<MediaModel> mediaModelList;
        public final String errorMessage;

        UploadErrorEvent(PostModel post, String errorMessage) {
            this.post = post;
            this.mediaModelList = null;
            this.errorMessage = errorMessage;
        }

        UploadErrorEvent(List<MediaModel> mediaModelList, String errorMessage) {
            this.post = null;
            this.mediaModelList = mediaModelList;
            this.errorMessage = errorMessage;
        }
    }
}
