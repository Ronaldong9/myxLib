package com.myx.library.image;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.internal.Closeables;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeController;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor;
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferInputStream;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.myx.library.util.Futils;
import com.myx.library.util.ToastUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by mayuxin on 2017/3/17.
 */

public class ImageUtils {
    public static GenericDraweeHierarchyBuilder builder;
    public static GenericDraweeHierarchy hierarchy;
    private static final int MAX_HEAP_SIZE = (int) Runtime.getRuntime().maxMemory();
    public static final long MAX_MEMORY_CACHE_SIZE = MAX_HEAP_SIZE / 8;//使用的缓存数量
    public static final long MAX_LOW_MEMORY_CACHE_SIZE = MAX_HEAP_SIZE / 16;//使用的缓存数量
    public static final long MAX_VERY_LOW_MEMORY_CACHE_SIZE = MAX_HEAP_SIZE / 32;//使用的缓存数量

    public static final int FADE_TIME = 120;
    public static Application app;

    /**
     * @param context
     */
    public static void initialize(Application context) {
        app = context;
        DiskCacheConfig diskCacheConfig = DiskCacheConfig.newBuilder(context)
                .setBaseDirectoryName("v1")
                .setMaxCacheSize(MAX_MEMORY_CACHE_SIZE)
                .setMaxCacheSizeOnLowDiskSpace(MAX_LOW_MEMORY_CACHE_SIZE)
                .setMaxCacheSizeOnVeryLowDiskSpace(MAX_VERY_LOW_MEMORY_CACHE_SIZE)
                .setVersion(1)
                .build();
        ImagePipelineConfig imagePipelineConfig = ImagePipelineConfig.newBuilder(context).setMainDiskCacheConfig(diskCacheConfig).
                build();

        Fresco.initialize(context, imagePipelineConfig);
        builder = new GenericDraweeHierarchyBuilder(context.getResources());
        hierarchy = builder.setFadeDuration(FADE_TIME).build();
    }

    /**
     * @param loadUri
     * @return
     */
    private static boolean isImageDownloaded(Uri loadUri) {
        if (loadUri == null) {
            return false;
        }
        CacheKey cacheKey = DefaultCacheKeyFactory.getInstance().getEncodedCacheKey(ImageRequest.fromUri(loadUri), null);
        return ImagePipelineFactory.getInstance().getMainDiskStorageCache().hasKey(cacheKey) || ImagePipelineFactory.getInstance().getSmallImageDiskStorageCache().hasKey(cacheKey);
    }

    /**
     * @param path
     * @param imageView
     * @param isOnlyWifi
     * @param defaultDrawable
     */
    public static void loadBitmapOnlyWifi(String path, ImageView imageView, boolean isOnlyWifi, int defaultDrawable) {
        try {
            if (path == null) {
                return;
            }
            if (!path.startsWith("http://") && !path.startsWith("file://") && !path.startsWith("res://")) {
                path = "file://" + path;
            }

            if (imageView instanceof SimpleDraweeView) {
                if (((SimpleDraweeView) imageView).getHierarchy() == null) {
                    if (defaultDrawable != 0) {
                        hierarchy.setPlaceholderImage(defaultDrawable);
                        ((SimpleDraweeView) imageView).setHierarchy(hierarchy);
                    }
                } else {
                    if (defaultDrawable != 0) {
                        ((SimpleDraweeView) imageView).getHierarchy().setPlaceholderImage(defaultDrawable);
                    }
                }

                if (isOnlyWifi) {
                    if (Futils.isWifiConnected(app)) {
                        imageView.setImageURI(Uri.parse(path));
                    } else {
                        boolean isInCache = isImageDownloaded(Uri.parse(path));
                        if (isInCache) {
                            imageView.setImageURI(Uri.parse(path));
                        } else {
                            imageView.setImageURI(Uri.parse(""));
                        }
                    }
                } else {
                    imageView.setImageURI(Uri.parse(path));
                }
            }
        } catch (OutOfMemoryError error) {
            Fresco.getImagePipeline().clearMemoryCaches();
        } catch (Exception e) {

        }
    }

    /**
     * @param path
     * @param imageView
     * @param isOnlyWifi
     * @param defaultDrawable
     * @param precent
     */
    public static void loadBitmapOnlyWifi(String path, ImageView imageView, boolean isOnlyWifi, int defaultDrawable, float precent) {
        if (imageView instanceof SimpleDraweeView) {
            ((SimpleDraweeView) imageView).setAspectRatio(precent);
        }
        loadBitmapOnlyWifi(path, imageView, isOnlyWifi, defaultDrawable);
    }

