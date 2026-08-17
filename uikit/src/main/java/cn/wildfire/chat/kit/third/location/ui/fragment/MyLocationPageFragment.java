/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.third.location.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lqr.adapter.LQRAdapterForRecyclerView;
import com.lqr.adapter.LQRViewHolderForRecyclerView;
import com.tencent.lbssearch.TencentSearch;
import com.tencent.lbssearch.httpresponse.BaseObject;
import com.tencent.lbssearch.httpresponse.HttpResponseListener;
import com.tencent.lbssearch.object.Location;
import com.tencent.lbssearch.object.param.Geo2AddressParam;
import com.tencent.lbssearch.object.result.Geo2AddressResultObject;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.mapsdk.raster.model.BitmapDescriptorFactory;
import com.tencent.mapsdk.raster.model.CameraPosition;
import com.tencent.mapsdk.raster.model.Circle;
import com.tencent.mapsdk.raster.model.CircleOptions;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.mapsdk.raster.model.Marker;
import com.tencent.mapsdk.raster.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.map.MapView;
import com.tencent.tencentmap.mapsdk.map.TencentMap;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.third.location.data.LocationData;
import cn.wildfire.chat.kit.third.location.ui.activity.MyLocationActivity;
import cn.wildfire.chat.kit.third.utils.UIUtils;

/**
 * 「发送位置」整页：上半张地图 + 下半周边地点列表 + 标题栏「发送」。
 * <p>
 * 逐行搬自 {@link MyLocationActivity}，那个类现在只是手机端的壳。入口是会话输入栏加号面板里的
 * 「位置」，会话本身就在右栏，不迁的话选个位置要整屏跳出去再跳回来。
 * <p>
 * <strong>顺带删掉了位置包自带的那套 MVP 脚手架</strong>（{@code ui/base/BaseActivity}、
 * {@code ui/base/BasePresenter}、{@code ui/view/IMyLocationAtView}、
 * {@code ui/presenter/MyLocationAtPresenter}，四个文件）。它是这份第三方位置代码带进来的，
 * 全仓库只有位置这两页在用，而 {@code BasePresenter} 的构造函数签名写死了
 * {@code BaseActivity} ——页面变成 Fragment 之后它已经无法成立。Presenter 里真正的逻辑只有
 * 「POI 列表 adapter + 回传选中项」共约 50 行，并进本页比给脚手架再造一套 Fragment 版划算。
 * <p>
 * 「发送」按钮从布局里的 {@code confirmButton} 改成了标题栏菜单项：标题栏现在由宿主提供
 * （手机端是壳 Activity，平板是右栏的 {@code PanePageFragment}），页面无从往里塞按钮。
 * 与之一致，POI 还没搜出来时菜单项隐藏，等价于改造前的 {@code View.GONE}。
 */
public class MyLocationPageFragment extends Fragment implements WfcPage, TencentLocationListener {

    private final int maxMapHeight = UIUtils.dip2Px(300);
    private final int minMapHeight = UIUtils.dip2Px(150);

    private RelativeLayout mapContainer;
    private MapView mapView;
    private ImageButton showLocationButton;
    private RecyclerView poiRecyclerView;
    private ProgressBar progressBar;

    private TencentMap tencentMap;
    private TencentLocationManager locationManager;
    private TencentLocationRequest locationRequest;
    private TencentSearch tencentSearch;

    private Marker myLocation;
    private Circle accuracy;

    private final List<Geo2AddressResultObject.ReverseAddressResult.Poi> pois = new ArrayList<>();
    private LQRAdapterForRecyclerView<Geo2AddressResultObject.ReverseAddressResult.Poi> poiAdapter;
    private int selectedPosition = 0;
    /**
     * 搜出周边地点之后「发送」才可点。改造前是 {@code mBtnToolbarSend.setVisibility}，
     * 现在按钮在宿主的标题栏上，只能记个状态让 {@link #onPreparePageMenu} 去读。
     */
    private boolean poiLoaded;

    public static MyLocationPageFragment fromIntent(Intent intent) {
        return new MyLocationPageFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.location_activity_my_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setMapHeight(maxMapHeight);

        locationManager = TencentLocationManager.getInstance(requireContext());
        locationRequest = TencentLocationRequest.create();
        tencentMap = mapView.getMap();
        tencentSearch = new TencentSearch(requireContext());

        bindEvents();
        requestLocationUpdate();
    }