    /**
     * @param res
     * @param imageView
     * @param isOnlyWifi
     * @param defaultDrawable
     * @param width
     * @param precent
     */
    public static void loadBitmapOnlyWifi(int res, ImageView imageView, boolean isOnlyWifi, int defaultDrawable, int width, float precent) {
        if (res <= 0) {
            return;
        }
        String path = "res://" + app.getPackageName() + "/" + res;
        if (imageView instanceof SimpleDraweeView) {
            ViewGroup.LayoutParams p = imageView.getLayoutParams();
            p.width = width;
            ((SimpleDraweeView) imageView).setAspectRatio(precent);
        }
        loadBitmapOnlyWifi(path, imageView, isOnlyWifi, defaultDrawable);
    }

    /**
     * @param filePath
     * @param imageView
     * @param widthSize
     * @param heightSize
     */
    public static void loadBitmap(String filePath, ImageView imageView, int widthSize, int heightSize) {
        if (filePath == null) {
            return;
        }
        if (imageView instanceof SimpleDraweeView) {
            ViewGroup.LayoutParams p = imageView.getLayoutParams();
            p.width = widthSize;
            p.height = heightSize;
            filePath = "file://" + filePath;
            ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(filePath))
                    .setResizeOptions(new ResizeOptions(widthSize, heightSize))
                    .build();
            PipelineDraweeController controller = (PipelineDraweeController) Fresco.newDraweeControllerBuilder().setOldController(((SimpleDraweeView) imageView).getController())
                    .setImageRequest(request)
                    .build();
            ((SimpleDraweeView) imageView).setController(controller);
        }
    }

    /**
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
            final float totalPixels = width * height;
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;
            if (inSampleSize <= 0) {
                inSampleSize = 1;
            }
            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }


    /**
     *
     */
    public static void clearFrescoCache() {
        try {
            Fresco.getImagePipeline().clearCaches();
        } catch (Exception e) {
        }
    }

    public static interface ImageCallBack {
        void onSuccess(String imgaeUrl, File file);
    }

    public static void test(final String imgUrl, final ImageCallBack imageCallBack) {
        File temp = getFile(imgUrl);
        if (temp.exists() && temp.length() > 0) {
            if (imageCallBack != null) {
                imageCallBack.onSuccess(imgUrl, temp);
                return;
            }
            // 已下载 直接读
        } else {
            ImagePipeline imagePipeline = Fresco.getImagePipeline();
            DataSource<CloseableReference<PooledByteBuffer>> dataSource = imagePipeline.fetchEncodedImage(ImageRequest.fromUri(Uri.parse(imgUrl)), null);
            dataSource.subscribe(new BaseDataSubscriber<CloseableReference<PooledByteBuffer>>() {
                @Override
                protected void onNewResultImpl(DataSource<CloseableReference<PooledByteBuffer>> dataSource) {
                    if (!dataSource.isFinished()) {
                        return;
                    }

//                    CloseableReference ref = dataSource.getResult();
                    new ImageAsyncTask(imageCallBack, imgUrl).execute(dataSource.getResult());
                }

                @Override
                protected void onFailureImpl(DataSource<CloseableReference<PooledByteBuffer>> dataSource) {

                }
            }, CallerThreadExecutor.getInstance());

        }
    }

    static class ImageAsyncTask extends AsyncTask<Object, Void, File> {
        private ImageCallBack imageCallBack;
        private String imageUrl;

        public ImageAsyncTask(ImageCallBack imageCallBack, String imageUrl) {
            this.imageCallBack = imageCallBack;
            this.imageUrl = imageUrl;
        }

        @Override
        protected File doInBackground(Object... params) {
            CloseableReference ref = (CloseableReference) params[0];
            InputStream is = new PooledByteBufferInputStream((PooledByteBuffer) ref.get());
            FileOutputStream bos = null;
            File f = getFile(imageUrl);
            try {
                if (!f.exists()) {
                    f.createNewFile();
                }
                bos = new FileOutputStream(f);
                byte[] b = new byte[2048];
                int n;
                while ((n = is.read(b)) != -1) {
                    bos.write(b, 0, n);
                }
                bos.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Closeables.closeQuietly(is);
                CloseableReference.closeSafely(ref);
                ref = null;
            }
            return f;
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            if (file != null && imageCallBack != null) {
                imageCallBack.onSuccess(imageUrl, file);
            }
        }
    }

    public static File getFile(String imgUrl) {
        File filedir = new File(Environment.getExternalStorageDirectory() + File.separator + "Android/data/" + app.getPackageName() + "/cache/image");
        if (!filedir.exists()) {
            filedir.mkdirs();
        }
        return new File(filedir, Futils.getMD5(imgUrl) + ".dat");
    }
}