    private void bindViews(View view) {
        mapContainer = view.findViewById(R.id.rlMap);
        mapView = view.findViewById(R.id.map);
        showLocationButton = view.findViewById(R.id.ibShowLocation);
        poiRecyclerView = view.findViewById(R.id.rvPOI);
        progressBar = view.findViewById(R.id.pb);
    }

    private void bindEvents() {
        poiRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                GridLayoutManager layoutManager = (GridLayoutManager) poiRecyclerView.getLayoutManager();
                if (layoutManager == null) {
                    return;
                }
                if (dy > 0 && Math.abs(dy) > 10
                    && layoutManager.findFirstCompletelyVisibleItemPosition() <= 1
                    && mapContainer.getHeight() == maxMapHeight) {
                    setMapHeight(minMapHeight);
                    UIUtils.postTaskDelay(() -> poiRecyclerView.scrollToPosition(0), 0);
                } else if (dy < 0 && Math.abs(dy) > 10
                    && layoutManager.findFirstCompletelyVisibleItemPosition() == 1
                    && mapContainer.getHeight() == minMapHeight) {
                    setMapHeight(maxMapHeight);
                    UIUtils.postTaskDelay(() -> poiRecyclerView.scrollToPosition(0), 0);
                }
            }
        });
        showLocationButton.setOnClickListener(v -> requestLocationUpdate());
        tencentMap.setOnMapCameraChangeListener(new TencentMap.OnMapCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (myLocation != null) {
                    myLocation.setPosition(tencentMap.getMapCenter());
                }
            }

            @Override
            public void onCameraChangeFinish(CameraPosition cameraPosition) {
                if (accuracy != null) {
                    accuracy.setCenter(tencentMap.getMapCenter());
                }
                search(tencentMap.getMapCenter());
            }
        });
    }

    private void setMapHeight(int height) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mapContainer.getLayoutParams();
        params.height = height;
        mapContainer.setLayoutParams(params);
    }

    private void requestLocationUpdate() {
        int error = locationManager.requestLocationUpdates(locationRequest, this);
        if (error != 0) {
            Toast.makeText(getContext(), "腾讯地图key不正确，请看日志，查看更多信息", Toast.LENGTH_LONG).show();
            Log.e(MyLocationPageFragment.class.getSimpleName(),
                "!!! 腾讯地图key不正确，请查看AndroidManifest.xml里面的TencentMapSDK的配置及注释 !!!");
        }
    }

    // ==================== 周边地点 ====================

    private void search(LatLng latLng) {
        progressBar.setVisibility(View.VISIBLE);
        poiRecyclerView.setVisibility(View.GONE);
        Location location = new Location().lat((float) latLng.getLatitude()).lng((float) latLng.getLongitude());
        // 还可以传入其他坐标系的坐标，不过需要用coord_type()指明所用类型
        // 这里设置返回周边poi列表，可以在一定程度上满足用户获取指定坐标周边poi的需求
        Geo2AddressParam geo2AddressParam = new Geo2AddressParam().location(location).get_poi(true);
        tencentSearch.geo2address(geo2AddressParam, new HttpResponseListener() {

            @Override
            public void onSuccess(int statusCode, BaseObject result) {
                // 搜索是异步回来的，视图可能已经销毁（右栏里本页被弹掉）。
                // 改造前这里判的是 isFinishing()，Fragment 里对应的是「视图还在不在」。
                if (getView() == null) {
                    return;
                }
                progressBar.setVisibility(View.GONE);
                poiRecyclerView.setVisibility(View.VISIBLE);
                if (result == null) {
                    return;
                }
                loadPois((Geo2AddressResultObject) result);
            }

            @Override
            public void onFailure(int statusCode, String message, Throwable throwable) {
                if (getView() == null) {
                    return;
                }
                progressBar.setVisibility(View.GONE);
                poiRecyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadPois(Geo2AddressResultObject result) {
        pois.clear();
        pois.addAll(result.result.pois);
        setupPoiAdapter();
        if (!pois.isEmpty() && !poiLoaded) {
            poiLoaded = true;
            WfcPageCompat.invalidatePageMenu(this);
        }
    }

    private void setupPoiAdapter() {
        if (poiAdapter == null) {
            poiAdapter = new LQRAdapterForRecyclerView<Geo2AddressResultObject.ReverseAddressResult.Poi>(
                getContext(), pois, R.layout.location_item_location_poi) {
                @Override
                public void convert(LQRViewHolderForRecyclerView helper,
                                    Geo2AddressResultObject.ReverseAddressResult.Poi item, int position) {
                    helper.setText(R.id.tvTitle, item.title)
                        .setText(R.id.tvDesc, item.address)
                        .setViewVisibility(R.id.ivSelected, selectedPosition == position ? View.VISIBLE : View.GONE);
                }
            };
            poiRecyclerView.setAdapter(poiAdapter);
            poiRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
            poiAdapter.setOnItemClickListener((helper, parent, itemView, position) -> {
                selectedPosition = position;
                setupPoiAdapter();
            });
        } else {
            poiAdapter.notifyDataSetChangedWrapper();
        }
    }

    // ==================== WfcPage ====================

    @Override
    public int pageMenu() {
        return R.menu.location_send;
    }

    @Override
    public void onPreparePageMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.send);
        if (item != null) {
            item.setVisible(poiLoaded);
        }
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() != R.id.send) {
            return false;
        }
        sendSelectedLocation();
        return true;
    }

    private void sendSelectedLocation() {
        if (pois.size() <= selectedPosition) {
            return;
        }
        Geo2AddressResultObject.ReverseAddressResult.Poi poi = pois.get(selectedPosition);
        Intent data = new Intent();
        // 键名和类型不能动：接收方是 LocationExt.onActivityResult，按 "location" 取 LocationData
        data.putExtra("location", new LocationData(poi.location.lat, poi.location.lng, poi.title, snapshotMap()));
        WfcPageCompat.setPageResult(this, Activity.RESULT_OK, data);
        WfcPageCompat.finishPage(this);
    }

    /**
     * 位置消息气泡上那张缩略图：截地图当前画面的正中方块，最长边 240px。
     */
    private Bitmap snapshotMap() {
        mapView.buildDrawingCache();
        Bitmap original = mapView.getDrawingCache();
        int width = Math.min(original.getWidth(), original.getHeight());
        width = Math.min(width, 240);
        Bitmap thumbnail = Bitmap.createBitmap(original,
            (original.getWidth() - width) / 2, (original.getHeight() - width) / 2, width, width);
        mapView.destroyDrawingCache();
        return thumbnail;
    }

    // ==================== TencentLocationListener ====================

    @Override
    public void onLocationChanged(TencentLocation tencentLocation, int errorCode, String reason) {
        if (getView() == null) {
            return;
        }
        if (errorCode != TencentLocation.ERROR_OK) {
            return;
        }
        LatLng latLng = new LatLng(tencentLocation.getLatitude(), tencentLocation.getLongitude());
        if (myLocation == null) {
            myLocation = tencentMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.arm))
                .anchor(0.5f, 0.8f));
        }
        if (accuracy == null) {
            accuracy = tencentMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(tencentLocation.getAccuracy())
                .fillColor(0x440000ff)
                .strokeWidth(0f));
        }
        myLocation.setPosition(latLng);
        accuracy.setCenter(latLng);
        accuracy.setRadius(tencentLocation.getAccuracy());
        tencentMap.animateTo(latLng);
        tencentMap.setZoom(16);
        search(latLng);
        locationManager.removeUpdates(this);
    }

    @Override
    public void onStatusUpdate(String name, int status, String desc) {
        // 改造前这里只是把状态码翻译成一个没人读的本地变量，原样保留「什么也不做」
    }

    // ==================== 地图生命周期 ====================

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        // 理由同 ShowLocationPageFragment：右栏里宿主 Activity 长期活着，
        // 本页出栈只销毁视图，不放地图和定位就是真泄漏。
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        if (mapView != null) {
            mapView.onDestroy();
        }
        mapView = null;
        tencentMap = null;
        myLocation = null;
        accuracy = null;
        poiAdapter = null;
        super.onDestroyView();
    }
}
